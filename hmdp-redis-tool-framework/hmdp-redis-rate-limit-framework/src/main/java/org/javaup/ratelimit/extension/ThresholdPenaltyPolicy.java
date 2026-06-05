package org.javaup.ratelimit.extension;

import lombok.extern.slf4j.Slf4j;
import org.javaup.config.SeckillRateLimitConfigProperties;
import org.javaup.core.RedisKeyManage;
import org.javaup.enums.BaseCode;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThresholdPenaltyPolicy implements RateLimitPenaltyPolicy {

    private final RedisCache redisCache;
    private final SeckillRateLimitConfigProperties props;

    public ThresholdPenaltyPolicy(RedisCache redisCache, SeckillRateLimitConfigProperties props) {
        this.redisCache = redisCache;
        this.props = props;
    }

    @Override
    public void apply(RateLimitContext context, BaseCode reason) {
        try {
            if (reason == BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED) {
                applyForIp(context);
            } else if (reason == BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED) {
                applyForUser(context);
            }
        } catch (Exception e) {
            log.debug("Penalty policy apply failed: {}", e.getMessage());
        }
    }

    private void applyForIp(RateLimitContext ctx) {
        Long voucherId = ctx.getVoucherId();
        String clientIp = ctx.getClientIp();
        if (Objects.isNull(voucherId) || Objects.isNull(clientIp)) {
            return;
        }
        RedisKeyBuild violationKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.SECKILL_VIOLATION_IP_TAG_KEY, voucherId, clientIp);
        long count = redisCache.incrBy(violationKey, 1L);
        if (count == 1L) {
            redisCache.expire(violationKey, props.getViolationWindowSeconds(), TimeUnit.SECONDS);
        }
        if (count >= props.getIpBlockThreshold()) {
            RedisKeyBuild blockKey = RedisKeyBuild.createRedisKey(
                    RedisKeyManage.SECKILL_BLOCK_IP_TAG_KEY, voucherId, clientIp);
            redisCache.set(blockKey, "1", props.getIpBlockTtlSeconds(), TimeUnit.SECONDS);
            log.warn("Temporary banned IP: voucherId={}, ip={}, ttlSeconds={}, violationCount={}",
                    voucherId, clientIp, props.getIpBlockTtlSeconds(), count);
        }
    }

    private void applyForUser(RateLimitContext ctx) {
        Long voucherId = ctx.getVoucherId();
        Long userId = ctx.getUserId();
        if (Objects.isNull(voucherId) || Objects.isNull(userId)) {
            return;
        }
        RedisKeyBuild violationKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.SECKILL_VIOLATION_USER_TAG_KEY, voucherId, userId);
        long count = redisCache.incrBy(violationKey, 1L);
        if (count == 1L) {
            redisCache.expire(violationKey, props.getViolationWindowSeconds(), TimeUnit.SECONDS);
        }
        if (count >= props.getUserBlockThreshold()) {
            RedisKeyBuild blockKey = RedisKeyBuild.createRedisKey(
                    RedisKeyManage.SECKILL_BLOCK_USER_TAG_KEY, voucherId, userId);
            redisCache.set(blockKey, "1", props.getUserBlockTtlSeconds(), TimeUnit.SECONDS);
            log.warn("Temporary banned user: voucherId={}, userId={}, ttlSeconds={}, violationCount={}",
                    voucherId, userId, props.getUserBlockTtlSeconds(), count);
        }
    }
}