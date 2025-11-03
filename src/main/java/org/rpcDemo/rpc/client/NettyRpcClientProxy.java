package org.rpcDemo.rpc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NettyRpcClientProxy implements InvocationHandler {
    private final NettyRpcClient nettyRpcClient;
    private final NettyServiceDiscoverer nettyServiceDiscoverer;
    private final String serviceName;
    private final ObjectMapper objectMapper=new ObjectMapper();

    public NettyRpcClientProxy(NettyRpcClient nettyRpcClient,
                               NettyServiceDiscoverer nettyServiceDiscoverer,
                               Class<?> serviceInterface) {
        this.nettyRpcClient = nettyRpcClient;
        this.nettyServiceDiscoverer = nettyServiceDiscoverer;
        this.serviceName=serviceInterface.getName();
    }

    /**
     * 创建代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(NettyRpcClient nettyRpcClient,
                                    NettyServiceDiscoverer nettyServiceDiscoverer,
                                    Class<T> serviceInterface){
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                new NettyRpcClientProxy(nettyRpcClient,nettyServiceDiscoverer,serviceInterface));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 跳过Object类的方法
        if(method.getDeclaringClass()==Object.class){
            return method.invoke(this,args);
        }

        System.out.println("Netty客户端代理: 拦截方法调用 "+method.getName());

        // 构建RPC请求
        RpcRequest request=new RpcRequest(
                serviceName,
                method.getName(),
                method.getParameterTypes(),
                args
        );

        // 生成请求ID
        String requestId="netty-" + System.currentTimeMillis() + "-" + System.nanoTime();
        request.setRequestId(requestId);

        // 服务发现
        InetSocketAddress address=nettyServiceDiscoverer.discover(serviceName);

        // 使用Netty发送请求（异步转同步）
        CompletableFuture<RpcResponse> future=nettyRpcClient.sendRequest(request,address);

        // 同步等待结果（设置超时时间）
        RpcResponse response=future.get(30, TimeUnit.SECONDS);

        // 处理响应
        if(response.isError()){
            throw new RuntimeException("RPC调用失败: "+ response.getError());
        }

        // 类型转换
        return convertResult(response,method.getReturnType());
    }

    /**
     * 类型转换
     */
    private Object convertResult(RpcResponse response,Class<?> returnType){
        Object result=response.getResult();
        String resultType= response.getResultType();

        if(result==null){
            return null;
        }

        // 如果已经是目标类型，直接返回
        if(returnType.isInstance(result)){
            return result;
        }

        // 如果是LinkedHashMap，使用resultType转换
        if(result instanceof java.util.LinkedHashMap&&resultType!=null&&!resultType.isEmpty()){
            try{
                Class<?> targetClass=Class.forName(resultType);
                return objectMapper.convertValue(result,targetClass);
            } catch (Exception e) {
                System.err.println("Netty类型转换失败: "+e.getMessage());
                throw new RuntimeException("RPC结果类型转换失败", e);
            }
        }

        try{
            return objectMapper.convertValue(result,returnType);
        }catch(Exception e){
            System.err.println("Netty直接类型转换失败: "+e.getMessage());
            throw new RuntimeException("RPC结果类型转换失败",e);
        }
    }
}
