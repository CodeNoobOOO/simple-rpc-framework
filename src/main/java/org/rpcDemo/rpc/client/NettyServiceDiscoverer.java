package org.rpcDemo.rpc.client;

import java.net.InetSocketAddress;

public class NettyServiceDiscoverer implements ServiceDiscoverer{

    private final InetSocketAddress serviceAddress;

    public NettyServiceDiscoverer(String host, int port) {
        this.serviceAddress = new InetSocketAddress(host,port);
    }

    @Override
    public InetSocketAddress discover(String serviceName) {
        System.out.println("Netty服务发现: "+serviceName+" -> " + serviceAddress);
        return serviceAddress;
    }
}
