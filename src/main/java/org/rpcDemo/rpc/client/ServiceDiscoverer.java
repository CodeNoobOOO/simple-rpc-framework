package org.rpcDemo.rpc.client;

import java.net.InetSocketAddress;

public interface ServiceDiscoverer {
    InetSocketAddress discover(String serviceName);
}
