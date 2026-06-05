package org.javaup.enums;

import lombok.Getter;

public enum StockUpdateType {
    /**
     * 库存操作类型
     * */
    DECREASE(-1, "扣减"),
    
    INCREASE(1, "增加"),
    ;
    
    @Getter
    private final Integer code;
    
    private String msg = "";
    
    StockUpdateType(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (StockUpdateType re : StockUpdateType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static StockUpdateType getRc(Integer code) {
        for (StockUpdateType re : StockUpdateType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
