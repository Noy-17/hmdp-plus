package org.javaup.config;

import org.javaup.constant.LockInfoType;
import org.javaup.handle.RedissonDataHandle;
import org.javaup.locallock.LocalLockCache;
import org.javaup.lockinfo.LockInfoHandle;
import org.javaup.lockinfo.factory.LockInfoHandleFactory;
import org.javaup.lockinfo.impl.RepeatExecuteLimitLockInfoHandle;
import org.javaup.repeatexecutelimit.aspect.RepeatExecuteLimitAspect;
import org.javaup.servicelock.factory.ServiceLockFactory;
import org.springframework.context.annotation.Bean;

public class RepeatExecuteLimitAutoConfiguration {
    
    @Bean(LockInfoType.REPEAT_EXECUTE_LIMIT)
    public LockInfoHandle repeatExecuteLimitHandle(){
        return new RepeatExecuteLimitLockInfoHandle();
    }
    
    @Bean
    public RepeatExecuteLimitAspect repeatExecuteLimitAspect(LocalLockCache localLockCache,
                                                             LockInfoHandleFactory lockInfoHandleFactory,
                                                             ServiceLockFactory serviceLockFactory,
                                                             RedissonDataHandle redissonDataHandle){
        return new RepeatExecuteLimitAspect(localLockCache, lockInfoHandleFactory,serviceLockFactory,redissonDataHandle);
    }
}
    