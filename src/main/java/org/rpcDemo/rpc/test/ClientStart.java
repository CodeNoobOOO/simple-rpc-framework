package org.rpcDemo.rpc.test;

import org.rpcDemo.rpc.client.RpcClient;
import org.rpcDemo.rpc.client.RpcClientProxy;
import org.rpcDemo.rpc.client.SimpleServiceDiscoverer;
import org.rpcDemo.rpc.model.User;
import org.rpcDemo.rpc.service.UserService;

public class ClientStart {
    public static void main(String[] args){
        System.out.println("=== RPC客户端启动 ===");

        // 创建服务发现（连接到本地8080端口）
        SimpleServiceDiscoverer discoverer=new SimpleServiceDiscoverer("localhost",8080);

        // 创建RPC客户端
        RpcClient rpcClient=new RpcClient(discoverer);

        // 创建动态代理
        UserService userService= RpcClientProxy.createProxy(rpcClient,UserService.class);

        // 测试远程调用
        testRemoteCalls(userService);
    }

    private static void testRemoteCalls(UserService userService){
        try{
            System.out.println("\\n--- 测试1: 查询存在的用户 ---");
            User user1=userService.getUserById(1);
            System.out.println("查询结果: "+user1);

            System.out.println("\\n--- 测试2: 查询不存在的用户 ---");
            User user2=userService.getUserById(999);
            System.out.println("查询结果: "+user2);

            System.out.println("\\n--- 测试3: 获取用户数量 ---");
            Integer count=userService.getUserCount();
            System.out.println("查询结果: "+count);

            System.out.println("\\n--- 测试4: 添加新用户 ---");
            User newUser=new User(null,"远程用户",28);
            Boolean success=userService.addUser(newUser);
            System.out.println("添加结果: "+success);
            System.out.println("新用户ID: "+newUser.getId());

            System.out.println("\\n--- 测试5: 再次获取用户数量 ---");
            count=userService.getUserCount();
            System.out.println("查询结果: "+count);

            System.out.println("\n=== 所有测试完成 ===");
        }catch (Exception e){
            System.out.println("测试失败: "+e.getMessage());
            e.printStackTrace();
        }
    }
}
