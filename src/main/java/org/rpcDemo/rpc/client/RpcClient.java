package org.rpcDemo.rpc.client;

import org.rpcDemo.rpc.model.RpcRequest;
import org.rpcDemo.rpc.model.RpcResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
            ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input=new ObjectInputStream(socket.getInputStream())){
            System.out.println("客户端: 连接到服务端 "+address);
            System.out.println("客户端: 发送请求 "+request);

            // 发送请求
            output.writeObject(request);
            output.flush();

            // 接收响应
            RpcResponse response=(RpcResponse) input.readObject();
            System.out.println("客户端: 收到响应 "+response);
            return response;
        }catch (Exception e){
            System.err.println("客户端: RPC调用失败 - "+e.getMessage());
            return RpcResponse.error(e,"error");
        }
    }
}
