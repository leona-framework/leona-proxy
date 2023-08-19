package com.tealeaf.leona.proxy.aspects;

import com.tealeaf.leona.proxy.ProxyMachine;
import com.tealeaf.leona.proxy.ReflectionFieldsCopier;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Import(AspectBeanRegistry.class)
public class AspectProducerAspect {
    private final ReflectionFieldsCopier fieldCopier = new ReflectionFieldsCopier();
    private final AspectBeanRegistry aspectBeanRegistry;
    private final ProxyMachine proxyMachine;

    @Pointcut("within(@com.tealeaf.leona.proxy.aspects.ProxyProducer *)")
    public void findAspectProducers() {}

    @Pointcut("execution(!void *(..)) && !execution(@com.tealeaf.leona.proxy.aspects.ProxyProducer.ExcludeAlways * *(..))")
    public void findNonVoidMethods() {}

    @Pointcut("findAspectProducers() && findNonVoidMethods()")
    public void findNonVoidMethodsInAspectProducer() {}

    @Around("findNonVoidMethodsInAspectProducer()")
    public Object wrapAspectProducer(ProceedingJoinPoint joinPoint) throws Throwable {
        Object original = joinPoint.proceed();
        if (AopUtils.isAopProxy(original)) return original;
        if (original == null) return null;

        MethodSignature signature = ((MethodSignature)joinPoint.getSignature());
        Method method = signature.getMethod();
        ProxyProducer proxyProducer = method.getDeclaringClass().getAnnotation(ProxyProducer.class);

        // Check if the result of the resulting class type is one of the allowed target types (if specified)
        Class<?>[] targetTypes = proxyProducer.targetTypes();
        if (targetTypes != null && targetTypes.length > 0 && !targetTypes[0].equals(void.class)) {
            Class<?> returningType = original.getClass();
            if (Arrays.stream(targetTypes).noneMatch(t -> t.isAssignableFrom(returningType))) return original;
        }

        ProxyProducer.ExcludeWhen excludeWhen = method.getAnnotation(ProxyProducer.ExcludeWhen.class);
        if (excludeWhen != null) {
            if (excludeWhen.value().isEmpty()) return original;
            if (invokeExclusionMethod(joinPoint.getThis(), excludeWhen.value(), signature.getParameterTypes(), joinPoint.getArgs())) return original;
        }

        List<Advisor> advisors = aspectBeanRegistry.getAdvisorsForClass(original.getClass());
        if (advisors.isEmpty()) return original;


        Object proxy = proxyMachine.create(original, advisors, proxyProducer.useCopyConstructor(), false);

        if (proxyProducer.useFieldCopying()) {
            fieldCopier.copyFieldValues(original, proxy);
        }

        return proxy;
    }

    private static boolean invokeExclusionMethod(Object source, String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        try {
            return (boolean) MethodUtils.invokeMethod(source, true, methodName, parameters, parameterTypes);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
