package org.rpcDemo.rpc.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JSON序列化器
 */
public class JsonSerializer {
    private static final ObjectMapper objectMapper=new ObjectMapper();

    public static <T> byte[] serialize(T obj){
        try{
            String json= objectMapper.writeValueAsString(obj);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败",e);
        }
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz){
        try{
            String json=new String(bytes,StandardCharsets.UTF_8);

            // 调试
            System.out.println("🔍 JsonSerializer - 原始JSON: " + json);  // 看看JSON里有没有resultType字段

            return objectMapper.readValue(json,clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON序列化失败",e);
        }
    }

    public static String toJsonString(Object obj){
        try{
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON字符串失败",e);
        }
    }
}
