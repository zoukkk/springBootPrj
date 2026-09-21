package com.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssignHeroRequest(
        @NotNull(message = "阵营ID不能为空")
        @Positive(message = "阵营ID必须大于0") Long factionId,
        @Size(max = 50, message = "阵营身份不能超过50个字符") String role) {
}
