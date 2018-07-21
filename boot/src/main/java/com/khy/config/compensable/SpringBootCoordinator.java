package com.khy.config.compensable;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.apache.commons.lang3.StringUtils;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinator;
import org.bytesoft.common.utils.ByteUtils;
import org.bytesoft.common.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

public class SpringBootCoordinator implements InvocationHandler {
	static final Logger logger = LoggerFactory.getLogger(SpringBootCoordinator.class);
	static final String CONSTANT_CONTENT_PATH = "org.bytesoft.bytetcc.contextpath";
	static final String HEADER_PROPAGATION_KEY = "X-PROPAGATION-KEY";
	private String identifier;
	private Environment environment;

	public SpringBootCoordinator() {
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> clazz = method.getDeclaringClass();
		String methodName = method.getName();
		if (Object.class.equals(clazz)) {
			return method.invoke(this, args);
		} else if (RemoteCoordinator.class.equals(clazz)) {
			if ("getIdentifier".equals(methodName)) {
				return this.identifier;
			} else if ("getApplication".equals(methodName)) {
				int firstIndex = this.identifier == null ? -1 : this.identifier.indexOf(":");
				int lastIndex = this.identifier == null ? -1 : this.identifier.lastIndexOf(":");
				String serviceKey = firstIndex > 0 && lastIndex > 0 && firstIndex <= lastIndex
						? (String) this.identifier.subSequence(firstIndex + 1, lastIndex) : (String) null;
				if (this.identifier == null) {
					return null;
				} else {
					return StringUtils.isNotBlank(serviceKey) ? serviceKey
							: this.invokePostCoordinator(proxy, method, args);
				}
			} else {
				throw new XAException(-7);
			}
		} else if (XAResource.class.equals(clazz)) {
			if ("start".equals(methodName)) {
				return null;
			} else if ("prepare".equals(methodName)) {
				return this.invokePostCoordinator(proxy, method, args);
			} else if ("commit".equals(methodName)) {
				return this.invokePostCoordinator(proxy, method, args);
			} else if ("rollback".equals(methodName)) {
				return this.invokePostCoordinator(proxy, method, args);
			} else if ("recover".equals(methodName)) {
				return this.invokeGetCoordinator(proxy, method, args);
			} else if ("forget".equals(methodName)) {
				return this.invokePostCoordinator(proxy, method, args);
			} else {
				throw new XAException(-7);
			}
		} else {
			throw new IllegalAccessException();
		}
	}

	public Object invokePostCoordinator(Object proxy, Method method, Object[] args) throws Throwable {
		Class returnType = method.getReturnType();

		XAException xaEx;
		String errorText;
		try {
			RestTemplate transactionRestTemplate = SpringBootBeanRegistry.getInstance().getRestTemplate();
			RestTemplate restTemplate = transactionRestTemplate == null ? new RestTemplate() : transactionRestTemplate;
			StringBuilder ber = new StringBuilder();
			errorText = this.identifier.matches("^[^\\:]+\\:\\d+\\:[^\\:]+$") ? this.identifier : null;
			String servId;
			int i;
			int lastIndex;
			String prefix;
			if (StringUtils.isNotBlank(errorText)) {
				i = errorText.indexOf(":");
				lastIndex = errorText.lastIndexOf(":");
				prefix = i <= 0 ? null : errorText.substring(0, i);
				servId = i > 0 && lastIndex > 0 && i < lastIndex ? errorText.substring(i + 1, lastIndex) : null;
				String suffix = lastIndex <= 0 ? null : errorText.substring(lastIndex + 1);
				ber.append("http://");
				ber.append(prefix != null && suffix != null ? prefix + ":" + suffix : null);
				ber.append("/rest/api/").append(servId).append("/");
			} else {
				i = this.identifier.indexOf(":");
				lastIndex = this.identifier.lastIndexOf(":");
				prefix = i <= 0 ? null : this.identifier.substring(0, i);
				servId = lastIndex <= 0 ? null : this.identifier.substring(lastIndex + 1);
				ber.append("http://");
				ber.append(prefix != null && servId != null ? prefix + ":" + servId : null);
				ber.append("/org/bytesoft/bytetcc/");
			}

			ber.append(method.getName());

			for (i = 0; i < args.length; ++i) {
				Serializable arg = (Serializable) args[i];
				ber.append("/").append(this.serialize(arg));
			}

			ResponseEntity<?> response = restTemplate.postForEntity(ber.toString(), (Object) null, returnType,
					new Object[0]);
			return response.getBody();
		} catch (HttpClientErrorException var15) {
			xaEx = new XAException(-7);
			xaEx.initCause(var15);
			throw xaEx;
		} catch (HttpServerErrorException var16) {
			HttpHeaders headers = var16.getResponseHeaders();
			String failureText = StringUtils.trimToNull(headers.getFirst("failure"));
			errorText = StringUtils.trimToNull(headers.getFirst("XA_XAER"));
			Boolean failure = failureText == null ? null : Boolean.parseBoolean(failureText);
			Integer errorCode = null;

			try {
				errorCode = errorText == null ? null : Integer.parseInt(errorText);
			} catch (Exception var14) {
				logger.debug(var14.getMessage());
			}

			if (failure != null && errorCode != null) {
				xaEx = new XAException(errorCode.intValue());
				xaEx.initCause(var16);
				throw xaEx;
			} else {
				xaEx = new XAException(-3);
				xaEx.initCause(var16);
				throw xaEx;
			}
		} catch (Exception var17) {
			xaEx = new XAException(-3);
			xaEx.initCause(var17);
			throw xaEx;
		}
	}

