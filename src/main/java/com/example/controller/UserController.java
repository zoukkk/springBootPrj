package com.example.controller;

import com.example.pojo.Result;
import com.example.pojo.User;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import com.example.utils.Md5Util;
import com.example.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\S{2,18}$") String username, @Pattern(regexp = "^\\S{2,18}$") String password) {
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
    public Result<String> login(@Pattern(regexp = "^\\S{2,18}$") String username, @Pattern(regexp = "^\\S{2,18}$") String password) {
        User loginUser = userService.findByUserName(username);
        if (loginUser == null) {
            return Result.error("暂无此用户，请先注册！");
        }
        if (Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", loginUser.getId());
            user.put("username", loginUser.getUsername());
            String token = JwtUtil.genToken(user);
            return Result.success(token);
        }
        return Result.error("密码错误！");
    }

    @GetMapping("/userInfo")
    public Result<User> userInfo() {
        Map<String, Object> map = ThreadLocalUtil.get();
        String userName = (String) map.get("username");
        return Result.success(userService.findByUserName(userName));
    }

    @PostMapping("/update")
    public Result update(@RequestBody @Validated User user) {
        userService.updata(user);
        return Result.success();
    }

    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl) {
        userService.updataAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("参数异常");
        }
        Map<String,Object> map = ThreadLocalUtil.get();
        String user_name = (String) map.get("username");
        User loginUser =  userService.findByUserName(user_name);
        if(!loginUser.getPassword().equals(Md5Util.getMD5String(oldPwd))){
            return Result.error("原密码有误");
        }
        if(!rePwd.equals(newPwd)){
            return Result.error("两次填写的密码不一致");
        }
        userService.updataPwd(newPwd);
        return Result.success();
    }
}
