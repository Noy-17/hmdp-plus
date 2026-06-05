package org.javaup.core;

import org.javaup.servicelock.LockType;
import org.javaup.servicelock.ServiceLocker;
import org.javaup.servicelock.impl.RedissonFairLocker;
import org.javaup.servicelock.impl.RedissonReadLocker;
import org.javaup.servicelock.impl.RedissonReentrantLocker;
import org.javaup.servicelock.impl.RedissonWriteLocker;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.Map;

import static org.javaup.servicelock.LockType.Fair;
import static org.javaup.servicelock.LockType.Read;
import static org.javaup.servicelock.LockType.Reentrant;
import static org.javaup.servicelock.LockType.Write;

public class ManageLocker {

    private final Map<LockType, ServiceLocker> cacheLocker = new HashMap<>();
    
    public ManageLocker(RedissonClient redissonClient){
        cacheLocker.put(Reentrant,new RedissonReentrantLocker(redissonClient));
        cacheLocker.put(Fair,new RedissonFairLocker(redissonClient));
        cacheLocker.put(Write,new RedissonWriteLocker(redissonClient));
        cacheLocker.put(Read,new RedissonReadLocker(redissonClient));
    }
    
    public ServiceLocker getReentrantLocker(){
        return cacheLocker.get(Reentrant);
    }
    
    public ServiceLocker getFairLocker(){
        return cacheLocker.get(Fair);
    }
    
    public ServiceLocker getWriteLocker(){
        return cacheLocker.get(Write);
    }
    
    public ServiceLocker getReadLocker(){
        return cacheLocker.get(Read);
    }
}