	public Object invokeGetCoordinator(Object proxy, Method method, Object[] args) throws Throwable {
		Class returnType = method.getReturnType();

		XAException xaEx;
		String errorText;
		try {
			RestTemplate transactionRestTemplate = SpringBootBeanRegistry.getInstance().getRestTemplate();
			RestTemplate restTemplate = transactionRestTemplate == null ? new RestTemplate() : transactionRestTemplate;
			StringBuilder ber = new StringBuilder();
			errorText = this.identifier.matches("^[^\\:]+\\:\\d+\\:[^\\:]+$") ? this.identifier : null;
			String servId;
			int i;
			int lastIndex;
			String prefix;
			if (StringUtils.isNotBlank(errorText)) {
				i = errorText.indexOf(":");
				lastIndex = errorText.lastIndexOf(":");
				prefix = i <= 0 ? null : errorText.substring(0, i);
				servId = i > 0 && lastIndex > 0 && i < lastIndex ? errorText.substring(i + 1, lastIndex) : null;
				String suffix = lastIndex <= 0 ? null : errorText.substring(lastIndex + 1);
				ber.append("http://");
				ber.append(prefix != null && suffix != null ? prefix + ":" + suffix : null);
				ber.append("/rest/api/").append(servId).append("/");
			} else {
				i = this.identifier.indexOf(":");
				lastIndex = this.identifier.lastIndexOf(":");
				prefix = i <= 0 ? null : this.identifier.substring(0, i);
				servId = lastIndex <= 0 ? null : this.identifier.substring(lastIndex + 1);
				ber.append("http://");
				ber.append(prefix != null && servId != null ? prefix + ":" + servId : null);
				ber.append("/org/bytesoft/bytetcc/");
			}

			ber.append(method.getName());

			for (i = 0; i < args.length; ++i) {
				Serializable arg = (Serializable) args[i];
				ber.append("/").append(this.serialize(arg));
			}

			ResponseEntity<?> response = restTemplate.getForEntity(ber.toString(), returnType, new Object[0]);
			return response.getBody();
		} catch (HttpClientErrorException var15) {
			xaEx = new XAException(-7);
			xaEx.initCause(var15);
			throw xaEx;
		} catch (HttpServerErrorException var16) {
			HttpHeaders headers = var16.getResponseHeaders();
			String failureText = StringUtils.trimToNull(headers.getFirst("failure"));
			errorText = StringUtils.trimToNull(headers.getFirst("XA_XAER"));
			Boolean failure = failureText == null ? null : Boolean.parseBoolean(failureText);
			Integer errorCode = null;

			try {
				errorCode = errorText == null ? null : Integer.parseInt(errorText);
			} catch (Exception var14) {
				logger.debug(var14.getMessage());
			}

			if (failure != null && errorCode != null) {
				xaEx = new XAException(errorCode.intValue());
				xaEx.initCause(var16);
				throw xaEx;
			} else {
				xaEx = new XAException(-3);
				xaEx.initCause(var16);
				throw xaEx;
			}
		} catch (Exception var17) {
			xaEx = new XAException(-3);
			xaEx.initCause(var17);
			throw xaEx;
		}
	}

	private String serialize(Serializable arg) throws IOException {
		if (Xid.class.isInstance(arg)) {
			Xid xid = (Xid) arg;
			byte[] globalTransactionId = xid.getGlobalTransactionId();
			return ByteUtils.byteArrayToString(globalTransactionId);
		} else if (!Integer.class.isInstance(arg) && !Integer.TYPE.isInstance(arg)) {
			if (!Boolean.class.isInstance(arg) && !Boolean.TYPE.isInstance(arg)) {
				byte[] byteArray = CommonUtils.serializeObject(arg);
				return ByteUtils.byteArrayToString(byteArray);
			} else {
				return String.valueOf(arg);
			}
		} else {
			return String.valueOf(arg);
		}
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}