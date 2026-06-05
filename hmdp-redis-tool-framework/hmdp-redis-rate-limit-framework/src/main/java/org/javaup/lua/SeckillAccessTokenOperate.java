package org.javaup.lua;

import org.javaup.redis.RedisCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.List;

public class SeckillAccessTokenOperate {

    private final DefaultRedisScript<Long> script;
    private final RedisCache redisCache;

    public SeckillAccessTokenOperate(RedisCache redisCache) {
        this.redisCache = redisCache;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_access_token.lua")));
        this.script.setResultType(Long.class);
    }
    
    public boolean validateAndConsume(String key, String expected) {
        List<String> keys = Collections.singletonList(key);
        Long ret = (Long)redisCache.getInstance().execute(script, keys, expected);
        return ret == 1L;
    }
}