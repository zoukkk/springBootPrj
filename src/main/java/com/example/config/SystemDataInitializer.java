package com.example.config;

import com.example.mapper.AuthMapper;
import com.example.pojo.SysUser;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SystemDataInitializer implements ApplicationRunner {
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemDataInitializer(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SysUser admin = authMapper.findUserByUsername("adming");
        if (admin == null) {
            authMapper.insertUser("adming", passwordEncoder.encode("123456"), "管理员");
            admin = authMapper.findUserByUsername("adming");
        }
        authMapper.assignRole(admin.getId(), "admin");
    }
}
