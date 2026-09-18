package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^\\S{2,18}$", message = "用户名必须为2到18位非空白字符") String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度必须为6到32位") String password) {
}
