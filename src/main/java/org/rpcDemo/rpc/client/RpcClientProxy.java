package org.rpcDemo.rpc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper=new ObjectMapper();

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

        // 打印详细的响应信息
        System.out.println("🔍 响应详细信息:");
        System.out.println("  - RequestId: " + response.getRequestId());
        System.out.println("  - Result: " + response.getResult());
        System.out.println("  - ResultType: " + response.getResultType());
        System.out.println("  - Error: " + response.getError());
        if (response.getResult() != null) {
            System.out.println("  - Result的实际类型: " + response.getResult().getClass().getName());
        }

        // 处理响应
        if(response.isError()){
            throw new RuntimeException("RPC调用失败: "+response.getError().getMessage(),response.getError());
        }

        // 调试：直接返回结果，看看是什么
        System.out.println("🔍 直接返回结果，类型: " +
                (response.getResult() != null ? response.getResult().getClass().getName() : "null"));

        return convertResult(response,method.getReturnType());
    }

    /**
     * 使用resultType进行智能类型转换
     */
    private Object convertResult(RpcResponse response, Class<?> returnType){
        Object result=response.getResult();
        String resultType= response.getResultType();

        if(result==null){
            return null;
        }

        System.out.println("🔄 开始类型转换:");
        System.out.println("  - 原始类型: " + result.getClass().getName());
        System.out.println("  - 服务端类型: " + resultType);
        System.out.println("  - 方法返回类型: " + returnType.getName());

        try{
            // 情况1：如果已经是目标类型，直接返回
            if(returnType.isInstance(result)){
                System.out.println("结果已经是目标类型，直接返回");
                return result;
            }

            // 情况2：如果是LinkedHashMap，使用resultType转换
            if(result instanceof java.util.LinkedHashMap){
                System.out.println("检测到LinkedHashMap，使用resultType转换");
                if(resultType!=null&&!resultType.isEmpty()){
                    try{
                        Class<?> targetClass=Class.forName(resultType);
                        Object converted=objectMapper.convertValue(result, targetClass);
                        System.out.println("LinkedHashMap转换成功: " + resultType);
                        return converted;
                    }catch (Exception e){
                        System.err.println("LinkedHashMap转换失败: " + e.getMessage());
                        throw e;
                    }
                }else{
                    throw new RuntimeException("resultType为空，无法转换LinkedHashMap");
                }
            }

            // 情况3：其他类型，尝试直接转换
            System.out.println("尝试直接类型转换");
            return objectMapper.convertValue(result, returnType);
        } catch (Exception e) {
            System.err.println("类型转换失败: " + e.getMessage());
            throw new RuntimeException("RPC结果类型转换失败",e);
        }
    }
}
