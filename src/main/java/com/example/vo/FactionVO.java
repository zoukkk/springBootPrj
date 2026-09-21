package com.example.vo;

import java.util.List;

public record FactionVO(
        Long id,
        Long parentId,
        String name,
        String code,
        String iconUrl,
        String themeColor,
        Long leaderHeroId,
        String description,
        Integer level,
        Integer sort,
        Integer status,
        String createTime,
        String updateTime,
        List<FactionVO> children,
        List<HeroVO> members) {
}
