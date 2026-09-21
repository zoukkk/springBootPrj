package com.example.vo;

import java.util.List;

public record RiotSyncResult(
        String version,
        int totalChampions,
        int officialFactionCount,
        int inferredFactionCount,
        int fallbackCount,
        int inserted,
        int updated,
        List<String> fallbackChampionIds) {
}
