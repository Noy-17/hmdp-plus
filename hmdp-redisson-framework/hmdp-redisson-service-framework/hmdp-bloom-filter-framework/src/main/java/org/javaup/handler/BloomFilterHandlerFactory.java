package org.javaup.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class BloomFilterHandlerFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public BloomFilterHandler get(String name){
        return applicationContext.getBean(name, BloomFilterHandler.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}