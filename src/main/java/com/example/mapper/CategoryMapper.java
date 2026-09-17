package com.example.mapper;

import com.example.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {
    // 新增
    @Insert("insert into zip_st_category(category_name,category_alias,create_user,create_time,update_time)" +
    " values(#{categoryName},#{categoryAlias},#{createUser},#{createTime},#{updateTime})")
    void add(Category category);

    // 查询列表
    @Select("select * from zip_st_category where create_user = #{userid}")
    List<Category> list(Integer userid);

    // 查询详情
    @Select("select * from zip_st_category where id = #{id}")
    Category detail(Integer id);

    // 更新文章
    @Update("update zip_st_category set category_name=#{categoryName},category_alias = #{categoryAlias},update_time=CURRENT_TIMESTAMP where id=#{id}")
    void update(Category category);

    // 删除文章
    @Delete("delete from zip_st_category where id=#{id}")
    void delete(Integer id);
}
