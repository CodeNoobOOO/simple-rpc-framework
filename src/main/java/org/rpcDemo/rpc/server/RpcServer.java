package org.rpcDemo.rpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RpcServer {
    private final int port;
    private final ExecutorService threadPool;;
    private final Map<String,Object> serviceRegistry=new HashMap<>();
    private final ObjectMapper objectMapper=new ObjectMapper();
    private volatile boolean running=false;

    public RpcServer(int port) {
        this.port = port;
        this.threadPool= Executors.newFixedThreadPool(10);
    }

    /**
     * 注册服务
     */
    public void registerService(String interfaceName, Object serviceImpl){
        serviceRegistry.put(interfaceName,serviceImpl);
        System.out.println("注册服务: " + interfaceName + " -> " + serviceImpl.getClass().getSimpleName());
    }

    /**
     * 启动服务端
     */
    public void start(){
        if(running){
            System.out.println("服务端已经在运行中...");
            return;
        }
        running=true;
        try(ServerSocket serverSocket=new ServerSocket(port)){
            System.out.println("=== RPC服务端启动 ===");
            System.out.println("监听端口: " + port);
            System.out.println("已注册服务: " + serviceRegistry.keySet());
            System.out.println("等待客户端连接...");

            while(running){
                try{
                    // 接受客户端连接
                    Socket clientSocket=serverSocket.accept();
                    System.out.println("接收到客户端连接: "+clientSocket.getInetAddress());
                    // 使用线程池处理请求
                    threadPool.execute(new ClientHandler(clientSocket));//
                } catch (IOException e) {
                    if(running){
                        System.out.println("处理客户端连接时出错: "+e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("启动服务端失败: "+e.getMessage());
        }finally {
            stop();
        }
    }

    /**
     * 停止服务端
     */
    public void stop(){
        running=false;
        threadPool.shutdown();
        System.out.println("RPC服务端已停止");
    }

    /**
     * 客户端请求处理器
     */
    private class ClientHandler implements Runnable{
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try(ObjectInputStream input=new ObjectInputStream(clientSocket.getInputStream()); ObjectOutputStream output=new ObjectOutputStream(clientSocket.getOutputStream())) {
                // 读取客户端请求
                RpcRequest request=(RpcRequest) input.readObject();
                System.out.println("接收到请求: "+request);

                // 处理请求
                RpcResponse response=handleRequest(request);
                // 发送响应
                output.writeObject(response);
                output.flush();
                System.out.println("发送响应: "+response);
            } catch (Exception e) {
                System.out.println("处理客户端请求时出错: "+e.getMessage());
            }finally {
                try {
                    clientSocket.close();
                }catch (IOException e){
                    System.out.println("关闭客户端连接时出错: "+e.getMessage());
                }
            }
        }

        /**
         * 处理RPC请求
         */
        private RpcResponse handleRequest(RpcRequest request){
            try{
                Object service=serviceRegistry.get(request.getInterfaceName());
                if(service==null){
                    throw new RuntimeException("服务未找到: "+request.getInterfaceName());
                }

                // 通过反射调用方法
                Method method=service.getClass().getMethod(request.getMethodName(),request.getParameterTypes());

                Object result=method.invoke(service,request.getParameters());

                // 返回成功响应
                return RpcResponse.success(result,generateRequestId());

            } catch (Exception e) {
                System.err.println("处理请求时发生错误: " + e.getMessage());
                return RpcResponse.error(e,generateRequestId());
            }
        }

        private String generateRequestId(){
            return "req-"+System.currentTimeMillis()+"-"+Thread.currentThread().getId();
        }
    }

}
