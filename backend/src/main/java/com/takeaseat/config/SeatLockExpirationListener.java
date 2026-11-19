package com.takeaseat.config;

import com.takeaseat.service.SeatLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Listens for Redis keyspace expiration events (__keyevent@*__:expired) so abandoned
 * 10-minute seat holds (seat_lock:{showId}:{seatNumber}) are automatically released and logged.
 *
 * Requires Redis keyspace notifications for expired events:
 *   CONFIG SET notify-keyspace-events Ex
 */
@Slf4j
@Component
public class SeatLockExpirationListener extends KeyExpirationEventMessageListener {

    public SeatLockExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
        // Attempt to enable expired keyspace events automatically if Redis allows CONFIG SET
        setKeyspaceNotificationsConfigParameter("Ex");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!expiredKey.startsWith(SeatLockService.LOCK_KEY_PREFIX)) {
            return;
        }

        // Format: seat_lock:{showId}:{seatNumber}
        String[] parts = expiredKey.split(":");
        if (parts.length == 3) {
            String showId = parts[1];
            String seatNumber = parts[2];
            log.info("Seat hold expired automatically: showId={}, seat={} (10-minute TTL elapsed)", showId, seatNumber);
        } else {
            log.info("Seat lock key expired: {}", expiredKey);
        }
    }
}
