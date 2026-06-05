package org.javaup.enums;

import lombok.Getter;

public enum SeckillVoucherOrderOperate {
    /**
     * 是否删除秒杀优惠券订单记录
     * */
    NO(0, "不删除"),
    YES(1, "删除"),
    ;
    
    @Getter
    private final Integer code;
    
    private String msg = "";
    
    SeckillVoucherOrderOperate(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (SeckillVoucherOrderOperate re : SeckillVoucherOrderOperate.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static SeckillVoucherOrderOperate getRc(Integer code) {
        for (SeckillVoucherOrderOperate re : SeckillVoucherOrderOperate.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
