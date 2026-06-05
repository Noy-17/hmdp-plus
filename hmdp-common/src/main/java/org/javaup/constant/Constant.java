package org.javaup.constant;

public class Constant {
    
    public static final String PREFIX_DISTINCTION_NAME = "prefix.distinction.name";
    
    public static final String DEFAULT_PREFIX_DISTINCTION_NAME = "hmdp";
    
    public static final String SPRING_INJECT_PREFIX_DISTINCTION_NAME = "${"+PREFIX_DISTINCTION_NAME+":"+DEFAULT_PREFIX_DISTINCTION_NAME+"}";
    
    public static final String SECKILL_VOUCHER_TOPIC = "seckill_voucher_topic";
    
    public static final String SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC = "seckill_voucher_cache_invalidation_topic";
    
    public static final String BLOOM_FILTER_HANDLER_SHOP = "shop";
    
    public static final String BLOOM_FILTER_HANDLER_VOUCHER = "voucher";
    
    public static final String DELAY_VOUCHER_REMINDER ="h_delay_voucher_reminder";
}
