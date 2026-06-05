package org.javaup.enums;

import lombok.Getter;

public enum ReconciliationStatus {
    /**
     * 对账状态
     * */
    PENDING(1, "待处理"),
    ABNORMAL(2, "异常"),
    INCONSISTENT(3, "不一致"),
    CONSISTENT(4, "一致"),
    
    ;
    
    @Getter
    private final Integer code;
    
    private String msg = "";
    
    ReconciliationStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (ReconciliationStatus re : ReconciliationStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static ReconciliationStatus getRc(Integer code) {
        for (ReconciliationStatus re : ReconciliationStatus.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
