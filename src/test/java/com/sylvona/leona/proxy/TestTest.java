package com.sylvona.leona.proxy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;

@Slf4j
public class TestTest {

    @SneakyThrows
    @Test
    public void test() {
        AspectClass aspect = new AspectClass();

        CompositedProceedingJoinPoint sourceInterceptor = new CompositedProceedingJoinPoint(aspect::proxyHelloWorld);
        CompositedProceedingJoinPoint sourceInterceptor2 = new CompositedProceedingJoinPoint(aspect::proxyEvan);

        Class<?> dynamicClass = new ByteBuddy()
                .subclass(TestClass.class)
                .method(named("helloWorld"))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named("intercept"))
                        .to(sourceInterceptor, "thisJoinPoint"))
                .method(named("proxyTest"))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named("intercept"))
                        .to(sourceInterceptor2, "thisJointPoint"))
                .make()
                .load(ClassLoader.getSystemClassLoader())
                .getLoaded();

        TestClass testClass2 = (TestClass) dynamicClass.newInstance();

        log.info(testClass2.helloWorld());
    }

    public static class TestClass {
        public TestClass() {
        }

        public String helloWorld() {
            return "Hello World! " + proxyTest();
        }

        public String proxyTest() {
            return "Evan";
        }
    }

    public static class AspectClass {
        @SneakyThrows
        public String proxyHelloWorld(ProceedingJoinPoint proceedingJoinPoint) {
            String result = (String) proceedingJoinPoint.proceed();
            return result + "!!!!!!!!!!!!!!!!!!!!!";
        }

        public String proxyEvan(ProceedingJoinPoint proceedingJoinPoint) {
            return "Mary";
        }
    }


}
