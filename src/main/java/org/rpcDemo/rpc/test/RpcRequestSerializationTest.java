package org.rpcDemo.rpc.test;

import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.serialize.JsonSerializer;

public class RpcRequestSerializationTest {
    public static void main(String[] args){
        System.out.println("=== 测试RpcRequest序列化 ===");

        // 创建请求
        RpcRequest request = new RpcRequest();
        request.setRequestId("netty-123456789-987654321");
        request.setInterfaceName("TestService");
        request.setMethodName("testMethod");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameters(new Object[]{"test"});

        System.out.println("来自RpcRequestSerializationTest：原始请求: " + request);
        System.out.println("来自RpcRequestSerializationTest：requestId: " + request.getRequestId());

        // 序列化
        byte[] bytes = JsonSerializer.serialize(request);
        String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("来自RpcRequestSerializationTest：序列化后的JSON: " + json);

        // 反序列化
        RpcRequest deserialized = JsonSerializer.deserialize(bytes, RpcRequest.class);
        System.out.println("来自RpcRequestSerializationTest：反序列化后的请求: " + deserialized);
        System.out.println("来自RpcRequestSerializationTest：反序列化后的requestId: " + deserialized.getRequestId());

        // 检查是否相等
        System.out.println("来自RpcRequestSerializationTest：requestId是否相等: " +
                request.getRequestId().equals(deserialized.getRequestId()));

        System.out.println("=== 测试完成 ===");
    }
}
