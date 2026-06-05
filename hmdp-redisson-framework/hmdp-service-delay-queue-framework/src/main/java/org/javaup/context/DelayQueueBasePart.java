package org.javaup.context;

import org.javaup.config.DelayQueueProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.redisson.api.RedissonClient;

@Data
@AllArgsConstructor
public class DelayQueueBasePart {
    
    private final RedissonClient redissonClient;
    
    private final DelayQueueProperties delayQueueProperties;
}
