package org.javaup.enums;

import lombok.Getter;

public enum BusinessType {
    /**
     * 业务类型
     * */
    SUCCESS(1, "创建订单成功"),
    TIMEOUT(2, "创建订单超时"),
    FAIL(3, "创建订单失败"),
    CANCEL(4, "主动取消"),
   
    
    ;
    
    @Getter
    private final Integer code;
    
    private String msg = "";
    
    BusinessType(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (BusinessType re : BusinessType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static BusinessType getRc(Integer code) {
        for (BusinessType re : BusinessType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
