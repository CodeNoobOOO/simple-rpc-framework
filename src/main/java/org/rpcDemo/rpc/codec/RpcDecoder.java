package org.rpcDemo.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.rpcDemo.rpc.serialize.JsonSerializer;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass){
        this.genericClass=genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 确保有足够的数据读取（长度字段 + 数据）
        if(in.readableBytes()<4){
            return;
        }

        // 标记当前读位置
        in.markReaderIndex();

        // 读取数据长度
        int dataLength=in.readInt();

        // 如果可读数据小于实际数据长度，重置读指针等待更多数据
        if(in.readableBytes()<dataLength){
            in.resetReaderIndex();
            return;
        }

        // 读取数据字节
        byte[] data=new byte[dataLength];
        in.readBytes(data);

        // JSON反序列化
        Object obj= JsonSerializer.deserialize(data,genericClass);
        out.add(obj);

        System.out.println("Netty解码: "+dataLength+" 字节 -> "+genericClass.getSimpleName());
    }
}
