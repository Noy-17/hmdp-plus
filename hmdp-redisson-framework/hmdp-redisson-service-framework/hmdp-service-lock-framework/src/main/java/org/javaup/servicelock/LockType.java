package org.javaup.servicelock;

public enum LockType {
    /**
     * 锁类型
     */
    Reentrant,
    
    Fair,
   
    Read,
    
    Write;

    LockType() {
    }

}
