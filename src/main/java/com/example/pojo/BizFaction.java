package com.example.pojo;

import lombok.Data;

@Data
public class BizFaction {
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String iconUrl;
    private String themeColor;
    private Long leaderHeroId;
    private String description;
    private Integer sort;
    private Integer status;
    private Integer delFlag;
    private String createTime;
    private String updateTime;
}
