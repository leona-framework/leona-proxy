package com.tealeaf.leona.proxy.aspects;

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
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AspectAware {
    Class<?>[] value() default void.class;
}
