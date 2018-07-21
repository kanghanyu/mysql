package com.khy.config.compensable;

import java.lang.reflect.Proxy;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinator;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinatorRegistry;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

public final class SpringBootBeanRegistry implements CompensableBeanFactoryAware, EnvironmentAware {
    static final Logger logger = LoggerFactory.getLogger(SpringBootBeanRegistry.class);
    
    private static final SpringBootBeanRegistry instance = new SpringBootBeanRegistry();
    
    @Inject  //和@Autowired 功能差不多
    private CompensableBeanFactory beanFactory;
    private RestTemplate restTemplate;
    private Environment environment;

    private SpringBootBeanRegistry() {
        if (instance != null) {
            throw new IllegalStateException();
        }
    }

    public static SpringBootBeanRegistry getInstance() {
        return instance;
    }

    public RemoteCoordinator getConsumeCoordinator(String identifier) {
        RemoteCoordinatorRegistry registry = RemoteCoordinatorRegistry.getInstance();
        if (StringUtils.isBlank(identifier)) {
            return null;
        } else {
            RemoteCoordinator coordinator = registry.getRemoteCoordinator(identifier);
            if (coordinator != null) {
                return coordinator;
            } else {
                SpringBootCoordinator handler = new SpringBootCoordinator();
                handler.setIdentifier(identifier);
                handler.setEnvironment(this.environment);
                coordinator = (RemoteCoordinator)Proxy.newProxyInstance(SpringBootCoordinator.class.getClassLoader(), new Class[]{RemoteCoordinator.class}, handler);
                registry.putRemoteCoordinator(identifier, coordinator);
                return coordinator;
            }
        }
    }

    public RestTemplate getRestTemplate() {
        return this.restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setBeanFactory(CompensableBeanFactory tbf) {
        this.beanFactory = tbf;
    }

    public CompensableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}