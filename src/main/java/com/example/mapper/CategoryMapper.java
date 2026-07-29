package com.example.mapper;

import com.example.pojo.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper {
    // 新增
    @Insert("insert into zip_st_category(category_name,category_alias,create_user,create_time,update_time)" +
    " values(#{categoryName},#{categoryAlias},#{createUser,},#{createTime},#{updateTime})")
    void add(Category category);
}
