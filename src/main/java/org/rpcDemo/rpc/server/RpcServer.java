package org.rpcDemo.rpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;
import org.rpcDemo.rpc.serialize.JsonSerializer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RpcServer {
    private final int port;
    private final ExecutorService threadPool;
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
                    threadPool.execute(new JsonClientHandler(clientSocket));//
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
    private class JsonClientHandler implements Runnable{
        private final Socket clientSocket;

        public JsonClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try(
                    InputStream inputStream=clientSocket.getInputStream();
                    OutputStream outputStream=clientSocket.getOutputStream()
            ) {

                // 读取请求数据
                byte[] requestBytes=readRequest(inputStream);
                System.out.println("接收到请求数据，长度: "+requestBytes.length+"字节");

                // JSON反序列化
                RpcRequest request= JsonSerializer.deserialize(requestBytes, RpcRequest.class);
                System.out.println("反序列化后的请求: "+request);

                // 调试：打印JSON格式的请求
                System.out.println("请求JSON:\\n"+JsonSerializer.toJsonString(request));

                // 处理请求
                RpcResponse response=handleRequest(request);

                // 序列化响应
                byte[] responseBytes=JsonSerializer.serialize(response);
                System.out.println("发送响应数据，长度: "+responseBytes.length+" 字节");

                // 调试：打印JSON格式的请求
                System.out.println("响应JSON:\\n"+JsonSerializer.toJsonString(response));

                outputStream.write(responseBytes);
                outputStream.flush();

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

        private byte[] readRequest(InputStream inputStream)throws IOException{
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            byte[] data=new byte[1024];
            int bytesRead;

            while((bytesRead=inputStream.read(data,0, data.length))!=-1){
                buffer.write(data,0,bytesRead);
                // 简单判断：如果可读数据小于缓冲区，认为读取完成
                if(inputStream.available()==0){
                    break;
                }
            }
            return buffer.toByteArray();
        }

        /**
         * 处理RPC请求
         */
        private RpcResponse handleRequest(RpcRequest request){
            try{
                // 调试
                if (request.getParameters() != null) {
                    System.out.println("🔍 服务端调试 - 原始参数:");
                    for (int i = 0; i < request.getParameters().length; i++) {
                        Object param = request.getParameters()[i];
                        System.out.println("  参数[" + i + "]: " + param + " (类型: " +
                                (param != null ? param.getClass().getName() : "null") + ")");
                    }
                }

                // 获取服务实现
                Object service=serviceRegistry.get(request.getInterfaceName());
                if(service==null){
                    throw new RuntimeException("服务未找到: "+request.getInterfaceName());
                }

                // 关键修复：转换参数类型
                Object[] convertedParameters = convertParameters(request.getParameters(), request.getParameterTypes());

                // 调试
                if (convertedParameters != null){
                    System.out.println("🔍 服务端调试 - 转换后参数:");
                    for (int i = 0; i < convertedParameters.length; i++) {
                        Object param = convertedParameters[i];
                        System.out.println("  参数[" + i + "]: " + param + " (类型: " +
                                (param != null ? param.getClass().getName() : "null") + ")");
                    }
                }


                // 通过反射调用方法
                Method method=service.getClass().getMethod(request.getMethodName(),request.getParameterTypes());

                Object result=method.invoke(service,convertedParameters);

                // 返回成功响应
                RpcResponse response= RpcResponse.success(result,generateRequestId());//bug?

                // 设置resultType
                if (result != null) {
                    response.setResultType(result.getClass().getName());
                    System.out.println("服务端: 设置resultType = " + result.getClass().getName());
                }

                return response;

            } catch (Exception e) {
                System.err.println("处理请求时发生错误: " + e.getMessage());
                return RpcResponse.error(e,generateRequestId());//bug?
            }
        }

        private Object[] convertParameters(Object[] parameters, Class<?>[] parameterTypes) {
            if (parameters == null) {
                return null;
            }

            Object[] converted = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Object param = parameters[i];
                Class<?> targetType = parameterTypes[i];

                // 如果参数已经是目标类型，直接使用
                if (param == null || targetType.isInstance(param)) {
                    converted[i] = param;
                    continue;
                }

                // 如果是LinkedHashMap，需要转换
                if (param instanceof java.util.LinkedHashMap) {
                    try {
                        converted[i] = objectMapper.convertValue(param, targetType);
                        System.out.println("参数类型转换成功: LinkedHashMap -> " + targetType.getName());
                    } catch (Exception e) {
                        System.err.println("参数类型转换失败: " + e.getMessage());
                        converted[i] = param; // 保持原样
                    }
                } else {
                    // 其他类型，尝试转换
                    try {
                        converted[i] = objectMapper.convertValue(param, targetType);
                    } catch (Exception e) {
                        System.err.println("参数转换失败，使用原始参数: " + e.getMessage());
                        converted[i] = param;
                    }
                }
            }

            return converted;
        }

        private String generateRequestId(){
            return "req-"+System.currentTimeMillis()+"-"+Thread.currentThread().getId();
        }
    }

}
