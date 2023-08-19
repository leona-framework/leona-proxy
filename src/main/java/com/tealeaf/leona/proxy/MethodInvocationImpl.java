package com.tealeaf.leona.proxy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

@Getter
@RequiredArgsConstructor
public class MethodInvocationImpl implements MethodInvocation {
    private final Method method;
    private final Object[] arguments;
    private final Object thisObject;

    @Nullable
    @Override
    public Object proceed() throws Throwable {
        return null;
    }

    @Nullable
    @Override
    public Object getThis() {
        return thisObject;
    }

    @Nonnull
    @Override
    public AccessibleObject getStaticPart() {
        return null;
    }
}
