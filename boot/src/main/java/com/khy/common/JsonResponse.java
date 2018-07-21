package com.khy.common;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class JsonResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    static final String SUCCESS = "0000000";
    private String retCode;
    private String retDesc;
    private String timestamp;
    private T rspBody;

    public JsonResponse() {
        this((T)null);
    }

    public JsonResponse(T value) {
        this("0000000", "操作成功!", value);
    }

    public JsonResponse(String retCode, String retDesc, T rspBody) {
        this(retCode, retDesc, rspBody, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).format(new Date()));
    }

    public JsonResponse(String retCode, String retDesc) {
        this(retCode, retDesc, (T)null);
    }

    public JsonResponse(String retCode, String retDesc, T rspBody, String timestamp) {
        this.retCode = retCode;
        this.retDesc = retDesc;
        this.rspBody = rspBody;
        this.timestamp = timestamp;
    }

    public void setTimestamp(Date date) {
        this.timestamp = date == null ? this.timestamp : (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).format(date);
    }

    public String getRetCode() {
        return this.retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public String getRetDesc() {
        return this.retDesc;
    }

    public void setRetDesc(String retDesc) {
        this.retDesc = retDesc;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public T getRspBody() {
        return this.rspBody;
    }

    public void setRspBody(T rspBody) {
        this.rspBody = rspBody;
    }
}