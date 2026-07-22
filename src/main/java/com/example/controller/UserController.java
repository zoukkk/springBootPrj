package com.example.controller;

import com.example.pojo.Result;
import com.example.pojo.User;
import com.example.service.UserService;
import com.example.utils.Md5Util;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\${2,5}$") String username, @Pattern(regexp = "^\\${2,5}$") String password) {
        // 查询用户
        User u = userService.findByUserName(username);
        if (u == null) {
            userService.register(username, password);
            return Result.success();
        } else {
            return Result.error("用户已存在");
        }
    }

    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\${2,5}$") String username, @Pattern(regexp = "^\\${2,5}$") String password) {
        User loginUser = userService.findByUserName(username);
        if (loginUser == null) {
            return Result.error("暂无此用户，请先注册！");
        }
        if (Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
            return Result.success("ddsaddxqddaff");
        }
        return Result.error("密码错误！");
    }

}
