package org.javaup.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;


public class HmdpCommonAutoConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustom(){
        return new JacksonCustom();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
