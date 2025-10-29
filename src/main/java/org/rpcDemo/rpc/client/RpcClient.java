package org.rpcDemo.rpc.client;

import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;
import org.rpcDemo.rpc.serialize.JsonSerializer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RpcClient {
    private final ServiceDiscoverer serviceDiscoverer;

    public RpcClient(ServiceDiscoverer serviceDiscoverer){
        this.serviceDiscoverer=serviceDiscoverer;
    }

    /**
     * 发送RPC请求并获取响应
     */
    public RpcResponse sendRequest(RpcRequest request){
        String serviceName=request.getInterfaceName();
        InetSocketAddress address=serviceDiscoverer.discover(serviceName);
        try(Socket socket=new Socket(address.getHostName(), address.getPort());
            OutputStream outputStream=socket.getOutputStream();
            InputStream inputStream=socket.getInputStream()){
            System.out.println("客户端: 连接到服务端 "+address);
            System.out.println("客户端: 发送请求 "+request);

            // 调试：打印JSON格式的请求
            System.out.println("请求JSON:\\n"+ JsonSerializer.toJsonString(request));

            // Json序列化请求
            byte[] requestBytes=JsonSerializer.serialize(request);
            System.out.println("发送请求数据，长度: "+requestBytes.length+" 字节");

            // 发送请求
            outputStream.write(requestBytes);
            outputStream.flush();

            // 读取响应
            byte[] responseBytes=readResponse(inputStream);
            System.out.println("接收到响应数据，长度: "+responseBytes.length+" 字节");

            // JSON反序列化响应
            RpcResponse response=JsonSerializer.deserialize(responseBytes,RpcResponse.class);
            System.out.println("客户端: 收到响应 "+response);

            // 调试
            System.out.println("🔍 客户端调试 - 反序列化后的response: " + response);
            System.out.println("🔍 客户端调试 - resultType字段: " + response.getResultType());

            // 调试：打印JSON格式的响应
            System.out.println("响应JSON:\\n"+JsonSerializer.toJsonString(response));

            return response;
        }catch (Exception e){
            System.err.println("客户端: RPC调用失败 - "+e.getMessage());
            return RpcResponse.error(e,"error");
        }
    }

    private byte[] readResponse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        byte[] data=new byte[1024];
        int bytesRead;

        while((bytesRead= inputStream.read(data,0,data.length))!=-1){
            buffer.write(data,0,bytesRead);
            // 简单判断：如果可读数据小于缓冲区，认为读取完成
            if(inputStream.available()==0){
                break;
            }
        }

        return buffer.toByteArray();
    }
}
