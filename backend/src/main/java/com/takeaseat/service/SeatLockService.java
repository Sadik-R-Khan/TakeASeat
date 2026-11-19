package com.takeaseat.service;

import com.takeaseat.repository.BookingSeatRepository;
import com.takeaseat.util.SeatLayoutUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    public static final String LOCK_KEY_PREFIX = "seat_lock:";
    public static final String BOOKED_SET_PREFIX = "show_booked:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> lockSeatsScript;
    private final DefaultRedisScript<Long> releaseSeatsScript;
    private final BookingSeatRepository bookingSeatRepository;

    @Value("${app.seat-lock.ttl-seconds:600}")
    private long lockTtlSeconds;

    public String lockKey(Long showId, String seatNumber) {
        return LOCK_KEY_PREFIX + showId + ":" + seatNumber;
    }

    public String bookedSetKey(Long showId) {
        return BOOKED_SET_PREFIX + showId;
    }

    public long getLockTtlSeconds() {
        return lockTtlSeconds;
    }

    /**
     * Atomically checks MySQL + Redis booked state and Redis lock keys via Lua script.
     * Succeeds only if ALL requested seats are free; otherwise locks zero seats.
     */
    public void acquireLocks(Long showId, List<String> seatNumbers, Long userId) {
        List<String> validSeats = SeatLayoutUtil.normalizeAndValidate(seatNumbers);

        // Ensure Redis booked set reflects MySQL confirmed seats (handles cold Redis restarts cleanly)
        List<String> dbBookedSeats = bookingSeatRepository.findActiveSeatNumbersByShowId(showId);
        if (!dbBookedSeats.isEmpty()) {
            redisTemplate.opsForSet().add(bookedSetKey(showId), dbBookedSeats.toArray(String[]::new));
            Set<String> dbBookedSet = new HashSet<>(dbBookedSeats);
            for (String seat : validSeats) {
                if (dbBookedSet.contains(seat)) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Seat " + seat + " is already booked"
                    );
                }
            }
        }

        // Prepare KEYS: [show_booked:{showId}, seat_lock:{showId}:{seat1}, ...]
        List<String> keys = new ArrayList<>(validSeats.size() + 1);
        keys.add(bookedSetKey(showId));
        for (String seat : validSeats) {
            keys.add(lockKey(showId, seat));
        }

        // Prepare ARGV: [userId, ttlSeconds, seat1, seat2, ...]
        List<String> args = new ArrayList<>(validSeats.size() + 2);
        args.add(String.valueOf(userId));
        args.add(String.valueOf(lockTtlSeconds));
        args.addAll(validSeats);

        Long result = redisTemplate.execute(lockSeatsScript, keys, args.toArray());
        if (result == null || result != 1L) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "One or more selected seats are already booked or temporarily held by another user"
            );
        }

        log.info("Acquired {}s Redis lock for userId={}, showId={}, seats={}", lockTtlSeconds, userId, showId, validSeats);
    }

    public boolean verifyUserOwnsLocks(Long showId, List<String> seatNumbers, Long userId) {
        List<String> keys = seatNumbers.stream()
                .map(seat -> lockKey(showId, seat))
                .toList();

        List<String> holders = redisTemplate.opsForValue().multiGet(keys);
        if (holders == null || holders.size() != seatNumbers.size()) {
            return false;
        }

        String expectedUserId = String.valueOf(userId);
        for (String holder : holders) {
            if (!expectedUserId.equals(holder)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fetches lock holders for all 50 seats in a single MGET round-trip.
     */
    public Map<String, String> getSeatLockHolders(Long showId) {
        List<String> allSeats = SeatLayoutUtil.ALL_SEATS;
        List<String> keys = allSeats.stream()
                .map(seat -> lockKey(showId, seat))
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, String> holders = new HashMap<>();
        if (values != null) {
            for (int i = 0; i < allSeats.size(); i++) {
                String holder = values.get(i);
                if (holder != null) {
                    holders.put(allSeats.get(i), holder);
                }
            }
        }
        return holders;
    }

    public long getRemainingTtlSeconds(Long showId, List<String> heldSeats) {
        if (heldSeats == null || heldSeats.isEmpty()) {
            return 0L;
        }
        long minTtl = Long.MAX_VALUE;
        for (String seat : heldSeats) {
            Long ttl = redisTemplate.getExpire(lockKey(showId, seat), TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                minTtl = Math.min(minTtl, ttl);
            }
        }
        return minTtl == Long.MAX_VALUE ? 0L : minTtl;
    }

    public long releaseLocks(Long showId, List<String> seatNumbers, Long userId) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            return 0L;
        }
        List<String> keys = seatNumbers.stream()
                .map(seat -> lockKey(showId, seat))
                .toList();

        Long deleted = redisTemplate.execute(releaseSeatsScript, keys, String.valueOf(userId));
        return deleted != null ? deleted : 0L;
    }

    public long releaseAllUserLocksForShow(Long showId, Long userId) {
        return releaseLocks(showId, SeatLayoutUtil.ALL_SEATS, userId);
    }

    public void markSeatsBookedAndReleaseLocks(Long showId, List<String> seatNumbers, Long userId) {
        if (!seatNumbers.isEmpty()) {
            redisTemplate.opsForSet().add(bookedSetKey(showId), seatNumbers.toArray(String[]::new));
            releaseLocks(showId, seatNumbers, userId);
        }
    }

    public void unmarkSeatsBooked(Long showId, List<String> seatNumbers) {
        if (seatNumbers != null && !seatNumbers.isEmpty()) {
            redisTemplate.opsForSet().remove(bookedSetKey(showId), (Object[]) seatNumbers.toArray(String[]::new));
        }
    }

    public void clearShowRedisState(Long showId) {
        List<String> keysToDelete = new ArrayList<>(SeatLayoutUtil.TOTAL_SEATS + 1);
        keysToDelete.add(bookedSetKey(showId));
        for (String seat : SeatLayoutUtil.ALL_SEATS) {
            keysToDelete.add(lockKey(showId, seat));
        }
        redisTemplate.delete(keysToDelete);
    }
}
