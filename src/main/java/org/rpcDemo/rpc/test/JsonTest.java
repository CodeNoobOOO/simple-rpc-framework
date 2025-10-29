package org.rpcDemo.rpc.test;

import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;
import org.rpcDemo.rpc.model.User;
import org.rpcDemo.rpc.serialize.JsonSerializer;

public class JsonTest {
    public static void main(String[] args){
        System.out.println("=== 测试JSON序列化 ===");

        // 测试1: 序列化RpcRequest
        RpcRequest request=new RpcRequest(
                "org.rpcDemo.rpc.service.UserService",
                "getUserById",
                new Class<?>[]{Integer.class},
                new Object[]{1}
        );

        System.out.println("原始请求: "+request);
        System.out.println("JSON格式:\\n"+ JsonSerializer.toJsonString(request));

        // 测试序列化/反序列化
        byte[] bytes=JsonSerializer.serialize(request);
        RpcRequest deserializedRequest=JsonSerializer.deserialize(bytes,RpcRequest.class);
        System.out.println("反序列化后: "+deserializedRequest);

        System.out.println("---");

        // 测试2: 序列化RpcResponse
        User user=new User(1,"测试用户",25);
        RpcResponse response=RpcResponse.success(user,"req-123");

        System.out.println("原始响应: "+response);
        System.out.println("JSON格式:\\n"+JsonSerializer.toJsonString(response));

        byte[] responseBytes=JsonSerializer.serialize(response);
        RpcResponse deserializedResponse = JsonSerializer.deserialize(responseBytes, RpcResponse.class);
        System.out.println("反序列化后: " + deserializedResponse);

        System.out.println("=== JSON序列化测试完成 ===");
    }
}
