package org.rpcDemo.rpc.test;

import org.rpcDemo.rpc.model.User;
import org.rpcDemo.rpc.service.UserService;
import org.rpcDemo.rpc.service.UserServiceImpl;

public class ServiceTest {

    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();

        System.out.println("=== 测试本地服务调用 ===");

        // 测试0: 查看下一个可用ID
        System.out.println("下一个可用ID: " + ((UserServiceImpl) userService).getNextAvailableId());

        // 测试1: 查询存在的用户
        User user1 = userService.getUserById(1);
        System.out.println("查询结果: " + user1);

        // 测试2: 查询不存在的用户
        User user2 = userService.getUserById(999);
        System.out.println("查询结果: " + user2);

        // 测试3: 添加新用户
        User newUser = new User(null, "赵六", 35);
        boolean success = userService.addUser(newUser);
        System.out.println("添加用户结果: " + success);
        System.out.println("新用户ID: " + newUser.getId());

        // 测试4: 获取用户数量
        Integer count = userService.getUserCount();
        System.out.println("总用户数: " + count);

        // 测试5: 再添加一个用户
        User anotherUser = new User(null, "钱七", 40);
        userService.addUser(anotherUser);

        // 打印所有用户
        ((UserServiceImpl) userService).printAllUsers();
    }

}
