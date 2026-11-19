-- Atomically release seat locks owned by a specific user
-- KEYS[1..n] = seat_lock:{showId}:{seatNumber}
-- ARGV[1]    = userId
-- Returns number of keys deleted

local userId = ARGV[1]
local deleted = 0

for i = 1, #KEYS do
    local currentHolder = redis.call('GET', KEYS[i])
    if currentHolder == userId then
        redis.call('DEL', KEYS[i])
        deleted = deleted + 1
    end
end

return deleted
