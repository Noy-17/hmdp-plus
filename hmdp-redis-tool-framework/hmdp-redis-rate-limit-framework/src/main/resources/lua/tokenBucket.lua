local ipKey = KEYS[1]
local userKey = KEYS[2]
local ipWindowMillis = tonumber(ARGV[1] or '0')
local ipMaxAttempts = tonumber(ARGV[2] or '0')
local userWindowMillis = tonumber(ARGV[3] or '0')
local userMaxAttempts = tonumber(ARGV[4] or '0')
local CODE_SUCCESS = 0        
local CODE_IP_EXCEEDED = 10007 
local CODE_USER_EXCEEDED = 10008
local now = redis.call('TIME')
local nowMillis = now[1] * 1000 + math.floor(now[2] / 1000)

local function clampDelta(delta, window)
  if delta < 0 then return 0 end
  local maxDelta = window > 0 and (window * 2) or 0
  if maxDelta > 0 and delta > maxDelta then return maxDelta end
  return delta
end

local function tryConsume(bucketKey, windowMillis, maxAttempts)
  if bucketKey == nil or bucketKey == '' or windowMillis <= 0 or maxAttempts <= 0 then
    return true
  end
  local capacity = maxAttempts
  local ratePerMs = maxAttempts / windowMillis
  local lastMs = tonumber(redis.call('HGET', bucketKey, 'last_ms'))
  local tokens = tonumber(redis.call('HGET', bucketKey, 'tokens'))

  if not lastMs then
    lastMs = nowMillis
    tokens = capacity
  end

  local delta = clampDelta(nowMillis - lastMs, windowMillis)
  local refill = delta * ratePerMs
  tokens = math.min(capacity, tokens + refill)

  if tokens >= 1.0 then
    tokens = tokens - 1.0
    redis.call('HSET', bucketKey, 'tokens', tokens)
    redis.call('HSET', bucketKey, 'last_ms', nowMillis)
    local ttl = (windowMillis * 2) + math.random(0, math.max(1, math.floor(windowMillis / 10)))
    redis.call('PEXPIRE', bucketKey, ttl)
    return true
  else
    redis.call('HSET', bucketKey, 'tokens', tokens)
    redis.call('HSET', bucketKey, 'last_ms', nowMillis)
    local ttl = (windowMillis * 2) + math.random(0, math.max(1, math.floor(windowMillis / 10)))
    redis.call('PEXPIRE', bucketKey, ttl)
    return false
  end
end

local ipAllowed = tryConsume(ipKey, ipWindowMillis, ipMaxAttempts)
if not ipAllowed then
  return CODE_IP_EXCEEDED
end
local userAllowed = tryConsume(userKey, userWindowMillis, userMaxAttempts)
if not userAllowed then
  return CODE_USER_EXCEEDED
end
return CODE_SUCCESS