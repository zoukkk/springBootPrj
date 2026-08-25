package com.example.controller;

import com.example.pojo.Article;
import com.example.pojo.PageBean;
import com.example.pojo.Result;
import com.example.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @PostMapping("/add")
    public Result add(@RequestBody @Validated Article article) {
        articleService.add(article);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageBean<Article>> list(Integer pageNum, Integer pageSize, @RequestParam(required = false) Integer categoryId, @RequestParam(required = false) String state) {
        PageBean<Article> pb = articleService.list(pageNum, pageSize, categoryId, state);
        return Result.success(pb);
    }

    @GetMapping("/detail")
    public Result<Article> detail(@RequestParam Integer id) {
        Article result = articleService.detail(id);
        return Result.success(result);
    }

    @PostMapping("update")
    public Result update(@RequestBody @Validated Article article) {
        articleService.update(article);
        return Result.success();
    }

    @GetMapping("delete")
    public Result delete(@RequestParam Integer id) {
        articleService.delete(id);
        return Result.success();
    }
}

