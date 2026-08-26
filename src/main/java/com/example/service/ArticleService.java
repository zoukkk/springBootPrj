package com.example.service;

import com.example.pojo.Article;
import com.example.pojo.Ids;
import com.example.pojo.PageBean;

public interface ArticleService {
    // 新增文章
    void add(Article article);
    // 文章列表
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);
    // 文章详情
    Article detail(Integer id);
    // 更新文章
    void update(Article article);
    // 刪除文章
    void delete(Ids id);
}
