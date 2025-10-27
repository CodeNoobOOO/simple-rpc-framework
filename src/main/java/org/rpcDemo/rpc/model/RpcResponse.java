package org.rpcDemo.rpc.model;

import java.io.Serializable;

public class RpcResponse implements Serializable {
    private static final long serialVersionUID=1L;

    private String requestId;
    private Object result;
    private Throwable error;

    public RpcResponse() {}

    public static RpcResponse success(Object result, String requestId){
        RpcResponse response=new RpcResponse();
        response.setRequestId(requestId);
        response.setResult(result);
        return response;
    }

    public static RpcResponse error(Throwable error, String requestId){
        RpcResponse response=new RpcResponse();
        response.setRequestId(requestId);
        response.setError(error);
        return response;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", result=" + result +
                ", error=" + error +
                '}';
    }
}
