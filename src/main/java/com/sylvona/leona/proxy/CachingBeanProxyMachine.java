package com.sylvona.leona.proxy;

import com.sylvona.leona.core.utils.SpringBridgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;


/**
 * The standard implementation of {@link ProxyMachine} which leverages reflections and spring to create a proxy-aware proxy object.
 * This class additionally caches the dynamically created proxy class to optimize subsequent proxy creations.
 */
@Slf4j
@RequiredArgsConstructor
public class CachingBeanProxyMachine implements ProxyMachine {
    /**
     * The cache responsible for containing newly generated proxy classes.
     */
    protected final TypeCache<Class<?>> typeCache = new TypeCache<>();
    private final ApplicationContext applicationContext;
    private final ClassLoader classLoader = getClass().getClassLoader();

    @Override
    public Object create(Object source, List<Advisor> advisors, boolean useCopyConstructor, boolean useSpringAutowiring) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> sourceClass = source.getClass();
        Class<?> proxyClass = typeCache.findOrInsert(classLoader, sourceClass, () -> createDynamicType(source, sourceClass, advisors, useCopyConstructor, useSpringAutowiring).load(classLoader, ClassLoadingStrategy.Default.INJECTION).getLoaded());
        return proxyClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Implementation for generating a dynamic class using a provided {@link Advisor} list.
     *
     * @param source              The source object to be proxied.
     * @param sourceClass         The class of the source object.
     * @param advisors            The list of advisors to be applied to the proxy.
     * @param useCopyConstructor  Whether to use a copy constructor for proxy creation.
     * @param useSpringAutowiring Whether to use Spring's autowiring for proxy creation.
     * @return An unloaded dynamic type representing the proxy class.
     */
    protected DynamicType.Unloaded<?> createDynamicType(Object source, Class<?> sourceClass, List<Advisor> advisors, boolean useCopyConstructor, boolean useSpringAutowiring) {

        DynamicType.Builder<?> typeBuilder = new ByteBuddy().subclass(sourceClass);

        for (Advisor advisor : advisors) {
            Advice advice = advisor.getAdvice();
            if (!(advice instanceof AspectJAroundAdvice aroundAdvice)) continue;
            AspectJExpressionPointcut pointcutExpression = aroundAdvice.getPointcut();

            for (Method method : sourceClass.getDeclaredMethods()) {
                if (!pointcutExpression.matches(method, sourceClass)) continue;

                log.info("Creating interception for method {}", method);
                CompositedProceedingJoinPoint composite = new CompositedProceedingJoinPoint(aroundAdvice.getAspectInstanceFactory().getAspectInstance(), aroundAdvice.getAspectJAdviceMethod());
                MethodDelegation delegation = MethodDelegation.withDefaultConfiguration()
                        .filter(named("intercept"))
                        .to(composite);

                typeBuilder = typeBuilder.method(named(method.getName())).intercept(delegation);
            }
        }

        if (useCopyConstructor) {
            try {
                log.info("Creating custom constructor");
                Constructor<?> constructor = sourceClass.getDeclaredConstructor(sourceClass);
                typeBuilder = typeBuilder.defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(constructor).with(source));

                return typeBuilder.make();
            }
            catch (NoSuchMethodException ignored) {}
        }


        // Used if the object is a spring bean
        if (useSpringAutowiring) {
            Constructor<?> eligibleCtor = SpringBridgeUtils.determineAutowiredConstructor(sourceClass, applicationContext);
            if (eligibleCtor != null) {
                Object[] arguments = SpringBridgeUtils.resolveAutowiredConstructorArguments(eligibleCtor, applicationContext);
                typeBuilder = typeBuilder.defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(eligibleCtor).with(arguments));
                return typeBuilder.make();
            }
        }

        try {
            Constructor<?> constructor = sourceClass.getDeclaredConstructor();
            typeBuilder = typeBuilder.defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(constructor));
            log.info("Created empty custom constructor");
            return typeBuilder.make();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
