package org.javaup.enums;

import lombok.Getter;

public enum VoucherStatus {
    /**
     * 优惠券状态 
     * */
    AVAILABLE(1, "上架"),
    UNAVAILABLE(2, "下架"),
    EXPIRED(3, "过期");
    
    ;
    
    @Getter
    private final Integer code;
    
    private String msg = "";
    
    VoucherStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (VoucherStatus re : VoucherStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static VoucherStatus getRc(Integer code) {
        for (VoucherStatus re : VoucherStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
