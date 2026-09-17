package com.example.mapper;

import com.example.pojo.Article;
import com.example.pojo.Ids;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleMapper {
    // 新增
    @Insert("insert into zip_st_article(title,content,cover_img,state,category_id,create_user,create_time,update_time)"+
    " values(#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime})")
    void add(Article article);

    // 查询
    List<Article> list(Integer userId, Integer categoryId, String state);

    // 查详情
    @Select("select * from zip_st_article where id=#{id} and create_user=#{userId}")
    Article detail(Integer userId, Integer id);

    // 更新
    @Update("update zip_st_article set title=#{article.title}, content=#{article.content}, cover_img=#{article.coverImg}, state=#{article.state}, update_time=CURRENT_TIMESTAMP where id=#{article.id} and create_user=#{userId}")
    void update(@Param("userId") Integer userId, @Param("article") Article article);

    // 删除
    void delete(@Param("userId") Integer userId,@Param("ids") Ids ids);
}
