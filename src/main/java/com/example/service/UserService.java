package com.example.service;
import com.example.pojo.User;

public interface UserService {
    User findByUserName(String username);
    void register(String username,String password);
}