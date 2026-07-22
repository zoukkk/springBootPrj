package com.example.mapper;

import com.example.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    // 查询
    @Select("select * from zip_st_user where username=#{username}")
    User findByUserName(String username);

    // 添加
    @Insert("insert into zip_st_user(username,password,create_time,update_time)" +
    " values(#{username},#{password},now(),now())")
    void add(String username, String password);
}
