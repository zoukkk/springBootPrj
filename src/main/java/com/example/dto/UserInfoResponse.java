package com.example.dto;

import java.util.List;

public record UserInfoResponse(Long id, String username, String nickname, String avatar,
                               List<String> roles) {
}
