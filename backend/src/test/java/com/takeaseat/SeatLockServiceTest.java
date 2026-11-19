package com.takeaseat;

import com.takeaseat.repository.BookingSeatRepository;
import com.takeaseat.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    private DefaultRedisScript<Long> lockScript;
    private DefaultRedisScript<Long> releaseScript;
    private SeatLockService seatLockService;

    @BeforeEach
    void setUp() {
        lockScript = new DefaultRedisScript<>("return 1", Long.class);
        releaseScript = new DefaultRedisScript<>("return 1", Long.class);
        seatLockService = new SeatLockService(redisTemplate, lockScript, releaseScript, bookingSeatRepository);
        ReflectionTestUtils.setField(seatLockService, "lockTtlSeconds", 600L);
    }

    @Test
    void acquireLocksSucceedsWhenLuaReturnsOne() {
        when(bookingSeatRepository.findActiveSeatNumbersByShowId(10L)).thenReturn(List.of());
        when(redisTemplate.execute(eq(lockScript), anyList(), any(Object[].class))).thenReturn(1L);

        assertDoesNotThrow(() -> seatLockService.acquireLocks(10L, List.of("1A", "1B"), 42L));
        verify(redisTemplate).execute(eq(lockScript), eq(List.of("show_booked:10", "seat_lock:10:1A", "seat_lock:10:1B")),
                eq("42"), eq("600"), eq("1A"), eq("1B"));
    }

    @Test
    void acquireLocksThrowsConflictWhenLuaFailsAtomically() {
        when(bookingSeatRepository.findActiveSeatNumbersByShowId(10L)).thenReturn(List.of());
        when(redisTemplate.execute(eq(lockScript), anyList(), any(Object[].class))).thenReturn(0L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> seatLockService.acquireLocks(10L, List.of("1A", "1B"), 42L)
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void acquireLocksThrowsConflictWhenSeatBookedInMySql() {
        when(bookingSeatRepository.findActiveSeatNumbersByShowId(10L)).thenReturn(List.of("1B"));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> seatLockService.acquireLocks(10L, List.of("1A", "1B"), 42L)
        );
        assertEquals(409, ex.getStatusCode().value());
        verify(redisTemplate, never()).execute(eq(lockScript), anyList(), any(Object[].class));
    }

    @Test
    void verifyUserOwnsLocksChecksAllKeysMatchUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(List.of("seat_lock:5:2A", "seat_lock:5:2B")))
                .thenReturn(List.of("7", "7"));

        assertTrue(seatLockService.verifyUserOwnsLocks(5L, List.of("2A", "2B"), 7L));

        when(valueOperations.multiGet(List.of("seat_lock:5:2A", "seat_lock:5:2B")))
                .thenReturn(List.of("7", "9"));
        assertFalse(seatLockService.verifyUserOwnsLocks(5L, List.of("2A", "2B"), 7L));
    }
}
