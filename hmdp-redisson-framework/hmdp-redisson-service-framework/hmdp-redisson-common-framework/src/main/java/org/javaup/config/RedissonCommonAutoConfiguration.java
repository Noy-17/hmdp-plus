package org.javaup.config;

import org.javaup.handle.RedissonDataHandle;
import org.javaup.locallock.LocalLockCache;
import org.javaup.lockinfo.factory.LockInfoHandleFactory;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@AutoConfigureBefore(value = {RedissonAutoConfigurationV2.class, RedissonAutoConfiguration.class})
@EnableConfigurationProperties(RedissonBaseProperties.class)
public class RedissonCommonAutoConfiguration {
    
    private final AtomicInteger executeTaskThreadCount = new AtomicInteger(1);
    
    @Bean
    public RedissonClient redissonClient(DataRedisProperties redisProperties, RedissonBaseProperties redissonBaseProperties){
        Config config = new Config();
        String prefix = "redis://";
        Method method = ReflectionUtils.findMethod(DataRedisProperties.class, "isSsl");
        if (method != null && (Boolean)ReflectionUtils.invokeMethod(method, redisProperties)) {
            prefix = "rediss://";
        }
        config.useSingleServer()
                .setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setConnectTimeout(1000)
                .setDatabase(redisProperties.getDatabase())
                .setPassword(redisProperties.getPassword());
        config.setThreads(redissonBaseProperties.getThreads());
        config.setNettyThreads(redissonBaseProperties.getNettyThreads());
        if (Objects.nonNull(redissonBaseProperties.getCorePoolSize()) && 
                Objects.nonNull(redissonBaseProperties.getMaximumPoolSize())) {
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    redissonBaseProperties.getCorePoolSize(),
                    redissonBaseProperties.getMaximumPoolSize(),
                    redissonBaseProperties.getKeepAliveTime(),
                    redissonBaseProperties.getUnit(),
                    new LinkedBlockingQueue<>(redissonBaseProperties.getWorkQueueSize()),
                    r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                            "redisson-thread-" + executeTaskThreadCount.getAndIncrement()));
            config.setExecutor(threadPoolExecutor);
        }
        return Redisson.create(config);
    }
    
    @Bean
    public RedissonDataHandle redissonDataHandle(RedissonClient redissonClient){
        return new RedissonDataHandle(redissonClient);
    }
    
    @Bean
    public LocalLockCache localLockCache(){
        return new LocalLockCache();
    }
    
    @Bean
    public LockInfoHandleFactory lockInfoHandleFactory(){
        return new LockInfoHandleFactory();
    }
}
