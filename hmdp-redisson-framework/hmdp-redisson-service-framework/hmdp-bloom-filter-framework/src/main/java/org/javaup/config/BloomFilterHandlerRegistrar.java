package org.javaup.config;

import cn.hutool.core.collection.CollectionUtil;
import org.javaup.handler.BloomFilterHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

public class BloomFilterHandlerRegistrar implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private final Environment environment;

    public BloomFilterHandlerRegistrar(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Map<String, BloomFilterProperties.Filter> filters = resolveFiltersFromEnvironment();
        if (CollectionUtil.isEmpty(filters)) {
            return;
        }
        filters.forEach((alias, cfg) -> {
            String beanName = StringUtils.hasText(cfg.getName()) ? cfg.getName() : alias;

            RootBeanDefinition bd = new RootBeanDefinition(BloomFilterHandler.class);
            bd.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference("redissonClient"));
            bd.getConstructorArgumentValues().addIndexedArgumentValue(1, beanName);
            bd.getConstructorArgumentValues().addIndexedArgumentValue(2, cfg.getExpectedInsertions());
            bd.getConstructorArgumentValues().addIndexedArgumentValue(3, cfg.getFalseProbability());

            registry.registerBeanDefinition(beanName, bd);
            if (!beanName.equals(alias)) {
                registry.registerAlias(beanName, alias);
            }
        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Map<String, BloomFilterProperties.Filter> resolveFiltersFromEnvironment() {
        Binder binder = Binder.get(environment);
        return binder.bind("bloom-filter.filters",
                Bindable.mapOf(String.class, BloomFilterProperties.Filter.class))
            .orElse(Collections.emptyMap());
    }
}