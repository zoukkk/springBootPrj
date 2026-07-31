package com.example.controller;

import com.example.pojo.Category;
import com.example.pojo.Result;
import com.example.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category) {
        categoryService.add(category);
        return Result.success();
    }

    @GetMapping
    public Result<List<Category>> list() {
        List<Category> data = categoryService.list();
        return Result.success(data);
    }
    @GetMapping("/detail")
    public Result<Category> detail(Integer id) {
        Category data = categoryService.detail(id);
        return Result.success(data);
    }

    @PostMapping("/update")
    public Result update(@RequestBody @Validated(Category.Update.class) Category category) {
        categoryService.update(category);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody Map<String, Object> params) {
        categoryService.delete(params);
        return Result.success();
    }
}
