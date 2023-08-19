package org.lyora.leona.proxy;

import org.springframework.aop.Advisor;

import java.util.List;

public interface ProxyMachine {
    Object create(Object source, List<Advisor> adviceList, boolean useCopyConstructor, boolean useSpringAutowiring) throws InstantiationException, IllegalAccessException;
}
