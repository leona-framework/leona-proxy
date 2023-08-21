package com.sylvona.leona.proxy;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.implementation.bind.annotation.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Function;

@RequiredArgsConstructor
class CompositedProceedingJoinPoint implements ProceedingJoinPoint {
    private final Function<ProceedingJoinPoint, Object> joinPointFunction;

    private Callable<?> defaultCall;
    private Object thisObject;
    private Object defaultObject;
    private Object[] arguments;
    private MethodSignature methodSignature;

    public CompositedProceedingJoinPoint(Object aspect, Method aspectMethod) {
        joinPointFunction = pjp -> {
            try {
                aspectMethod.setAccessible(true);
                return aspectMethod.invoke(aspect, pjp);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @RuntimeType
    public Object intercept(@This Object thisObject, @Origin Method targetMethod, @SuperCall Callable<?> defaultCall, @Super Object target, @AllArguments Object[] arguments) {
        this.defaultCall = defaultCall;
        this.thisObject = thisObject;
        this.defaultObject = target;
        this.methodSignature = new MethodSignatureImpl(targetMethod);
        this.arguments = arguments;

        return joinPointFunction.apply(this);
    }

    @Override
    public void set$AroundClosure(AroundClosure arc) {}

    @Override
    public Object proceed() throws Throwable {
        return defaultCall.call();
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
        return methodSignature.getMethod().invoke(defaultObject, args);
    }

    @Override
    public String toShortString() {
        return "ProceedingJoinPoint(target=%s)".formatted(methodSignature.getName());
    }

    @Override
    public String toLongString() {
        return toShortString();
    }

    @Override
    public Object getThis() {
        return thisObject;
    }

    @Override
    public Object getTarget() {
        return defaultObject;
    }

    @Override
    public Object[] getArgs() {
        return arguments;
    }

    @Override
    public Signature getSignature() {
        return methodSignature;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public String getKind() {
        return null;
    }

    @Override
    public StaticPart getStaticPart() {
        return null;
    }

    @RequiredArgsConstructor
    private static class MethodSignatureImpl implements MethodSignature {
        private final Method method;
        private String[] parameterNames;

        @Override
        public Class getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            if (parameterNames != null) return parameterNames;
            return parameterNames = Arrays.stream(method.getParameters()).map(Parameter::getName).toArray(String[]::new);
        }

        @Override
        public Class[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return "Signature(name=%s, Parameters=%s)".formatted(method.getName(), Arrays.toString(getParameterNames()));
        }

        @Override
        public String toLongString() {
            return toShortString();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getTypeName();
        }
    }
}
