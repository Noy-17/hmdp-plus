package org.javaup.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.entity.SeckillVoucher;
import org.javaup.entity.Shop;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IShopService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_SHOP;
import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;

@Slf4j
@Order(1)
@Component
public class BloomFilterDataInit {
    
    @Resource
    private IShopService shopService;
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    @Resource
    private BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @PostConstruct
    public void init() {
        log.info("==========初始化商铺的布隆过滤器==========");
        List<Shop> shopList = shopService.list();
        for (Shop shop : shopList) {
            bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_SHOP).add(String.valueOf(shop.getId()));
        }
        log.info("==========初始化优惠券的布隆过滤器==========");
        List<SeckillVoucher> seckillVoucherlist = seckillVoucherService.list();
        for (SeckillVoucher seckillVoucher : seckillVoucherlist) {
            bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).add(String.valueOf(seckillVoucher.getVoucherId()));
        }
    }
}
