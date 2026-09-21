package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateHeroRequest(
        @Size(max = 255, message = "头像地址不能超过255个字符") String avatarUrl,
        @NotBlank(message = "英雄姓名不能为空")
        @Size(max = 50, message = "英雄姓名不能超过50个字符") String name,
        @Size(max = 50, message = "英雄昵称不能超过50个字符") String nickname,
        @Size(max = 50, message = "阵营身份不能超过50个字符") String role,
        @Positive(message = "阵营ID必须大于0") Long factionId,
        @Min(value = 0, message = "性别只能为0、1或2")
        @Max(value = 2, message = "性别只能为0、1或2") Integer gender,
        @Size(max = 2000, message = "人物介绍不能超过2000个字符") String introduction,
        @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1") Integer status) {
}
