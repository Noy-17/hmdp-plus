package org.javaup.servicelock.info;

public interface LockTimeOutHandler {
    
    /**
     * 处理
     * @param lockName 锁名
     * */
    void handler(String lockName);
}
