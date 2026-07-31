package com.example.service;
import com.example.pojo.Category;
import java.util.List;
import java.util.Map;

public interface CategoryService {
    // 新增
    void add(Category category);
    // 列表查询
    List<Category> list();
    // 查询详情
    Category detail(Integer id);
    // 更新
    void update(Category category);
    // 删除
    void delete(Map<String,Object> params);
}
