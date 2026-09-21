package com.example.vo;

public record HeroVO(
        Long id,
        String riotChampionId,
        String dataVersion,
        String avatarUrl,
        String name,
        String nickname,
        String role,
        Long factionId,
        Integer gender,
        String introduction,
        Integer status,
        String createTime,
        String updateTime) {
}
