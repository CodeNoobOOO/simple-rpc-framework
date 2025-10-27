package org.rpcDemo.rpc.service;

import org.rpcDemo.rpc.model.User;

public interface UserService {

    /**
     * 根据ID查询用户
     */
    User getUserById(Integer id);

    /**
     * 添加新用户
     */
    Boolean addUser(User user);

    /**
     * 获取用户数量（用于测试）
     */
    Integer getUserCount();

}
