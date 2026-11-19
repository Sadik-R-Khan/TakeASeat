-- Atomic multi-seat reservation script for TakeASeat
-- KEYS[1]       = show_booked:{showId}  (Redis Set of confirmed seat numbers)
-- KEYS[2..n+1]  = seat_lock:{showId}:{seatNumber}
-- ARGV[1]       = userId (string)
-- ARGV[2]       = ttlSeconds (600)
-- ARGV[3..n+2]  = seatNumber corresponding to KEYS[2..n+1]
--
-- Returns 1 if ALL seats were atomically locked for userId.
-- Returns 0 if ANY seat is already booked or locked by another user (locks nothing).

local bookedSetKey = KEYS[1]
local userId = ARGV[1]
local ttl = tonumber(ARGV[2])
local seatCount = #KEYS - 1

if seatCount <= 0 then
    return 0
end

-- Phase 1: Validate every requested seat before mutating anything
for i = 1, seatCount do
    local lockKey = KEYS[i + 1]
    local seatNum = ARGV[i + 2]

    if redis.call('SISMEMBER', bookedSetKey, seatNum) == 1 then
        return 0
    end

    local currentHolder = redis.call('GET', lockKey)
    if currentHolder and currentHolder ~= userId then
        return 0
    end
end

-- Phase 2: Atomically acquire (or renew) locks with TTL
for i = 1, seatCount do
    redis.call('SET', KEYS[i + 1], userId, 'EX', ttl)
end

return 1
