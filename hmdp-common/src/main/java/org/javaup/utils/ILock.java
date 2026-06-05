package org.javaup.utils;

public interface ILock {

    boolean tryLock(long timeoutSec);

    void unlock();
}
