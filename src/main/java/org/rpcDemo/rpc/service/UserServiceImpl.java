package org.rpcDemo.rpc.service;

import org.rpcDemo.rpc.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UserServiceImpl implements UserService{

    // 模拟数据库 - 用Map存储用户数据
    private final Map<Integer, User> userDatabase = new HashMap<>();
    private final AtomicInteger idGenerator;

    public UserServiceImpl() {
        // 初始化一些测试数据
        initializeTestData();

        // 然后让idGenerator从当前最大ID+1开始
        int maxId = userDatabase.size(); // 如果map为空，从0开始
        this.idGenerator = new AtomicInteger(maxId + 1);

        System.out.println("ID生成器初始化完成，下一个ID: " + idGenerator.get());

    }

    private void initializeTestData() {
        userDatabase.put(1, new User(1, "张三", 25));
        userDatabase.put(2, new User(2, "李四", 30));
        userDatabase.put(3, new User(3, "王五", 28));
    }

    @Override
    public User getUserById(Integer id) {
        System.out.println("服务端: 正在查询用户 ID = " + id);

        // 模拟一点网络延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        User user = userDatabase.get(id);
        if (user == null) {
            System.out.println("服务端: 未找到用户 ID = " + id);
        } else {
            System.out.println("服务端: 找到用户 " + user);
        }
        return user;
    }

    @Override
    public Boolean addUser(User user) {
        if (user == null) {
            return false;
        }

        // 如果用户没有ID，自动生成一个
        if (user.getId() == null) {
            int newId = idGenerator.getAndIncrement();
            user.setId(newId);
        }

        System.out.println("服务端: 添加用户 " + user);
        userDatabase.put(user.getId(), user);
        return true;
    }

    @Override
    public Integer getUserCount() {
        int count = userDatabase.size();
        System.out.println("服务端: 当前用户数量 = " + count);
        return count;
    }

    /**
     * 辅助方法：打印所有用户（用于测试）
     */
    public void printAllUsers() {
        System.out.println("=== 所有用户 ===");
        userDatabase.values().forEach(System.out::println);
        System.out.println("==============");
    }

    /**
     * 获取下一个可用的ID（用于测试）
     */
    public int getNextAvailableId() {
        return idGenerator.get();
    }

}
