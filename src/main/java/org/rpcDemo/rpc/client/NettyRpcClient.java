package org.rpcDemo.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.rpcDemo.rpc.codec.RpcDecoder;
import org.rpcDemo.rpc.codec.RpcEncoder;
import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class NettyRpcClient {
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final ConcurrentMap<String, CompletableFuture<RpcResponse>> pendingRequests=new ConcurrentHashMap<>();

    public NettyRpcClient(){
        this.group=new NioEventLoopGroup();
        this.bootstrap=new Bootstrap();

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline=socketChannel.pipeline();

                        // 添加编解码器
                        pipeline.addLast(new RpcDecoder(RpcResponse.class));
                        pipeline.addLast(new RpcEncoder(RpcRequest.class));

                        // 添加业务处理器
                        pipeline.addLast(new NettyRpcClientHandler(pendingRequests));
                    }
                })
                .option(ChannelOption.TCP_NODELAY,true)
                .option(ChannelOption.SO_KEEPALIVE,true);
    }

    /**
     * 发送RPC请求
     */
    public CompletableFuture<RpcResponse> sendRequest(RpcRequest request, InetSocketAddress address){
        // 生成唯一的请求ID
//        String requestId="netty-"+System.currentTimeMillis()+"-"+System.nanoTime();

        String requestId= request.getRequestId();

        CompletableFuture<RpcResponse> future=new CompletableFuture<>();

        try{
            // 连接到服务端
            ChannelFuture channelFuture=bootstrap.connect(address.getHostName(), address.getPort()).sync();

            // 保存请求到待处理映射
            pendingRequests.put(requestId,future);
            System.out.println("来自NettyRpcClient-sendRequest:保存请求到待处理映射: " + requestId);

            // 发送请求
            channelFuture.channel().writeAndFlush(request).addListener((ChannelFutureListener) f->{
                if(f.isSuccess()){
                    System.out.println("来自NettyRpcClient-sendRequest:Netty客户端发送请求成功: "+request.getMethodName() + ", 请求ID: " + requestId);
                }else{
                    System.err.println("来自NettyRpcClient-sendRequest:发送请求失败: " + f.cause().getMessage());
                    future.completeExceptionally(f.cause());
                    pendingRequests.remove(requestId);
                }
            });

            // 设置超时
            group.schedule(()->{
                if(!future.isDone()){
                    System.err.println("来自NettyRpcClient-sendRequest:请求超时: " + requestId);
                    future.completeExceptionally(new RuntimeException("请求超时"));
                    pendingRequests.remove(requestId);
                }
            },30, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
            pendingRequests.remove(requestId);
        }

        return future;
    }

    public void close(){
        group.shutdownGracefully();
    }

    private class NettyRpcClientHandler extends SimpleChannelInboundHandler<RpcResponse>{
        private final ConcurrentMap<String,CompletableFuture<RpcResponse>> pendingRequests;

        public NettyRpcClientHandler(ConcurrentMap<String, CompletableFuture<RpcResponse>> pendingRequests) {
            this.pendingRequests = pendingRequests;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) throws Exception {
            System.out.println("来自NettyRpcClient-channelRead0:Netty客户端收到响应: "+response.getRequestId());

            // 根据请求ID找到对应的CompletableFuture并完成
            String requestId=response.getRequestId();
            CompletableFuture<RpcResponse> future=pendingRequests.remove(requestId);

            if(future!=null){
                System.out.println("来自NettyRpcClient-channelRead0:找到对应的请求，完成Future: " + requestId);
                future.complete(response);
            }else{
                System.err.println("来自NettyRpcClient-channelRead0:未找到对应的请求: "+requestId);
                System.err.println("来自NettyRpcClient-channelRead0:当前待处理请求: " + pendingRequests.keySet());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
            System.err.println("Netty客户端处理异常: "+cause.getMessage());
            ctx.close();
        }
    }
}
