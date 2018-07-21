package com.khy.config.compensable.config;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.bytesoft.bytetcc.CompensableCoordinator;
import org.bytesoft.bytetcc.logging.SampleCompensableLogger;
import org.bytesoft.bytetcc.supports.spring.CompensableBeanPostProcessor;
import org.bytesoft.bytetcc.work.CleanupWork;
import org.bytesoft.common.utils.CommonUtils;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.aware.CompensableEndpointAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.khy.config.compensable.SpringBootBeanRegistry;
import com.khy.config.compensable.controller.CompensableCoordinatorController;
import com.khy.config.compensable.interceptor.CompensableHandlerInterceptor;
import com.khy.config.compensable.interceptor.CompensableRequestInterceptor;

public class SpringBootConfiguration extends WebMvcConfigurerAdapter
		implements InitializingBean, CompensableEndpointAware, EnvironmentAware, ApplicationContextAware {
	private ApplicationContext applicationContext;
	private String identifier;
	private Environment environment;

	public SpringBootConfiguration() {
	}

	public void afterPropertiesSet() throws Exception {
		String host = CommonUtils.getInetAddress();
		String name = this.environment.getProperty("spring.application.name");
		String port = this.environment.getProperty("server.port");
		this.identifier = String.format("%s:%s:%s", host, name, port);
	}

	@Bean
	public CompensableBeanPostProcessor compensableBeanPostProcessor() {
		return new CompensableBeanPostProcessor();
	}

	@Bean
	public BeanFactoryPostProcessor loggerPostProcessor() {
		String baseDir = this.environment.getProperty("user.home");
		String dirName = this.identifier.replaceAll("\\:", "-");
		File homeDir = new File(baseDir);
		File tmgrDir = new File(homeDir, "bytetcc");
		final File directory = new File(tmgrDir, dirName);
		return new BeanFactoryPostProcessor() {
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				String[] beanNameArray = beanFactory.getBeanDefinitionNames();

				for (int i = 0; i < beanNameArray.length; ++i) {
					String beanName = beanNameArray[i];
					BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
					String clazzName = beanDef.getBeanClassName();
					MutablePropertyValues mpv = beanDef.getPropertyValues();
					if (!mpv.contains("directory")) {
						if (StringUtils.equals(SampleCompensableLogger.class.getName(), clazzName)) {
							mpv.add("directory", directory);
						} else if (StringUtils.equals(CleanupWork.class.getName(), clazzName)) {
							mpv.add("directory", directory);
						}
					}
				}

			}
		};
	}

	// @Bean
	// public CompensableServiceUtils compensableServiceUtils(@Autowired
	// RestTemplate restTemplate) {
	// CompensableServiceUtils compensableServiceUtils =
	// CompensableServiceUtils.getInstance();
	// compensableServiceUtils.setRestTemplate(restTemplate);
	// return compensableServiceUtils;
	// }

	@Bean
	public CompensableCoordinatorController compensableCoordinatorController(@Autowired CompensableBeanFactory tbf,
			@Autowired CompensableCoordinator compensableCoordinator) {
		CompensableCoordinatorController compensable = new CompensableCoordinatorController();
		compensable.setBeanFactory(tbf);
		compensable.setCompensableCoordinator(compensableCoordinator);
		return compensable;
	}

	@Bean
	public ServletRegistrationBean coordinatorServlet(@Autowired CompensableCoordinatorController controller) {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		registrationBean.setServlet(controller);
		registrationBean.addUrlMappings(new String[] { "/org/bytesoft/bytetcc/*" });
		return registrationBean;
	}

	@Bean
	public CompensableHandlerInterceptor compensableHandlerInterceptor() {
		CompensableHandlerInterceptor interceptor = new CompensableHandlerInterceptor();
		interceptor.setEndpoint(this.identifier);
		return interceptor;
	}

	@Bean
	public CompensableRequestInterceptor compensableRequestInterceptor() {
		CompensableRequestInterceptor interceptor = new CompensableRequestInterceptor();
		interceptor.setEndpoint(this.identifier);
		return interceptor;
	}

	@ConditionalOnMissingBean({ ClientHttpRequestFactory.class })
	@Bean
	public ClientHttpRequestFactory defaultRequestFactory() {
		return new Netty4ClientHttpRequestFactory();
	}

	@Bean({ "compensableRestTemplate" })
	public RestTemplate transactionTemplate(@Autowired ClientHttpRequestFactory requestFactory) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(requestFactory);
		SpringBootBeanRegistry registry = SpringBootBeanRegistry.getInstance();
		registry.setRestTemplate(restTemplate);
		return restTemplate;
	}

	@Primary
	@Bean
	public RestTemplate defaultRestTemplate(@Autowired CompensableRequestInterceptor compensableRequestInterceptor) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(compensableRequestInterceptor);
		return restTemplate;
	}

	public void addInterceptors(InterceptorRegistry registry) {
		CompensableHandlerInterceptor compensableHandlerInterceptor = (CompensableHandlerInterceptor) this.applicationContext
				.getBean(CompensableHandlerInterceptor.class);
		registry.addInterceptor(compensableHandlerInterceptor);
	}

	public void setEndpoint(String identifier) {
		this.identifier = identifier;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}