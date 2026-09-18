package com.example.mapper;

import com.example.pojo.SysMenu;
import com.example.pojo.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AuthMapper {
    @Select("select id, username, password, nickname, avatar, status from sys_user " +
            "where username = #{username} and del_flag = 0 limit 1")
    SysUser findUserByUsername(String username);

    @Select("select id, username, password, nickname, avatar, status from sys_user " +
            "where id = #{id} and del_flag = 0 limit 1")
    SysUser findUserById(Long id);

    @Insert("insert into sys_user(username, password, nickname, status) " +
            "values(#{username}, #{password}, #{nickname}, 1)")
    void insertUser(@Param("username") String username,
                    @Param("password") String password,
                    @Param("nickname") String nickname);

    @Insert("insert or ignore into sys_user(username, password, nickname, status) " +
            "values(#{username}, #{password}, #{nickname}, 1)")
    int insertUserIfAbsent(@Param("username") String username,
                           @Param("password") String password,
                           @Param("nickname") String nickname);

    @Select("select distinct r.code from sys_role r " +
            "join sys_user_role ur on ur.role_id = r.id and ur.del_flag = 0 " +
            "where ur.user_id = #{userId} and r.status = 1 and r.del_flag = 0 order by r.code")
    List<String> findRoleCodesByUserId(Long userId);

    @Insert("insert or ignore into sys_user_role(user_id, role_id) " +
            "select #{userId}, id from sys_role where code = #{roleCode} and del_flag = 0")
    void assignRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Insert("insert or ignore into sys_menu " +
            "(id, parent_id, name, path, component, icon, sort, visible, status, del_flag) " +
            "values(#{id}, #{parentId}, #{name}, #{path}, #{component}, #{icon}, #{sort}, 1, 1, 0)")
    void insertMenu(@Param("id") Long id,
                    @Param("parentId") Long parentId,
                    @Param("name") String name,
                    @Param("path") String path,
                    @Param("component") String component,
                    @Param("icon") String icon,
                    @Param("sort") Integer sort);

    @Update("update sys_menu set parent_id = #{parentId}, name = #{name}, path = #{path}, " +
            "component = #{component}, icon = #{icon}, sort = #{sort}, visible = 1, status = 1, " +
            "del_flag = 0, update_time = CURRENT_TIMESTAMP where id = #{id}")
    void updateMenu(@Param("id") Long id,
                    @Param("parentId") Long parentId,
                    @Param("name") String name,
                    @Param("path") String path,
                    @Param("component") String component,
                    @Param("icon") String icon,
                    @Param("sort") Integer sort);

    @Select("select id, parent_id, name, path, component, icon, sort from sys_menu " +
            "where visible = 1 and status = 1 and del_flag = 0 order by sort, id")
    List<SysMenu> findAllActiveMenus();

    @Select("select distinct m.id, m.parent_id, m.name, m.path, m.component, m.icon, m.sort " +
            "from sys_menu m " +
            "join sys_role_menu rm on rm.menu_id = m.id and rm.del_flag = 0 " +
            "join sys_user_role ur on ur.role_id = rm.role_id and ur.del_flag = 0 " +
            "join sys_role r on r.id = ur.role_id and r.status = 1 and r.del_flag = 0 " +
            "where ur.user_id = #{userId} and m.visible = 1 and m.status = 1 and m.del_flag = 0 " +
            "order by m.sort, m.id")
    List<SysMenu> findActiveMenusByUserId(Long userId);
}
