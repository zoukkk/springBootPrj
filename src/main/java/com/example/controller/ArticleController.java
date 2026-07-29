package com.example.controller;
import com.example.pojo.Result;
import com.example.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @GetMapping("/list")
    public Result<String> list(/*@RequestHeader(name = "Authorization") String token, HttpServletResponse response*/) {
//        try{
//            Map<String, Object> claims = JwtUtil.parseToken(token);
//            return Result.success("查询succes");
//        }catch(Exception e){
//            response.setStatus(401);
//            return Result.error("未登录");
//        }
        return Result.success("查询succes");
    }
}

