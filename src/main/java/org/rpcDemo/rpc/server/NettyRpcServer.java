package org.rpcDemo.rpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.rpcDemo.rpc.codec.RpcDecoder;
import org.rpcDemo.rpc.codec.RpcEncoder;
import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyRpcServer {
    private final int port;
    private final Map<String, Object> serviceRegistry=new HashMap<>();
    private final AtomicBoolean running=new AtomicBoolean(false);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyRpcServer(int port) {
        this.port = port;
    }

    public void registerService(String interfaceName, Object serviceImpl){
        serviceRegistry.put(interfaceName,serviceImpl);
        System.out.println("Netty服务端注册服务: "+interfaceName + " -> " + serviceImpl.getClass().getSimpleName());
    }

    public void start(){
        if(!running.compareAndSet(false,true)){
            System.out.println("Netty服务端已经在运行中...");
            return;
        }

        bossGroup=new NioEventLoopGroup(1);
        workerGroup=new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap=new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline= socketChannel.pipeline();

                            // 添加编解码器（解决粘包问题）
                            pipeline.addLast(new RpcDecoder(RpcRequest.class));// 解码：字节 -> RpcRequest
                            pipeline.addLast(new RpcEncoder(RpcResponse.class));// 编码：RpcResponse -> 字节

                            // 添加业务处理器
                            pipeline.addLast(new NettyRpcServerHandler(serviceRegistry));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);

            // 绑定端口，开始接收连接
            ChannelFuture channelFuture=serverBootstrap.bind(port).sync();
            System.out.println("=== Netty RPC服务端启动 ===");
            System.out.println("监听端口: " + port);
            System.out.println("已注册服务: " + serviceRegistry.keySet());
            System.out.println("等待客户端连接...");

            // 等待服务器socket关闭
            channelFuture.channel().closeFuture().sync();
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }finally {
            stop();
        }
    }

    public void stop(){
        if(running.compareAndSet(true,false)){
            if(workerGroup!=null){
                workerGroup.shutdownGracefully();
            }
            if(bossGroup!=null){
                bossGroup.shutdownGracefully();
            }
            System.out.println("Netty RPC服务端已停止");
        }
    }

    private class NettyRpcServerHandler extends SimpleChannelInboundHandler<RpcRequest>{
        private final Map<String, Object> serviceRegistry;
        private final ObjectMapper objectMapper=new ObjectMapper();

        public NettyRpcServerHandler(Map<String, Object> serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest request) throws Exception {
            System.out.println("来自NettyRpcServer-channelRead0:Netty服务端收到请求: " + request.getMethodName());
            System.out.println("来自NettyRpcServer-channelRead0:客户端requestId: " + request.getRequestId());
            System.out.println("来自NettyRpcServer-channelRead0:请求对象: " + request);

            RpcResponse response=handleRequest(request);
            System.out.println("来自NettyRpcServer-channelRead0:Netty服务端发送响应，responseId: " + response.getRequestId());

            // 发送响应
            channelHandlerContext.writeAndFlush(response);
        }

        private RpcResponse handleRequest(RpcRequest request){
            String requestId=request.getRequestId();
            System.out.println("来自NettyRpcServer-handleRequest:handleRequest中读取的requestId: " + requestId);

            if(requestId==null||requestId.isEmpty()){
                requestId = "netty-" + System.currentTimeMillis() + "-" + System.nanoTime();
            }

            try{
                // 获取服务实现
                Object service=serviceRegistry.get(request.getInterfaceName());
                if(service==null){
                    throw new RuntimeException("服务未找到: "+request.getInterfaceName());
                }

                // 转换参数类型
                Object[] convertedParameters=convertParameters(request.getParameters(), request.getParameterTypes());

                // 通过反射调用方法
                Method method=service.getClass().getMethod(
                        request.getMethodName(),
                        request.getParameterTypes()
                );

                Object result=method.invoke(service,convertedParameters);

                // 创建响应
                RpcResponse response=RpcResponse.success(result,requestId);

                // 设置resultType
                if (result != null) {
                    response.setResultType(result.getClass().getName());
                }

                return response;
            }catch(Exception e){
                System.err.println("Netty服务端处理请求时发生错误: "+e.getMessage());
                return RpcResponse.error(e, requestId);
            }
        }

        /**
         * 参数类型转换
         */
        private Object[] convertParameters(Object[] parameters, Class<?>[] parameterTypes){
            if(parameters==null){
                return null;
            }

            Object[] converted=new Object[parameters.length];

            for(int i=0;i< parameters.length;i++){
                Object param=parameters[i];
                Class<?> targetType=parameterTypes[i];

                if(param==null){
                    converted[i]=null;
                    continue;
                }

                if(targetType.isInstance(param)){
                    converted[i]=param;
                    continue;
                }

                if(param instanceof java.util.LinkedHashMap){
                    try{
                        converted[i]=objectMapper.convertValue(param, targetType);
                    }catch(Exception e){
                        System.err.println("参数类型转换失败: "+e.getMessage());
                        converted[i]=param;
                    }
                }else{
                    converted[i]=param;
                }
            }
            return converted;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause){
            System.err.println("Netty服务端处理异常: "+cause.getMessage());
            channelHandlerContext.close();
        }
    }
}
