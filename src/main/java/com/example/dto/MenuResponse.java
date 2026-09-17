package com.example.dto;

import java.util.List;

public record MenuResponse(Long id, Long parentId, String name, String path, String component,
                           String icon, Integer sort, List<MenuResponse> children) {
}
