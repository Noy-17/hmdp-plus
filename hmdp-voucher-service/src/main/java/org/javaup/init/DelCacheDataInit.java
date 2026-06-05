package org.javaup.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.cache.SeckillVoucherLocalCache;
import org.javaup.core.RedisKeyManage;
import org.javaup.entity.SeckillVoucher;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillVoucherService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时清空秒杀相关 Redis 缓存（库存标签、用户标签、订阅状态等），确保重启后无脏数据。
 */
@Slf4j
@Order(3)
@Component
public class DelCacheDataInit {

    @Resource
    private RedisCache redisCache;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private SeckillVoucherLocalCache seckillVoucherLocalCache;

    @PostConstruct
    public void init(){
        log.info("==========删除优惠券缓存中的数据==========");
        List<SeckillVoucher> seckillVoucherList = seckillVoucherService.list();
        for (final SeckillVoucher seckillVoucher : seckillVoucherList) {
            RedisKeyBuild seckillVoucherRedisKey =
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, seckillVoucher.getVoucherId());
            seckillVoucherLocalCache.invalidate(seckillVoucherRedisKey.getRelKey());
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, seckillVoucher.getVoucherId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, seckillVoucher.getVoucherId()));
        }
    }
}
