package org.rpcDemo.rpc.client;

import java.net.InetSocketAddress;

public class SimpleServiceDiscoverer implements ServiceDiscoverer{
    private final InetSocketAddress serviceAddress;

    public SimpleServiceDiscoverer(String host, int port){
        this.serviceAddress=new InetSocketAddress(host,port);
    }

    @Override
    public InetSocketAddress discover(String serviceName) {
        System.out.println("发现服务: "+serviceName+" ->"+serviceAddress);
        return serviceAddress;
    }
}
