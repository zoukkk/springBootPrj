package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateFactionRequest(
        @PositiveOrZero(message = "父级ID不能小于0") Long parentId,
        @NotBlank(message = "阵营名称不能为空")
        @Size(max = 50, message = "阵营名称不能超过50个字符") String name,
        @NotBlank(message = "阵营编码不能为空")
        @Size(max = 50, message = "阵营编码不能超过50个字符") String code,
        @Size(max = 255, message = "图标地址不能超过255个字符") String iconUrl,
        @Size(max = 20, message = "主题色不能超过20个字符") String themeColor,
        @PositiveOrZero(message = "领袖英雄ID不能小于0") Long leaderHeroId,
        String description,
        @PositiveOrZero(message = "排序值不能小于0") Integer sort,
        @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1") Integer status) {
}
