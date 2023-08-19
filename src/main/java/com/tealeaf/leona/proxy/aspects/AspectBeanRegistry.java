package com.tealeaf.leona.proxy.aspects;

import com.tealeaf.leona.core.utils.AnnotationHelper;
import com.tealeaf.leona.proxy.CachingBeanProxyMachine;
import com.tealeaf.leona.proxy.ProxyMachine;
import com.tealeaf.leona.proxy.ReflectionFieldsCopier;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AspectBeanRegistry extends AbstractAdvisorAutoProxyCreator {
    private final Map<Class<?>, List<Advisor>> advisorsByClassMap = new HashMap<>();
    private final List<Advisor> advisors = new ArrayList<>();
    private final ReflectionFieldsCopier fieldCopier = new ReflectionFieldsCopier();
    private final ApplicationContext applicationContext;
    private final ProxyMachine proxyMachine;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
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
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Unable to create proxy for bean \"{}\" ({})", bean, beanName);
            log.error("Caused by", e);
            return bean;
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!AnnotationHelper.hasAnnotation(bean, Aspect.class)) return bean;
        BeanFactory beanFactory = getBeanFactory();
        if (beanFactory == null) return bean;

        MetadataAwareAspectInstanceFactory instanceFactory = new BeanFactoryAspectInstanceFactory(beanFactory, beanName);
        ReflectiveAspectJAdvisorFactory reflectiveAspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory();
        advisors.addAll(reflectiveAspectJAdvisorFactory.getAdvisors(instanceFactory));
        return bean;
    }

    public List<Advisor> getAdvisorsForClass(Class<?> cls) {
        return advisorsByClassMap.computeIfAbsent(cls, c -> AopUtils.findAdvisorsThatCanApply(advisors, c));
    }
}
