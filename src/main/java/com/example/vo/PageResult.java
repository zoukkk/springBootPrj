package com.example.vo;

import java.util.List;

public record PageResult<T>(
        List<T> list,
        long total,
        int pageNum,
        int pageSize) {
}
