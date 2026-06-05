package org.javaup.service.impl;

import cn.hutool.core.util.IdUtil;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.lua.SeckillAccessTokenOperate;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillAccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SeckillAccessTokenServiceImpl implements ISeckillAccessTokenService {

    @Value("${seckill.access.token.enabled:true}")
    private boolean enabled;

    @Value("${seckill.access.token.ttl-seconds:30}")
    private long ttlSeconds;

    @Resource
    private RedisCache redisCache;
    
    @Resource
    private MeterRegistry meterRegistry;

    private SeckillAccessTokenOperate operate;

    @PostConstruct
    public void init() {
        operate = new SeckillAccessTokenOperate(redisCache);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String issueAccessToken(Long voucherId, Long userId) {
        String token = IdUtil.simpleUUID();
        boolean ok = redisCache.setIfAbsent(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ACCESS_TOKEN_TAG_KEY, voucherId, userId), 
                token, 
                ttlSeconds, 
                TimeUnit.SECONDS);
        if (!ok) {
            String existing = redisCache.get(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ACCESS_TOKEN_TAG_KEY, voucherId, userId), 
                    String.class);
            safeInc("seckill_access_token_issue_conflict", "component", "service_impl");
            return existing != null ? existing : token;
        }
        safeInc("seckill_access_token_issue_success", "component", "service_impl");
        log.info("获取到令牌成功！令牌：{}", token);
        return token;
    }

    @Override
    public boolean validateAndConsume(Long voucherId, Long userId, String token) {
        String key = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ACCESS_TOKEN_TAG_KEY, voucherId, userId).getRelKey();
        boolean success = operate.validateAndConsume(key, token);
        safeInc(success ? "seckill_access_token_consume_success" : "seckill_access_token_consume_fail",
                "component", "service_impl");
        return success;
    }

    private void safeInc(String name, String tagKey, String tagValue) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter(name, tagKey, tagValue).increment();
            }
        } catch (Exception ignore) {
        }
    }
}