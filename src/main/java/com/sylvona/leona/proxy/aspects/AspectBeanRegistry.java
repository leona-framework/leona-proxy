package com.sylvona.leona.proxy.aspects;

import com.sylvona.leona.core.utils.AnnotationHelper;
import com.sylvona.leona.proxy.ProxyMachine;
import com.sylvona.leona.proxy.ReflectionFieldsCopier;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectInstanceFactory;
import org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory;
import org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A bean post processor that identifies and process classes with the {@link Aspect} or {@link AspectAware} annotations.
 * This class had the added side effect of storing all {@link Advisor} created by Spring.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AspectBeanRegistry extends AbstractAdvisorAutoProxyCreator {
    /**
     * Contains and caches all advisors that are applicable to a specific type.
     */
    private final Map<Class<?>, List<Advisor>> advisorsByClassMap = new HashMap<>();
    /**
     * The object-to-object field value copier.
     */
    private final ReflectionFieldsCopier fieldCopier = new ReflectionFieldsCopier();
    /**
     * All advisors found by this post processor.
     */
    private final List<Advisor> advisors = new ArrayList<>();
    /**
     * The spring {@link ApplicationContext}.
     */
    private final ApplicationContext applicationContext;
    /**
     * The {@link ProxyMachine} responsible for creating bean proxies.
     */
    private final ProxyMachine proxyMachine;

    /**
     * Processes and attempts to proxy a bean, making it aware of itself as a proxy class.
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return the processed bean, or original bean if a proxy couldn't be made
     * @throws BeansException If an error occurs during bean processing.
     */
    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        // Implicit null check
        AspectAware aspectAware = AnnotationHelper.getNestedAnnotation(bean, AspectAware.class);
        if (aspectAware == null) return bean;

        for (Class<?> aspectClass : aspectAware.value()) {
            if (aspectClass.isPrimitive()) continue;
            applicationContext.getBean(aspectClass); // Load all necessary aspects
        }

        // TODO: possibly fix load order to include all aspects... perhaps by doing a lazy return on the modified bean until all aspects have bene loaded

        List<Advisor> advisors = getAdvisorsForClass(bean.getClass());
        if (advisors.isEmpty()) return bean;
        try {
            Object proxy = proxyMachine.create(bean, advisors, false, true);
            fieldCopier.copyFieldValues(bean, proxy);
            return proxy;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error("Unable to create proxy for bean \"{}\" ({})", bean, beanName);
            log.error("Caused by", e);
            return bean;
        }
    }

    /**
     * Finds all beans annotated with {@link Aspect} and registers any {@link Advisor} associated with the class.
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return the original (unmodified) bean
     * @throws BeansException If an error occurs during bean processing.
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, @NotNull String beanName) throws BeansException {
        if (!AnnotationHelper.hasAnnotation(bean, Aspect.class)) return bean;
        BeanFactory beanFactory = getBeanFactory();
        if (beanFactory == null) return bean;

        MetadataAwareAspectInstanceFactory instanceFactory = new BeanFactoryAspectInstanceFactory(beanFactory, beanName);
        ReflectiveAspectJAdvisorFactory reflectiveAspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory();
        advisors.addAll(reflectiveAspectJAdvisorFactory.getAdvisors(instanceFactory));
        return bean;
    }

    /**
     * Get the list of advisors applicable to the given class.
     *
     * @param cls The class for which advisors are to be retrieved.
     * @return The list of applicable advisors.
     */
    public List<Advisor> getAdvisorsForClass(Class<?> cls) {
        return advisorsByClassMap.computeIfAbsent(cls, c -> AopUtils.findAdvisorsThatCanApply(advisors, c));
    }
}
