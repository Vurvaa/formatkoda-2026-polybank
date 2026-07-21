local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local period = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

if capacity == nil or capacity <= 0 then
	return redis.error_reply("capacity must be positive")
end

if period == nil or period <= 0 then
	return redis.error_reply("period must be positive")
end

if now == nil then
	return redis.error_reply("now is required")
end

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')

local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

if tokens == nil or last_refill == nil then
    tokens = capacity
    last_refill = now
else
    tokens = math.min(tokens, capacity)
end

if now < last_refill then
	now = last_refill
end

local time_passed = now - last_refill

if time_passed >= period then
	local passed_intervals = math.floor(time_passed / period)
	tokens = capacity
	last_refill = last_refill + passed_intervals * period
end

local allowed = tokens > 0
if allowed then
    tokens = tokens - 1
end

redis.call('HSET', key, 'tokens', tokens, 'last_refill', last_refill)
redis.call('PEXPIRE', key, period * 2)

local retry_after = 0

if not allowed then
    retry_after = math.max(0, period - (now - last_refill))
end

return {allowed and 1 or 0, tokens, retry_after}
