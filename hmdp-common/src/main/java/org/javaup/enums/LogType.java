package org.javaup.enums;

import lombok.Getter;

public enum LogType {
    /**
     * 记录类型
     * */
    DEDUCT(-1, "扣减"),
    
    RESTORE(1, "恢复"),
    ;
    
    @Getter
    private final Integer code;
    
    private String msg = "";
    
    LogType(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (LogType re : LogType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static LogType getRc(Integer code) {
        for (LogType re : LogType.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
