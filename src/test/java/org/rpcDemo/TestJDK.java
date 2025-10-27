package org.rpcDemo;

public class TestJDK {
    public static void main(String[] args) {
        System.out.println("Java版本: " + System.getProperty("java.version"));
        System.out.println("编译版本: " + System.getProperty("java.specification.version"));

        // 测试JDK 8特性 - Lambda表达式
        Runnable r = () -> System.out.println("Lambda表达式正常工作!");
        r.run();
    }
}