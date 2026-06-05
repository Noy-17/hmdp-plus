package org.javaup.ratelimit.extension;

public enum RateLimitScene {
    /** 发令牌接口 */
    ISSUE_TOKEN,
    /** 下单（秒杀）接口 */
    SECKILL_ORDER
}