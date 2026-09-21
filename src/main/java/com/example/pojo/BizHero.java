package com.example.pojo;

import lombok.Data;

@Data
public class BizHero {
    private Long id;
    private String riotChampionId;
    private String dataVersion;
    private String avatarUrl;
    private String name;
    private String nickname;
    private String role;
    private Long factionId;
    private Integer gender;
    private String introduction;
    private Integer status;
    private Integer delFlag;
    private String createTime;
    private String updateTime;
}
