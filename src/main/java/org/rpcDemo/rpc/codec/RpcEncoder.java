package org.rpcDemo.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.rpcDemo.rpc.serialize.JsonSerializer;

public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // 只处理指定类型的消息
        if(genericClass.isInstance(msg)){
            // JSON序列化
            byte[] data= JsonSerializer.serialize(msg);

            // 写入数据长度（解决粘包问题）
            out.writeInt(data.length);
            // 写入数据
            out.writeBytes(data);

            System.out.println("Netty编码: "+msg.getClass().getSimpleName()+" -> "+data.length + " 字节");
        }
    }
}
