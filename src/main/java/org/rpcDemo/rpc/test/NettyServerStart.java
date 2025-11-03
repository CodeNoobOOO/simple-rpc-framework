package org.rpcDemo.rpc.test;

import org.rpcDemo.rpc.server.NettyRpcServer;
import org.rpcDemo.rpc.service.UserService;
import org.rpcDemo.rpc.service.UserServiceImpl;

public class NettyServerStart {
    public static void main(String[] args){
        // 创建Netty RPC服务端，监听8080端口
        NettyRpcServer nettyRpcServer=new NettyRpcServer(8080);

        // 注册服务
        UserService userService=new UserServiceImpl();
        nettyRpcServer.registerService(UserService.class.getName(), userService);

        nettyRpcServer.start();
    }
}
