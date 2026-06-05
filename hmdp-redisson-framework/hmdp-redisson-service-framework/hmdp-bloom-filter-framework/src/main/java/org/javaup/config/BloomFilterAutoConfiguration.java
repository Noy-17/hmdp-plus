package org.javaup.config;

import org.javaup.handler.BloomFilterHandlerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@EnableConfigurationProperties(BloomFilterProperties.class)
public class BloomFilterAutoConfiguration {
    
    @Bean
    public BloomFilterHandlerFactory bloomFilterHandlerFactory(){
        return new BloomFilterHandlerFactory();
    }

    @Bean
    public BloomFilterHandlerRegistrar bloomFilterHandlerRegistrar(Environment environment){
        return new BloomFilterHandlerRegistrar(environment);
    }
}
