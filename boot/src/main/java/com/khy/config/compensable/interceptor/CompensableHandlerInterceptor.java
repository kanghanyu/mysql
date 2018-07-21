package com.khy.config.compensable.interceptor;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.bytesoft.bytejta.supports.rpc.TransactionRequestImpl;
import org.bytesoft.bytejta.supports.rpc.TransactionResponseImpl;
import org.bytesoft.common.utils.ByteUtils;
import org.bytesoft.common.utils.CommonUtils;
import org.bytesoft.compensable.Compensable;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.CompensableManager;
import org.bytesoft.compensable.CompensableTransaction;
import org.bytesoft.compensable.TransactionContext;
import org.bytesoft.compensable.aware.CompensableEndpointAware;
import org.bytesoft.transaction.supports.rpc.TransactionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.khy.config.compensable.SpringBootBeanRegistry;
import com.khy.config.compensable.controller.CompensableCoordinatorController;

public class CompensableHandlerInterceptor implements HandlerInterceptor, CompensableEndpointAware, ApplicationContextAware {
    static final String HEADER_TRANCACTION_KEY = "X-COMPENSABLE-XID";
    static final String HEADER_PROPAGATION_KEY = "X-PROPAGATION-KEY";
    private String identifier;
    private ApplicationContext applicationContext;

    public CompensableHandlerInterceptor() {
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!HandlerMethod.class.isInstance(handler)) {
            return true;
        } else {
            HandlerMethod hm = (HandlerMethod)handler;
            Class<?> clazz = hm.getBeanType();
            if (CompensableCoordinatorController.class.equals(clazz)) {
                return true;
            } else if (ErrorController.class.isInstance(hm.getBean())) {
                return true;
            } else {
                String transactionStr = request.getHeader("X-COMPENSABLE-XID");
                if (StringUtils.isBlank(transactionStr)) {
                    return true;
                } else {
                    String propagationStr = request.getHeader("X-PROPAGATION-KEY");
                    String transactionText = StringUtils.trimToNull(transactionStr);
                    String propagationText = StringUtils.trimToNull(propagationStr);
                    Compensable annotation = (Compensable)clazz.getAnnotation(Compensable.class);
                    if (annotation == null) {
                        return true;
                    } else {
                        SpringBootBeanRegistry beanRegistry = SpringBootBeanRegistry.getInstance();
                        CompensableBeanFactory beanFactory = beanRegistry.getBeanFactory();
                        TransactionInterceptor transactionInterceptor = beanFactory.getTransactionInterceptor();
                        byte[] byteArray = transactionText == null ? new byte[0] : ByteUtils.stringToByteArray(transactionText);
                        TransactionContext transactionContext = null;
                        if (byteArray != null && byteArray.length > 0) {
                            transactionContext = (TransactionContext)CommonUtils.deserializeObject(byteArray);
                            transactionContext.setPropagated(true);
                            transactionContext.setPropagatedBy(propagationText);
                        }

                        TransactionRequestImpl req = new TransactionRequestImpl();
                        req.setTransactionContext(transactionContext);
                        req.setTargetTransactionCoordinator(beanRegistry.getConsumeCoordinator(propagationText));
                        transactionInterceptor.afterReceiveRequest(req);
                        CompensableManager compensableManager = beanFactory.getCompensableManager();
                        CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
                        byte[] responseByteArray = CommonUtils.serializeObject(compensable.getTransactionContext());
                        String compensableStr = ByteUtils.byteArrayToString(responseByteArray);
                        response.addHeader("X-COMPENSABLE-XID", compensableStr);
                        response.addHeader("X-PROPAGATION-KEY", this.identifier);
                        return true;
                    }
                }
            }
        }
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (HandlerMethod.class.isInstance(handler)) {
            HandlerMethod hm = (HandlerMethod)handler;
            Class<?> clazz = hm.getBeanType();
            if (!CompensableCoordinatorController.class.equals(clazz)) {
                if (!ErrorController.class.isInstance(hm.getBean())) {
                    String transactionStr = request.getHeader("X-COMPENSABLE-XID");
                    if (!StringUtils.isBlank(transactionStr)) {
                        Compensable annotation = (Compensable)clazz.getAnnotation(Compensable.class);
                        if (annotation != null) {
                            SpringBootBeanRegistry beanRegistry = SpringBootBeanRegistry.getInstance();
                            CompensableBeanFactory beanFactory = beanRegistry.getBeanFactory();
                            CompensableManager compensableManager = beanFactory.getCompensableManager();
                            TransactionInterceptor transactionInterceptor = beanFactory.getTransactionInterceptor();
                            CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
                            TransactionContext transactionContext = compensable.getTransactionContext();
                            byte[] byteArray = CommonUtils.serializeObject(transactionContext);
                            String compensableStr = ByteUtils.byteArrayToString(byteArray);
                            response.addHeader("X-COMPENSABLE-XID", compensableStr);
                            response.addHeader("X-PROPAGATION-KEY", this.identifier);
                            TransactionResponseImpl resp = new TransactionResponseImpl();
                            resp.setTransactionContext(transactionContext);
                            resp.setSourceTransactionCoordinator(beanRegistry.getConsumeCoordinator((String)null));
                            transactionInterceptor.beforeSendResponse(resp);
                        }
                    }
                }
            }
        }
    }

    public void setEndpoint(String identifier) {
        this.identifier = identifier;
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
