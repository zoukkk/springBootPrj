package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;


@SpringBootTest
@Disabled("演示型测试会写入默认 Redis 库，不纳入自动化测试")
public class RedisTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testSet() {
        // redis存储
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set("username", "张三");
        operations.set("id", "1", 15, TimeUnit.SECONDS);
    }

    @Test
    public void testGet() {
        // redis获取
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String name = operations.get("username");
        System.out.println(name);
    }

}
