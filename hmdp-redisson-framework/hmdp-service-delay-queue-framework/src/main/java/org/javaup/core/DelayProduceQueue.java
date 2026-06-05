package org.javaup.core;

import org.redisson.Redisson;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RReliableQueue;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.PlainOptions;
import org.redisson.client.codec.Codec;

import java.util.concurrent.TimeUnit;

public class DelayProduceQueue extends DelayBaseQueue{
    
    /**
     * 虽然 RDelayedQueue 这里提示了是过期，建议使用{@link RReliableQueue}来代替，
     * <p>
     * 但是，RReliableQueue 是 pro 版本才能使用的功能，社区版本如果使用会直接抛出“不支持的”异常：
     * <p>
     * 源码位置：
     * <p>
     * <ul>
     *     <li>{@link Redisson#getReliableQueue(String)}</li>
     *     <li>{@link Redisson#getReliableQueue(String, Codec)}</li>
     *     <li>{@link Redisson#getReliableQueue(PlainOptions)}</li>
     * </ul>
     * 官网说明：
     * <p>
     * <ul>
     *     <li><a href="https://redisson.pro/feature-comparison.html">https://redisson.pro/feature-comparison.html</a></li>
     *     <li><a href="https://redisson.pro/docs/data-and-services/queues/">https://redisson.pro/docs/data-and-services/queues</a></li>
     * </ul>
     * 
     * 使用 RDelayedQueue 也是足够了，因为我们可以使用消息对账功能来完善可靠性，大麦项目就是这么做的
     * */
    private final RDelayedQueue<String> delayedQueue;
    public DelayProduceQueue(RedissonClient redissonClient, final String relTopic) {
        super(redissonClient, relTopic);
        this.delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
    }
    
    public void offer(String content, long delayTime, TimeUnit timeUnit) {
        delayedQueue.offer(content,delayTime,timeUnit);
    }
}
