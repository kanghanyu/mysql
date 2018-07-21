package com.khy.config.compensable;

import javax.inject.Inject;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.CompensableManager;
import org.bytesoft.compensable.CompensableTransaction;
import org.bytesoft.compensable.TransactionContext;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;
import org.bytesoft.transaction.supports.rpc.TransactionInterceptor;
import org.bytesoft.transaction.supports.rpc.TransactionRequest;
import org.bytesoft.transaction.supports.rpc.TransactionResponse;

import com.khy.config.compensable.rpc.TransactionResponseImpl;

public class CompensableInterceptorImpl implements TransactionInterceptor, CompensableBeanFactoryAware {
	@Inject
	private CompensableBeanFactory beanFactory;
	private TransactionInterceptor compensableInterceptor;

	public CompensableInterceptorImpl() {
	}

	public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
		CompensableManager compensableManager = this.beanFactory.getCompensableManager();
		CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
		if (compensable != null && compensable.getTransactionContext().isCompensable()) {
			this.compensableInterceptor.beforeSendRequest(request);
		}

	}

	public void afterReceiveRequest(TransactionRequest request) throws IllegalStateException {
		TransactionContext transactionContext = (TransactionContext) request.getTransactionContext();
		if (transactionContext != null && transactionContext.isCompensable()) {
			this.compensableInterceptor.afterReceiveRequest(request);
		}

	}

	public void beforeSendResponse(TransactionResponse response) throws IllegalStateException {
		CompensableManager compensableManager = this.beanFactory.getCompensableManager();
		CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
		if (compensable != null && compensable.getTransactionContext().isCompensable()) {
			this.compensableInterceptor.beforeSendResponse(response);
		}

	}

	public void afterReceiveResponse(TransactionResponse response) throws IllegalStateException {
		CompensableManager compensableManager = this.beanFactory.getCompensableManager();
		CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
		if (compensable != null && compensable.getTransactionContext().isCompensable()) {
			if (TransactionResponseImpl.class.isInstance(response)) {
				((TransactionResponseImpl) response).setIntercepted(true);
			}

			this.compensableInterceptor.afterReceiveResponse(response);
		}

	}

	public void setBeanFactory(CompensableBeanFactory tbf) {
		this.beanFactory = tbf;
	}

	public TransactionInterceptor getCompensableInterceptor() {
		return this.compensableInterceptor;
	}

	public void setCompensableInterceptor(TransactionInterceptor compensableInterceptor) {
		this.compensableInterceptor = compensableInterceptor;
	}
}
