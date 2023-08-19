package com.sylvona.leona.proxy;

import org.springframework.aop.Advisor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * An interface series responsible for creating proxy objects by dynamically generating subclasses.
 */
public interface ProxyMachine {
    /**
     * Creates a proxy object for the given source object with the provided advisors and configuration.
     *
     * @param source              The source object to be proxied.
     * @param advisors            The list of advisors to be applied to the proxy.
     * @param useCopyConstructor  Whether to use a copy constructor for proxy creation.
     * @param useSpringAutowiring Whether to use Spring's autowiring for proxy creation.
     * @return The created proxy object.
     * @throws InstantiationException        If an error occurs during instantiation.
     * @throws IllegalAccessException      If access to a class, field, method is denied.
     * @throws NoSuchMethodException         If a required method cannot be found.
     * @throws InvocationTargetException    If an exception occurs during method invocation.
     */
    Object create(Object source, List<Advisor> advisors, boolean useCopyConstructor, boolean useSpringAutowiring) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException;
}
