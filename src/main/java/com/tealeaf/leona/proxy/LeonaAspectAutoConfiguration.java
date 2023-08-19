package com.tealeaf.leona.proxy;

import com.tealeaf.leona.proxy.aspects.AspectProducerAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(AspectProducerAspect.class)
public class LeonaAspectAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ProxyMachine cachingProxyMachine(ApplicationContext applicationContext) {
        return new CachingBeanProxyMachine(applicationContext);
    }
}
