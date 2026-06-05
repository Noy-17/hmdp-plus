package org.javaup.cache;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.javaup.model.SeckillVoucherFullModel;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SeckillVoucherLocalCache {
    
    private final Cache<String, SeckillVoucherFullModel> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfter(new Expiry<String, SeckillVoucherFullModel>() {
                @Override
                public long expireAfterCreate(String key, SeckillVoucherFullModel value, long currentTime) {
                    long ttlSeconds = 60L;
                    if (value != null && value.getEndTime() != null) {
                        ttlSeconds = Math.max(
                                LocalDateTimeUtil.between(LocalDateTimeUtil.now(), value.getEndTime()).getSeconds(),
                                1L
                        );
                    }
                    return TimeUnit.NANOSECONDS.convert(ttlSeconds, TimeUnit.SECONDS);
                }
                
                @Override
                public long expireAfterUpdate(String key, SeckillVoucherFullModel value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
                
                @Override
                public long expireAfterRead(String key, SeckillVoucherFullModel value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();
    
    public SeckillVoucherFullModel get(String voucherId) {
        return cache.getIfPresent(voucherId);
    }
    
    public void put(String voucherId, SeckillVoucherFullModel voucher) {
        if (voucherId != null && voucher != null) {
            cache.put(voucherId, voucher);
        }
    }
    
    public void invalidate(String voucherId) {
        cache.invalidate(voucherId);
    }
}