package com.example.service.impl;

import com.example.mapper.CategoryMapper;
import com.example.pojo.Category;
import com.example.service.CategoryService;
import com.example.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void add(Category category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("id");
        category.setCreateUser(userid);
        categoryMapper.add(category);
    }

    @Override
    public List<Category> list() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("id");
        return categoryMapper.list(userid);
    }

    @Override
    public Category detail(Integer id) {
        return categoryMapper.detail(id);
    }

    @Override
    public void update(Category category) {
        categoryMapper.update(category);
    }

    @Override
    public void delete(Map<String,Object> params) {
        Integer id =(Integer) params.get("id");
        categoryMapper.delete(id);
    }
}
