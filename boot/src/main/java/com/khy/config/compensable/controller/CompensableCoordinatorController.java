package com.khy.config.compensable.controller;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import org.apache.commons.lang3.StringUtils;
import org.bytesoft.bytetcc.CompensableCoordinator;
import org.bytesoft.common.utils.ByteUtils;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;
import org.bytesoft.transaction.xa.XidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public class CompensableCoordinatorController extends HttpServlet implements CompensableBeanFactoryAware {
	private static final long serialVersionUID = 1L;
	static final Logger logger = LoggerFactory.getLogger(CompensableCoordinatorController.class);
	static final String HEADER_PROPAGATION_KEY = "org-bytesoft-bytetcc-propagation";
	@Autowired
	private CompensableCoordinator compensableCoordinator;
	@Autowired
	private CompensableBeanFactory beanFactory;

	public CompensableCoordinatorController() {
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String requestPath = request.getRequestURI();
		String servletPath = request.getServletPath();
		if (!requestPath.startsWith(servletPath)) {
			logger.error("非法请求(uri= {})!", requestPath);
			response.addHeader("failure", "true");
			response.addHeader("XA_XAER", String.valueOf(-6));
			response.setStatus(500);
		} else {
			String resource = requestPath.substring(servletPath.length());
			if (!resource.startsWith("/")) {
				logger.error("非法请求(uri= {})!", requestPath);
				response.addHeader("failure", "true");
				response.addHeader("XA_XAER", String.valueOf(-6));
				response.setStatus(500);
			} else {
				String[] values = resource.substring(1).split("\\/");
				if (values.length < 2) {
					logger.error("非法请求(uri= {})!", requestPath);
					response.addHeader("failure", "true");
					response.addHeader("XA_XAER", String.valueOf(-6));
					response.setStatus(500);
				} else {
					String methodName = values[0];
					Object[] args = null;
					Method method = null;

					try {
						if (StringUtils.equalsIgnoreCase("prepare", methodName)) {
							method = CompensableCoordinatorController.class.getDeclaredMethod(methodName, String.class,
									HttpServletResponse.class);
							args = new Object[] { values[1], response };
						} else if (StringUtils.equalsIgnoreCase("commit", methodName)) {
							method = CompensableCoordinatorController.class.getDeclaredMethod(methodName, String.class,
									Boolean.TYPE, HttpServletResponse.class);
							args = new Object[] { values[1], Boolean.parseBoolean(values[2]), response };
						} else if (StringUtils.equalsIgnoreCase("rollback", methodName)) {
							method = CompensableCoordinatorController.class.getDeclaredMethod(methodName, String.class,
									HttpServletResponse.class);
							args = new Object[] { values[1], response };
						} else if (StringUtils.equalsIgnoreCase("forget", methodName)) {
							method = CompensableCoordinatorController.class.getDeclaredMethod(methodName, String.class,
									HttpServletResponse.class);
							args = new Object[] { values[1], response };
						} else {
							if (!StringUtils.equalsIgnoreCase("recover", methodName)) {
								response.addHeader("failure", "true");
								response.setStatus(404);
								return;
							}

							method = CompensableCoordinatorController.class.getDeclaredMethod(methodName, Integer.TYPE,
									HttpServletResponse.class);
							args = new Object[] { Integer.parseInt(values[1]), response };
						}
					} catch (NoSuchMethodException var15) {
						logger.error("非法请求(uri= {})!", requestPath);
						response.addHeader("failure", "true");
						response.addHeader("XA_XAER", String.valueOf(-6));
						response.setStatus(500);
						return;
					} catch (SecurityException var16) {
						logger.error("非法请求(uri= {})!", requestPath);
						response.addHeader("failure", "true");
						response.addHeader("XA_XAER", String.valueOf(-6));
						response.setStatus(500);
						return;
					}

					try {
						Object value = method.invoke(this, args);
						if (value != null) {
							response.getOutputStream().write(JSONObject.toJSONString(value).getBytes());
						}

					} catch (IllegalAccessException var11) {
						logger.error("请求出错(uri= {})!", requestPath, var11);
						response.addHeader("failure", "true");
						response.setStatus(500);
					} catch (IllegalArgumentException var12) {
						logger.error("请求出错(uri= {})!", requestPath, var12);
						response.addHeader("failure", "true");
						response.setStatus(500);
					} catch (InvocationTargetException var13) {
						logger.error("请求出错(uri= {})!", requestPath, var13);
						response.addHeader("failure", "true");
						response.setStatus(500);
					} catch (RuntimeException var14) {
						logger.error("请求出错(uri= {})!", requestPath, var14);
						response.addHeader("failure", "true");
						response.setStatus(500);
					}
				}
			}
		}
	}

	@RequestMapping(value = { "/org/bytesoft/bytetcc/prepare/{xid}" }, method = { RequestMethod.POST })
	@ResponseBody
	public int prepare(@PathVariable("xid") String identifier, HttpServletResponse response) {
		try {
			XidFactory xidFactory = this.beanFactory.getCompensableXidFactory();
			byte[] byteArray = ByteUtils.stringToByteArray(identifier);
			Xid xid = xidFactory.createGlobalXid(byteArray);
			return this.compensableCoordinator.prepare(xid);
		} catch (XAException var6) {
			logger.error("Error occurred while preparing transaction: {}.", identifier, var6);
			response.addHeader("failure", "true");
			response.addHeader("XA_XAER", String.valueOf(var6.errorCode));
			response.setStatus(500);
			return -1;
		} catch (RuntimeException var7) {
			logger.error("Error occurred while preparing transaction: {}.", identifier, var7);
			response.addHeader("failure", "true");
			response.setStatus(500);
			return -1;
		}
	}

	@RequestMapping(value = { "/org/bytesoft/bytetcc/commit/{xid}/{opc}" }, method = { RequestMethod.POST })
	@ResponseBody
	public void commit(@PathVariable("xid") String identifier, @PathVariable("opc") boolean onePhase,
			HttpServletResponse response) {
		try {
			XidFactory xidFactory = this.beanFactory.getCompensableXidFactory();
			byte[] byteArray = ByteUtils.stringToByteArray(identifier);
			Xid xid = xidFactory.createGlobalXid(byteArray);
			this.compensableCoordinator.commit(xid, onePhase);
		} catch (XAException var7) {
			logger.error("Error occurred while committing transaction: {}.", identifier, var7);
			response.addHeader("failure", "true");
			response.addHeader("XA_XAER", String.valueOf(var7.errorCode));
			response.setStatus(500);
		} catch (RuntimeException var8) {
			logger.error("Error occurred while committing transaction: {}.", identifier, var8);
			response.addHeader("failure", "true");
			response.setStatus(500);
		}

	}

	@RequestMapping(value = { "/org/bytesoft/bytetcc/rollback/{xid}" }, method = { RequestMethod.POST })
	@ResponseBody
	public void rollback(@PathVariable("xid") String identifier, HttpServletResponse response) {
		try {
			XidFactory xidFactory = this.beanFactory.getCompensableXidFactory();
			byte[] byteArray = ByteUtils.stringToByteArray(identifier);
			Xid xid = xidFactory.createGlobalXid(byteArray);
			this.compensableCoordinator.rollback(xid);
		} catch (XAException var6) {
			logger.error("Error occurred while rolling back transaction: {}.", identifier, var6);
			response.addHeader("failure", "true");
			response.addHeader("XA_XAER", String.valueOf(var6.errorCode));
			response.setStatus(500);
		} catch (RuntimeException var7) {
			logger.error("Error occurred while rolling back transaction: {}.", identifier, var7);
			response.addHeader("failure", "true");
			response.setStatus(500);
		}

	}

	@RequestMapping(value = { "/org/bytesoft/bytetcc/recover/{flag}" }, method = { RequestMethod.GET })
	@ResponseBody
	public Xid[] recover(@PathVariable("flag") int flag, HttpServletResponse response) {
		try {
			return this.compensableCoordinator.recover(flag);
		} catch (XAException var4) {
			logger.error("Error occurred while recovering transactions.", var4);
			response.addHeader("failure", "true");
			response.addHeader("XA_XAER", String.valueOf(var4.errorCode));
			response.setStatus(500);
			return new Xid[0];
		} catch (RuntimeException var5) {
			logger.error("Error occurred while recovering transactions.", var5);
			response.addHeader("failure", "true");
			response.setStatus(500);
			return new Xid[0];
		}
	}

	@RequestMapping(value = { "/org/bytesoft/bytetcc/forget/{xid}" }, method = { RequestMethod.POST })
	@ResponseBody
	public void forget(@PathVariable("xid") String identifier, HttpServletResponse response) {
		try {
			XidFactory xidFactory = this.beanFactory.getCompensableXidFactory();
			byte[] byteArray = ByteUtils.stringToByteArray(identifier);
			Xid xid = xidFactory.createGlobalXid(byteArray);
			this.compensableCoordinator.forget(xid);
		} catch (XAException var6) {
			logger.error("Error occurred while forgetting transaction: {}.", identifier, var6);
			response.addHeader("failure", "true");
			response.addHeader("XA_XAER", String.valueOf(var6.errorCode));
			response.setStatus(500);
		} catch (RuntimeException var7) {
			logger.error("Error occurred while forgetting transaction: {}.", identifier, var7);
			response.addHeader("failure", "true");
			response.setStatus(500);
		}

	}

	public void setBeanFactory(CompensableBeanFactory tbf) {
		this.beanFactory = tbf;
	}

	public void setCompensableCoordinator(CompensableCoordinator compensableCoordinator) {
		this.compensableCoordinator = compensableCoordinator;
	}
}
