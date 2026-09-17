package com.example.pojo;

import lombok.Data;

@Data
public class SysMenu {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
}
