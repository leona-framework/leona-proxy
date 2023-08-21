package com.sylvona.leona.proxy.aspects;

import com.sylvona.leona.proxy.CachingBeanProxyMachine;
import com.sylvona.leona.proxy.ProxyMachine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration class for Leona's aspect-related components. This class imports the {@link AspectProducerAspect}
 * to enable the creation of dynamic proxies for aspect producers.
 * <p>
 * Additionally, it provides a default {@link ProxyMachine} bean, specifically a {@link CachingBeanProxyMachine},
 * for handling caching of proxied beans.
 * <p>
 * This configuration is conditionally applied if no existing bean of type {@link ProxyMachine} is present in
 * the application context.
 *
 * @see AspectProducerAspect
 * @see CachingBeanProxyMachine
 * @see ProxyMachine
 */
@Import(AspectProducerAspect.class)
public class LeonaAspectAutoConfiguration {
    /**
     * Creates a default {@link ProxyMachine} bean, specifically a {@link CachingBeanProxyMachine}, for handling
     * caching of proxied beans.
     *
     * @param applicationContext The application context to be used by the proxy machine.
     * @return The configured {@link ProxyMachine} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyMachine cachingProxyMachine(ApplicationContext applicationContext) {
        return new CachingBeanProxyMachine(applicationContext);
    }
}
