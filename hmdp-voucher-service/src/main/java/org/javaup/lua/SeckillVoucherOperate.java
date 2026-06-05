package org.javaup.lua;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.redis.RedisCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SeckillVoucherOperate {
    
    @Resource
    private RedisCache redisCache;
    
    private DefaultRedisScript<String> redisScript;
    
    @PostConstruct
    public void init(){
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckillVoucher.lua")));
            redisScript.setResultType(String.class);
        } catch (Exception e) {
            log.error("redisScript init lua error",e);
        }
    }
    
    public SeckillVoucherDomain execute(List<String> keys, String[] args){
        Object object = redisCache.getInstance().execute(redisScript, keys, args);
        return JSON.parseObject((String)object, SeckillVoucherDomain.class);
    }
}
