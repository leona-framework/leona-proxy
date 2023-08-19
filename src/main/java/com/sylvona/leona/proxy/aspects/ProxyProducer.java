package com.sylvona.leona.proxy.aspects;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes with this annotation will have their method return values automatically processed
 * by Spring and Leona's bean proxying system, enabling the dynamic creation of proxies with modified behavior.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AspectAware(AspectProducerAspect.class)
public @interface ProxyProducer {

    /**
     * Determines if the values of fields within a proxied object should be copied over from its source.
     * By default, this is true which means that any field that can be copied over will be copied over. The obvious
     * restriction being final fields.
     * <p>
     * When enabled, reflections will be used to intrusively copy restricted fields. Disabling this, usually means that
     * the resulting object will be initialized with null fields. The upside being that no reflections will be used to set fields, thus
     * saving in performance and useful in tight security contexts.
     * @return true if fields should be copied via reflections, false otherwise
     */
    boolean useFieldCopying() default true;

    /**
     * If true, the proxy will be created and instanced by passing in the original object with the intention of copying its fields.
     * @return true if a copy constructor should be used, false otherwise
     */
    boolean useCopyConstructor() default true;

    /**
     * If specified, only produces proxies for methods that return the specified type(s).
     * <b>Inheritance is taken into account</b>, thus subclasses of declared types will also be considered for return.
     *
     * @return the return types that are allowed to produce proxies
     */
    Class<?>[] targetTypes() default void.class;

    /**
     * Specifies methods that should be excluded from proxying based on conditions defined within the annotation.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ExcludeWhen {
        /**
         * Specifies the filter method that determines whether the annotated method should be excluded from proxying.
         */
        @AliasFor("filterMethod")
        String value() default "";

        /**
         * Specifies the filter method that determines whether the annotated method should be excluded from proxying.
         */
        @AliasFor("value")
        String filterMethod() default "";
    }

    /**
     * Specifies methods that should always be excluded from proxying.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ExcludeAlways { }
}
