package org.rpcDemo.rpc.test;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.rpcDemo.rpc.client.NettyRpcClient;
import org.rpcDemo.rpc.client.NettyRpcClientProxy;
import org.rpcDemo.rpc.client.NettyServiceDiscoverer;
import org.rpcDemo.rpc.model.User;
import org.rpcDemo.rpc.service.UserService;

public class NettyClientStart {
    public static void main(String[] args){
        System.out.println("=== Netty RPC客户端启动 ===");

        // 创建Netty服务发现和客户端
        NettyServiceDiscoverer discoverer=new NettyServiceDiscoverer("localhost",8080);
        NettyRpcClient nettyRpcClient=new NettyRpcClient();

        // 使用Netty专用的代理
        UserService userService= NettyRpcClientProxy.createProxy(nettyRpcClient,discoverer, UserService.class);

        // 测试远程调用
        testRemoteCalls(userService);

        // 关闭客户端
        nettyRpcClient.close();
    }

    private static void testRemoteCalls(UserService userService){
        try{
            System.out.println("\\n--- 测试1: 查询存在的用户 ---");

            User user1=userService.getUserById(1);
            System.out.println("查询结果: " + user1);

            System.out.println("\\n--- 测试2: 获取用户数量 ---");
            Integer count=userService.getUserCount();
            System.out.println("用户数量: " + count);

            System.out.println("\n--- 测试3: 添加新用户 ---");
            User newUser=new User(null,"Netty用户",30);
            Boolean success=userService.addUser(newUser);
            System.out.println("添加结果: "+success);
            System.out.println("新用户ID: " + newUser.getId());

            System.out.println("\\n--- 测试4: 再次获取用户数量 ---");
            count=userService.getUserCount();
            System.out.println("更新后用户数量: " + count);

            System.out.println("\n=== Netty版本测试完成 ===");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
