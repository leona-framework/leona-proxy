package com.sylvona.leona.proxy.aspects;

import org.aspectj.lang.annotation.Aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attempts to make the annotated class "aware" of its own aspected methods.
 * THis involves the creation of a subclass (as opposed to AOP's usual wrapper class) and thus has more restrictions
 * behind when it is / isn't usable.
 * <p>
 * If successful, the resulting class (when aspected) will be able to call its own aspect methods.
 * <p>
 * Classes with this annotation are only aware of {@link Aspect} classes that were loaded <b>before</b> their own instancing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AspectAware {

    /**
     * {@link Aspect} classes to force Spring to load. If not specified, the class will only be aware of any previously-loaded {@link Aspect} classes.
     */
    Class<?>[] value() default void.class;
}
