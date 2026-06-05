local stockKey = KEYS[1]
local seckillUserKey = KEYS[2]
local traceLogKey = KEYS[3]
local voucherId = ARGV[1]
local userId = ARGV[2]
local beginTime = tonumber(ARGV[3])
local endTime = tonumber(ARGV[4])
local status = tonumber(ARGV[5])
local orderId = ARGV[6]
local traceId = ARGV[7]
local logType = ARGV[8]
local ttlSeconds = tonumber(ARGV[9])
local timeArr = redis.call('TIME')
local nowMillis = tonumber(timeArr[1]) * 1000 + math.floor(tonumber(timeArr[2]) / 1000)

if nowMillis < beginTime then
    return string.format('{"%s": %d}', 'code', 10002)
end
if nowMillis > endTime then
    return string.format('{"%s": %d}', 'code', 10003)
end
if (status == 2) then
    return string.format('{"%s": %d}', 'code', 10011)
end
if (status == 3) then
    return string.format('{"%s": %d}', 'code', 10012)
end
local stock = redis.call('get', stockKey);
if not stock then
    return string.format('{"%s": %d}', 'code', 10004)
end
if (tonumber(stock) <= 0) then
    return string.format('{"%s": %d}', 'code', 10005)
end
if (redis.call('sismember', seckillUserKey, userId) == 1) then
    return string.format('{"%s": %d}', 'code', 10006)
end
local beforeQty = tonumber(stock)
local changeQty = 1
local afterQty = beforeQty - changeQty
redis.call('incrby', stockKey, -changeQty)
redis.call('sadd', seckillUserKey, userId)
local timeArr2 = redis.call('TIME')
local logNowMillis = tonumber(timeArr2[1]) * 1000 + math.floor(tonumber(timeArr2[2]) / 1000)
local logEntry = cjson.encode({
  logType = logType,
  ts = logNowMillis,
  orderId = orderId,
  traceId = traceId,
  userId = userId,
  voucherId = voucherId,
  beforeQty = beforeQty,
  changeQty = changeQty,
  afterQty = afterQty
})
redis.call('hset', traceLogKey, traceId, logEntry)
if ttlSeconds and ttlSeconds > 0 then
  redis.call('expire', traceLogKey, ttlSeconds)
end
return string.format('{"%s": %d, "%s": %s, "%s": %s, "%s": %s}', 'code', 0, 'beforeQty', beforeQty, 'deductQty', changeQty, 'afterQty', afterQty)