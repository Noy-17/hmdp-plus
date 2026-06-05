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

local function uniqueMember(baseKey, ts)
    local seqKey = baseKey .. ':seq'
    local seq = redis.call('INCR', seqKey)
    if seq == 1 then
        redis.call('PEXPIRE', seqKey, 600000) -- 10分钟
    end
    return tostring(ts) .. ':' .. tostring(seq)
end

local function checkSlidingLimit(zsetKey, windowMillis, maxAttempts, exceededCode)
    if zsetKey ~= nil and zsetKey ~= '' and windowMillis > 0 and maxAttempts > 0 then
        local member = uniqueMember(zsetKey, nowMillis)
        redis.call('ZADD', zsetKey, nowMillis, member)
        local minScore = 0
        local maxOld = nowMillis - windowMillis
        redis.call('ZREMRANGEBYSCORE', zsetKey, minScore, maxOld)
        local cnt = redis.call('ZCARD', zsetKey)
        if cnt == 1 then
            redis.call('PEXPIRE', zsetKey, windowMillis * 2)
        end
        if cnt > maxAttempts then
            return exceededCode
        end
    end
    return CODE_SUCCESS
end

local ipRet = CODE_SUCCESS
if ipKey ~= nil and ipKey ~= '' and ipWindowMillis > 0 and ipMaxAttempts > 0 then
    ipRet = checkSlidingLimit(ipKey, ipWindowMillis, ipMaxAttempts, CODE_IP_EXCEEDED)
    if ipRet ~= CODE_SUCCESS then
        return ipRet
    end
end

local userRet = checkSlidingLimit(userKey, userWindowMillis, userMaxAttempts, CODE_USER_EXCEEDED)
if userRet ~= CODE_SUCCESS then
    return userRet
end

return CODE_SUCCESS