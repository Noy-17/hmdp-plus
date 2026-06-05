package org.javaup.lua;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.javaup.redis.RedisCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Slf4j
public class TokenBucketRateLimitOperate {

    private final RedisCache redisCache;

    public TokenBucketRateLimitOperate(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    private DefaultRedisScript<Integer> redisScript;

    @PostConstruct
    public void init(){
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/tokenBucket.lua")));
            redisScript.setResultType(Integer.class);
        } catch (Exception e) {
            log.error("TokenBucketRateLimitOperate init lua error", e);
        }
    }

    public Long execute(List<String> keys, String[] args){
        return (Long)redisCache.getInstance().execute(redisScript, keys, args);
    }
}