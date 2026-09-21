package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.FactionMapper;
import com.example.mapper.HeroMapper;
import com.example.pojo.BizFaction;
import com.example.pojo.BizHero;
import com.example.vo.RiotSyncResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiotHeroSyncService {
    private static final String VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final String UNIVERSE_URL =
            "https://universe-meeps.leagueoflegends.com/v1/zh_cn/explore2/index.json";
    private static final Map<String, String> LORE_FACTION_OVERRIDES = Map.ofEntries(
            Map.entry("Ambessa", "noxus"),
            Map.entry("Anivia", "freljord"),
            Map.entry("Aurora", "freljord"),
            Map.entry("Briar", "noxus"),
            Map.entry("Chogath", "void"),
            Map.entry("Corki", "bandle-city"),
            Map.entry("KogMaw", "void"),
            Map.entry("Locke", "demacia"),
            Map.entry("Malzahar", "void"),
            Map.entry("Mel", "noxus"),
            Map.entry("Naafiri", "shurima"),
            Map.entry("Olaf", "freljord"),
            Map.entry("Rell", "noxus"),
            Map.entry("Renata", "zaun"),
            Map.entry("Skarner", "ixtal"),
            Map.entry("Teemo", "bandle-city"),
            Map.entry("Vayne", "demacia"),
            Map.entry("Yunara", "ionia"),
            Map.entry("Zilean", "shurima")
    );

    private final HeroMapper heroMapper;
    private final FactionMapper factionMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public RiotHeroSyncService(HeroMapper heroMapper, FactionMapper factionMapper,
                               ObjectMapper objectMapper, TransactionTemplate transactionTemplate) {
        this.heroMapper = heroMapper;
        this.factionMapper = factionMapper;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public RiotSyncResult sync(boolean overwriteFaction) {
        String version = fetchJson(VERSIONS_URL).path(0).asText();
        if (version.isBlank()) {
            throw upstreamError("Riot Data Dragon 未返回有效版本");
        }
        String championUrl = "https://ddragon.leagueoflegends.com/cdn/" + version
                + "/data/zh_CN/champion.json";
        JsonNode champions = fetchJson(championUrl).path("data");
        Map<String, String> universeFactions = extractUniverseFactions(fetchJson(UNIVERSE_URL));
        return transactionTemplate.execute(status ->
                importChampions(version, champions, universeFactions, overwriteFaction));
    }

    private RiotSyncResult importChampions(String version, JsonNode champions,
                                           Map<String, String> universeFactions,
                                           boolean overwriteFaction) {
        if (!champions.isObject()) {
            throw upstreamError("Riot Data Dragon 英雄数据格式异常");
        }
        Map<String, Long> factionIds = factionIds();
        Long runeterraId = factionIds.get("runeterra");
        if (runeterraId == null) {
            throw new BusinessException("缺少符文之地根阵营");
        }

        int total = 0;
        int official = 0;
        int inferred = 0;
        int inserted = 0;
        int updated = 0;
        List<String> fallbackIds = new ArrayList<>();

        var fields = champions.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode source = entry.getValue();
            String riotChampionId = source.path("id").asText(entry.getKey());
            String name = source.path("title").asText().trim();
            String nickname = source.path("name").asText().trim();
            String factionSlug = universeFactions.get(name);
            String factionCode = toFactionCode(factionSlug);
            if (factionSlug == null && LORE_FACTION_OVERRIDES.containsKey(riotChampionId)) {
                factionCode = LORE_FACTION_OVERRIDES.get(riotChampionId);
                inferred++;
            }
            Long officialFactionId = factionIds.get(factionCode);
            if ((factionSlug == null && !LORE_FACTION_OVERRIDES.containsKey(riotChampionId))
                    || officialFactionId == null) {
                officialFactionId = runeterraId;
                fallbackIds.add(riotChampionId);
            } else if (factionSlug != null) {
                official++;
            }

            BizHero hero = heroMapper.findByRiotChampionId(riotChampionId);
            if (hero == null) {
                hero = heroMapper.findByName(name);
            }
            boolean isNew = hero == null;
            if (isNew) {
                hero = new BizHero();
                hero.setRole("阵营成员");
                hero.setGender(0);
                hero.setStatus(1);
                hero.setFactionId(officialFactionId);
            } else if (overwriteFaction || hero.getFactionId() == null) {
                hero.setFactionId(officialFactionId);
            }

            hero.setRiotChampionId(riotChampionId);
            hero.setDataVersion(version);
            hero.setAvatarUrl("https://ddragon.leagueoflegends.com/cdn/" + version
                    + "/img/champion/" + source.path("image").path("full").asText());
            hero.setName(name);
            hero.setNickname(nickname);
            hero.setIntroduction(source.path("blurb").asText());

            if (isNew) {
                heroMapper.insert(hero);
                inserted++;
            } else {
                heroMapper.updateOfficialData(hero);
                updated++;
            }
            total++;
        }
        return new RiotSyncResult(version, total, official, inferred, fallbackIds.size(), inserted, updated,
                List.copyOf(fallbackIds));
    }

    private Map<String, String> extractUniverseFactions(JsonNode root) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonNode module : root.path("modules")) {
            for (JsonNode champion : module.path("featured-champions")) {
                if (!"champion".equals(champion.path("type").asText())) {
                    continue;
                }
                String name = champion.path("name").asText().trim();
                String slug = champion.path("associated-faction-slug").asText().trim();
                if (!name.isBlank() && !slug.isBlank()) {
                    result.putIfAbsent(name, slug);
                }
            }
        }
        return result;
    }

    private Map<String, Long> factionIds() {
        Map<String, Long> result = new HashMap<>();
        for (String code : List.of("runeterra", "bandle-city", "bilgewater", "demacia", "freljord",
                "ionia", "ixtal", "noxus", "piltover", "shadow-isles", "shurima", "targon",
                "void", "zaun")) {
            BizFaction faction = factionMapper.findByCode(code);
            if (faction != null) {
                result.put(code, faction.getId());
            }
        }
        return result;
    }

    private String toFactionCode(String universeSlug) {
        if (universeSlug == null || universeSlug.isBlank() || "unaffiliated".equals(universeSlug)) {
            return "runeterra";
        }
        if ("mount-targon".equals(universeSlug)) {
            return "targon";
        }
        return universeSlug;
    }

    private JsonNode fetchJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("User-Agent", "springBootPrj-riot-data-sync/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw upstreamError("Riot 数据源请求失败，HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw upstreamError("Riot 数据同步被中断");
        } catch (IOException | IllegalArgumentException exception) {
            throw upstreamError("无法读取 Riot 数据：" + exception.getMessage());
        }
    }

    private BusinessException upstreamError(String message) {
        return new BusinessException(1, HttpStatus.BAD_GATEWAY, message);
    }
}
