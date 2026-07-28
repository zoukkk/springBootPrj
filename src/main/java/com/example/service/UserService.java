package com.example.service;
import com.example.pojo.User;

public interface UserService {
    // 查询用户
    User findByUserName(String username);
    // 注册
    void register(String username,String password);
    // 更新用户数据
    void updata(User user);
    // 更新头像
    void updataAvatar(String avatarUrl);
    // 更新密码
    void updataPwd(String newPwd);
}