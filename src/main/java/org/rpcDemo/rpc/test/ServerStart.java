package org.rpcDemo.rpc.test;

import org.rpcDemo.rpc.server.RpcServer;
import org.rpcDemo.rpc.service.UserService;
import org.rpcDemo.rpc.service.UserServiceImpl;

public class ServerStart {
    public static void main(String[] args){
        // 创建RPC服务端，监听8080端口
        RpcServer rpcServer=new RpcServer(8080);

        // 注册服务
        UserService userService=new UserServiceImpl();
        rpcServer.registerService(UserService.class.getName(),userService);

        // 启动服务端
        rpcServer.start();
    }
}
