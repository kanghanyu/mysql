package com.khy.config.compensable.interceptor;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.bytesoft.bytejta.supports.rpc.TransactionRequestImpl;
import org.bytesoft.bytejta.supports.rpc.TransactionResponseImpl;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinator;
import org.bytesoft.bytejta.supports.wire.RemoteCoordinatorRegistry;
import org.bytesoft.bytetcc.CompensableTransactionImpl;
import org.bytesoft.common.utils.ByteUtils;
import org.bytesoft.common.utils.CommonUtils;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.CompensableManager;
import org.bytesoft.compensable.TransactionContext;
import org.bytesoft.compensable.aware.CompensableEndpointAware;
import org.bytesoft.transaction.supports.rpc.TransactionInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;

import com.khy.config.compensable.SpringBootBeanRegistry;
import com.khy.config.compensable.SpringBootCoordinator;

public class CompensableRequestInterceptor
		implements ClientHttpRequestInterceptor, CompensableEndpointAware, ApplicationContextAware {
	static final Logger logger = LoggerFactory.getLogger(CompensableRequestInterceptor.class);
	static final String HEADER_TRANCACTION_KEY = "X-COMPENSABLE-XID";
	static final String HEADER_PROPAGATION_KEY = "X-PROPAGATION-KEY";
	static final String PREFIX_TRANSACTION_KEY = "/org/bytesoft/bytetcc";
	private String identifier;
	private ApplicationContext applicationContext;

	public CompensableRequestInterceptor() {
	}

	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		SpringBootBeanRegistry beanRegistry = SpringBootBeanRegistry.getInstance();
		CompensableBeanFactory beanFactory = beanRegistry.getBeanFactory();
		CompensableManager compensableManager = beanFactory.getCompensableManager();
		CompensableTransactionImpl compensable = (CompensableTransactionImpl) compensableManager
				.getCompensableTransactionQuietly();
		String path = httpRequest.getURI().getPath();
		int position = path.startsWith("/") ? path.indexOf("/", 1) : -1;
		String pathWithoutContextPath = position > 0 ? path.substring(position) : null;
		if (!StringUtils.startsWith(path, "/org/bytesoft/bytetcc")
				&& !StringUtils.startsWith(pathWithoutContextPath, "/org/bytesoft/bytetcc")) {
			if (compensable == null) {
				return execution.execute(httpRequest, body);
			} else if (!compensable.getTransactionContext().isCompensable()) {
				return execution.execute(httpRequest, body);
			} else {
				URI requestURI = httpRequest.getURI();
				String pathInfo = requestURI.getPath();
				int index = pathInfo.indexOf("/rest/api/");
				int beginIndex = index + "/rest/api/".length();
				int endIndex = pathInfo.indexOf(47, beginIndex);
				String serv = pathInfo.substring(beginIndex, endIndex);
				String host = requestURI.getHost();
				int port = requestURI.getPort() < 0 ? 80 : requestURI.getPort();
				String versionKey = String.format("%s:%s:%s", host, serv, port);
				ClientHttpResponse httpResponse = null;
				boolean serverFlag = true;

				ClientHttpResponse var22;
				try {
					this.invokeBeforeSendRequest(httpRequest, versionKey);
					httpResponse = execution.execute(httpRequest, body);
					var22 = httpResponse;
				} catch (HttpClientErrorException var26) {
					serverFlag = false;
					throw var26;
				} finally {
					if (httpResponse != null) {
						this.invokeAfterRecvResponse(httpResponse, versionKey, serverFlag);
					}

				}

				return var22;
			}
		} else {
			return execution.execute(httpRequest, body);
		}
	}

	private void invokeBeforeSendRequest(HttpRequest httpRequest, String versionKey) throws IOException {
		SpringBootBeanRegistry beanRegistry = SpringBootBeanRegistry.getInstance();
		CompensableBeanFactory beanFactory = beanRegistry.getBeanFactory();
		CompensableManager compensableManager = beanFactory.getCompensableManager();
		TransactionInterceptor transactionInterceptor = beanFactory.getTransactionInterceptor();
		CompensableTransactionImpl compensable = (CompensableTransactionImpl) compensableManager
				.getCompensableTransactionQuietly();
		TransactionContext transactionContext = compensable.getTransactionContext();
		byte[] reqByteArray = CommonUtils.serializeObject(transactionContext);
		String reqTransactionStr = ByteUtils.byteArrayToString(reqByteArray);
		HttpHeaders reqHeaders = httpRequest.getHeaders();
		reqHeaders.add("X-COMPENSABLE-XID", reqTransactionStr);
		reqHeaders.add("X-PROPAGATION-KEY", this.identifier);
		TransactionRequestImpl request = new TransactionRequestImpl();
		request.setTransactionContext(transactionContext);
		RemoteCoordinator coordinator = beanRegistry.getConsumeCoordinator(versionKey);
		request.setTargetTransactionCoordinator(coordinator);
		transactionInterceptor.beforeSendRequest(request);
	}

	private void invokeAfterRecvResponse(ClientHttpResponse httpResponse, String versionKey, boolean serverFlag)
			throws IOException {
		SpringBootBeanRegistry beanRegistry = SpringBootBeanRegistry.getInstance();
		CompensableBeanFactory beanFactory = beanRegistry.getBeanFactory();
		TransactionInterceptor transactionInterceptor = beanFactory.getTransactionInterceptor();
		HttpHeaders respHeaders = httpResponse.getHeaders();
		String respTransactionStr = respHeaders.getFirst("X-COMPENSABLE-XID");
		String respPropagationStr = respHeaders.getFirst("X-PROPAGATION-KEY");
		String value = null;

		try {
			String serverInfo = httpResponse.getHeaders().getFirst("X-Server");
			value = new String(Hex.decodeHex(serverInfo.toCharArray()));
		} catch (Exception var30) {
			logger.warn(var30.getMessage(), var30);
		}

		String[] keyArray = respPropagationStr == null ? new String[0] : respPropagationStr.split("\\:");
		String element1 = keyArray.length == 3 ? keyArray[0] : null;
		String element2 = keyArray.length == 3 ? keyArray[1] : null;
		String element3 = keyArray.length == 3 ? keyArray[2] : null;
		String[] xserverArray = value == null ? new String[] { element1 } : value.split(":");
		String sourceKey = null;
		if (StringUtils.equalsIgnoreCase("localhost", element1)) {
			sourceKey = respPropagationStr == null ? null
					: String.format("%s:%s:%s", xserverArray[0], element2, element3);
		} else if (StringUtils.trimToEmpty(element1).startsWith("127")) {
			sourceKey = respPropagationStr == null ? null
					: String.format("%s:%s:%s", xserverArray[0], element2, element3);
		} else {
			sourceKey = respPropagationStr;
		}

		String instanceId = null;
		String targetAddr;
		String targetName;
		if (sourceKey == null) {
			int splitIndex = value == null ? -1 : value.indexOf(":");
			targetAddr = splitIndex < 0 ? null : value.substring(0, splitIndex);
			targetName = splitIndex < 0 ? null : value.substring(splitIndex + 1);
			instanceId = targetAddr != null && targetName != null
					? String.format("%s:%s:%s", targetAddr, null, targetName) : value;
		} else {
			instanceId = sourceKey;
		}

		String[] values = instanceId == null ? new String[0] : instanceId.split("\\s*:\\s*");
		targetAddr = values.length == 3 ? values[0] : "";
		targetName = values.length == 3 ? values[1] : "";
		String targetPort = values.length == 3 ? values[2] : String.valueOf(0);
		String remoteAddr = StringUtils.isBlank(targetAddr) && StringUtils.isBlank(targetPort) ? ""
				: String.format("%s:%s", targetAddr, targetPort);
		RemoteCoordinatorRegistry coordinatorRegistry = RemoteCoordinatorRegistry.getInstance();
		coordinatorRegistry.putApplication(remoteAddr, targetName);
		coordinatorRegistry.putRemoteAddr(instanceId, remoteAddr);
		RemoteCoordinator versionCoordinator = SpringBootBeanRegistry.getInstance().getConsumeCoordinator(versionKey);
		if (versionCoordinator != null && sourceKey != null
				&& !StringUtils.equals(instanceId, versionCoordinator.getIdentifier())) {
			SpringBootCoordinator bootCoordinator = (SpringBootCoordinator) Proxy
					.getInvocationHandler(versionCoordinator);
			bootCoordinator.setIdentifier(instanceId);
		}

		RemoteCoordinator remoteCoordinator = coordinatorRegistry.getRemoteCoordinatorByAddr(remoteAddr);
		SpringBootCoordinator springBootCoordinator;
		if (remoteCoordinator == null) {
			springBootCoordinator = new SpringBootCoordinator();
			springBootCoordinator.setIdentifier(instanceId);
			remoteCoordinator = (RemoteCoordinator) Proxy.newProxyInstance(SpringBootCoordinator.class.getClassLoader(),
					new Class[] { RemoteCoordinator.class }, springBootCoordinator);
			coordinatorRegistry.putRemoteCoordinatorByAddr(remoteAddr, remoteCoordinator);
			coordinatorRegistry.putRemoteCoordinator(instanceId, remoteCoordinator);
		} else {
			springBootCoordinator = (SpringBootCoordinator) Proxy.getInvocationHandler(remoteCoordinator);
			springBootCoordinator.setIdentifier(instanceId);
			coordinatorRegistry.putRemoteCoordinator(instanceId, remoteCoordinator);
		}

		byte[] byteArray = ByteUtils.stringToByteArray(StringUtils.trimToNull(respTransactionStr));
		TransactionContext serverContext = (TransactionContext) CommonUtils.deserializeObject(byteArray);
		TransactionResponseImpl txResp = new TransactionResponseImpl();
		txResp.setTransactionContext(serverContext);
		RemoteCoordinator serverCoordinator = beanRegistry.getConsumeCoordinator(sourceKey);
		txResp.setSourceTransactionCoordinator(serverCoordinator);
		txResp.setParticipantDelistFlag(!serverFlag);
		transactionInterceptor.afterReceiveResponse(txResp);
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setEndpoint(String identifier) {
		this.identifier = identifier;
	}

}
