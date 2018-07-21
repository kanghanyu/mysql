package com.khy.config.compensable.serialize;

import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bytesoft.bytejta.supports.resource.RemoteResourceDescriptor;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinator;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinatorRegistry;
import org.bytesoft.transaction.supports.resource.XAResourceDescriptor;
import org.bytesoft.transaction.supports.serialize.XAResourceDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import com.khy.config.compensable.SpringBootCoordinator;

public class XAResourceDeserializerImpl implements XAResourceDeserializer, ApplicationContextAware, EnvironmentAware {
	static final Logger logger = LoggerFactory.getLogger(XAResourceDeserializerImpl.class);
	static Pattern pattern = Pattern.compile("^[^:]+\\s*:\\s*[^:]+\\s*:\\s*\\d+$");
	private XAResourceDeserializer resourceDeserializer;
	private Environment environment;
	private ApplicationContext applicationContext;

	public XAResourceDeserializerImpl() {
	}

	public XAResourceDescriptor deserialize(String identifier) {
		XAResourceDescriptor resourceDescriptor = this.resourceDeserializer.deserialize(identifier);
		if (resourceDescriptor != null) {
			return resourceDescriptor;
		} else {
			Matcher matcher = pattern.matcher(identifier);
			if (!matcher.find()) {
				logger.error("can not find a matching xa-resource(identifier= {})!", identifier);
				return null;
			} else {
				RemoteCoordinatorRegistry registry = RemoteCoordinatorRegistry.getInstance();
				RemoteCoordinator coordinator = registry.getRemoteCoordinator(identifier);
				if (coordinator == null) {
					SpringBootCoordinator springCloudCoordinator = new SpringBootCoordinator();
					springCloudCoordinator.setIdentifier(identifier);
					springCloudCoordinator.setEnvironment(this.environment);
					coordinator = (RemoteCoordinator) Proxy.newProxyInstance(
							SpringBootCoordinator.class.getClassLoader(), new Class[] { RemoteCoordinator.class },
							springCloudCoordinator);
					registry.putRemoteCoordinator(identifier, coordinator);
				}

				RemoteResourceDescriptor descriptor = new RemoteResourceDescriptor();
				descriptor.setIdentifier(identifier);
				descriptor.setDelegate(registry.getRemoteCoordinator(identifier));
				return descriptor;
			}
		}
	}

	public XAResourceDeserializer getResourceDeserializer() {
		return this.resourceDeserializer;
	}

	public void setResourceDeserializer(XAResourceDeserializer resourceDeserializer) {
		this.resourceDeserializer = resourceDeserializer;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}
}
