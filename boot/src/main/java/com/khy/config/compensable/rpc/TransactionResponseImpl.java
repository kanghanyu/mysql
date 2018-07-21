package com.khy.config.compensable.rpc;


public class TransactionResponseImpl extends org.bytesoft.bytejta.supports.rpc.TransactionResponseImpl {
    private boolean intercepted;

    public TransactionResponseImpl() {
    }

    public boolean isIntercepted() {
        return this.intercepted;
    }

    public void setIntercepted(boolean intercepted) {
        this.intercepted = intercepted;
    }
}

