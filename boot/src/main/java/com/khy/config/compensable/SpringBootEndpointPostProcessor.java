package com.khy.config.compensable;

import java.util.ArrayList;
import java.util.List;

import org.bytesoft.common.utils.CommonUtils;
import org.bytesoft.compensable.aware.CompensableEndpointAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class SpringBootEndpointPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware {
    static final Logger logger = LoggerFactory.getLogger(SpringBootEndpointPostProcessor.class);
    private Environment environment;

    public SpringBootEndpointPostProcessor() {
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<BeanDefinition> beanDefList = new ArrayList();
        String[] beanNameArray = beanFactory.getBeanDefinitionNames();

        String beanName;
        String identifier;
        for(int i = 0; i < beanNameArray.length; ++i) {
            beanName = beanNameArray[i];
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            identifier = beanDef.getBeanClassName();
            Class beanClass = null;

            try {
                beanClass = cl.loadClass(identifier);
            } catch (Exception var12) {
                logger.debug("Cannot load class {}, beanId= {}!", new Object[]{identifier, beanName, var12});
                continue;
            }

            if (CompensableEndpointAware.class.isAssignableFrom(beanClass)) {
                beanDefList.add(beanDef);
            }
        }

        String host = CommonUtils.getInetAddress();
        beanName = this.environment.getProperty("spring.application.name");
        String port = this.environment.getProperty("server.port");
        identifier = String.format("%s:%s:%s", host, beanName, port);

        for(int i = 0; i < beanDefList.size(); ++i) {
            BeanDefinition beanDef = (BeanDefinition)beanDefList.get(i);
            MutablePropertyValues mpv = beanDef.getPropertyValues();
            mpv.addPropertyValue("endpoint", identifier);
        }

    }
}

