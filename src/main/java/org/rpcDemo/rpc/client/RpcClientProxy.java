package org.rpcDemo.rpc.client;

import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RPC客户端代理 - 使用JDK动态代理实现透明调用
 */
public class RpcClientProxy implements InvocationHandler {
    private final RpcClient rpcClient;
    private final String serviceName;

    public RpcClientProxy(RpcClient rpcClient, Class<?> serviceInterface) {
        this.rpcClient = rpcClient;
        this.serviceName=serviceInterface.getName();
    }

    /**
     * 创建代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(RpcClient rpcClient, Class<T> serviceInterface){
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                new RpcClientProxy(rpcClient,serviceInterface)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 跳过Object类的方法（toString, equals, hashCode等）
        if(method.getDeclaringClass()== Object.class){
            return method.invoke(this,args);
        }

        System.out.println("客户端代理: 拦截方法调用 "+method.getName());

        // 构建RPC请求
        RpcRequest request=new RpcRequest(
                serviceName,
                method.getName(),
                method.getParameterTypes(),
                args
        );

        // 发送RPC请求
        RpcResponse response=rpcClient.sendRequest(request);

        // 处理响应
        if(response.isError()){
            throw new RuntimeException("RPC调用失败: "+response.getError().getMessage(),response.getError());
        }

        return response.getResult();
    }
}
