package org.javaup.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.entity.SeckillVoucher;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.service.ISeckillVoucherService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;

/**
 * 启动时将所有秒杀优惠券 ID 加载到布隆过滤器，拦截非法优惠券请求。
 */
@Slf4j
@Order(1)
@Component
public class BloomFilterDataInit {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @PostConstruct
    public void init() {
        log.info("==========初始化优惠券的布隆过滤器==========");
        List<SeckillVoucher> seckillVoucherlist = seckillVoucherService.list();
        for (SeckillVoucher seckillVoucher : seckillVoucherlist) {
            bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).add(String.valueOf(seckillVoucher.getVoucherId()));
        }
    }
}
