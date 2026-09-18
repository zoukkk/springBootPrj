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
        initializeMenus();

        SysUser admin = authMapper.findUserByUsername("adming");
        if (admin == null) {
            authMapper.insertUser("adming", passwordEncoder.encode("123456"), "管理员");
            admin = authMapper.findUserByUsername("adming");
        }
        authMapper.assignRole(admin.getId(), "admin");
    }

    private void initializeMenus() {
        upsertMenu(1L, 0L, "首页", "/dashboard", "DashboardView", "HomeFilled", 1);
        upsertMenu(2L, 0L, "系统管理", "/system", "SystemLayout", "Setting", 2);
        upsertMenu(3L, 2L, "用户管理", "/system/users", "UserView", "User", 1);
        upsertMenu(4L, 2L, "角色管理", "/system/roles", "RoleView", "UserFilled", 2);
        upsertMenu(5L, 2L, "菜单管理", "/system/menus", "MenuView", "Menu", 3);
    }

    private void upsertMenu(Long id, Long parentId, String name, String path,
                            String component, String icon, Integer sort) {
        authMapper.insertMenu(id, parentId, name, path, component, icon, sort);
        authMapper.updateMenu(id, parentId, name, path, component, icon, sort);
    }
}
