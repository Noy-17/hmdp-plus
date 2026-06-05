package org.javaup.config;

import org.javaup.execute.RedisRateLimitHandler;
import org.javaup.lua.SlidingRateLimitOperate;
import org.javaup.lua.TokenBucketRateLimitOperate;
import org.javaup.ratelimit.extension.NoOpRateLimitEventListener;
import org.javaup.ratelimit.extension.NoOpRateLimitPenaltyPolicy;
import org.javaup.ratelimit.extension.RateLimitEventListener;
import org.javaup.ratelimit.extension.RateLimitPenaltyPolicy;
import org.javaup.ratelimit.extension.ThresholdPenaltyPolicy;
import org.javaup.redis.RedisCache;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(SeckillRateLimitConfigProperties.class)
public class RateLimitAutoConfiguration {
    
    @Bean
    public SlidingRateLimitOperate slidingRateLimitOperate(RedisCache redisCache){
        return new SlidingRateLimitOperate(redisCache);
    }
    
    @Bean
    public TokenBucketRateLimitOperate tokenBucketRateLimitOperate(RedisCache redisCache){
        return new TokenBucketRateLimitOperate(redisCache);
    }

    @Bean
    public RateLimitEventListener rateLimitEventListener(){
        return new NoOpRateLimitEventListener();
    }

    @Bean
    public RateLimitPenaltyPolicy rateLimitPenaltyPolicy(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                                         RedisCache redisCache){
        
        Boolean enable = seckillRateLimitConfigProperties.getEnablePenalty();
        if (Boolean.TRUE.equals(enable)) {
            return new ThresholdPenaltyPolicy(redisCache, seckillRateLimitConfigProperties);
        }
        return new NoOpRateLimitPenaltyPolicy();
    }

    @Bean
    public RedisRateLimitHandler redisRateLimitHandler(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                                       RedisCache redisCache,
                                                       SlidingRateLimitOperate slidingRateLimitOperate,
                                                       TokenBucketRateLimitOperate tokenBucketRateLimitOperate,
                                                       RateLimitEventListener rateLimitEventListener,
                                                       RateLimitPenaltyPolicy rateLimitPenaltyPolicy) {
        return new RedisRateLimitHandler(
                seckillRateLimitConfigProperties, 
                redisCache,
                slidingRateLimitOperate,
                tokenBucketRateLimitOperate,
                rateLimitEventListener,
                rateLimitPenaltyPolicy
        );
    }
}
