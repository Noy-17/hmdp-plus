package org.javaup.lockinfo.factory;


import org.javaup.lockinfo.LockInfoHandle;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class LockInfoHandleFactory implements ApplicationContextAware {
    
    private ApplicationContext applicationContext;

    public LockInfoHandle getLockInfoHandle(String lockInfoType){
        return applicationContext.getBean(lockInfoType,LockInfoHandle.class);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
