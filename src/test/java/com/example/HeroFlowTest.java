package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/hero-flow-test.db",
        "spring.data.redis.database=15",
        "app.jwt.secret=hero-flow-test-secret-must-be-at-least-32-bytes"
})
@AutoConfigureMockMvc
class HeroFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    @AfterEach
    void clearTestRedisDatabase() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void supportsHeroCrudMembershipAndFactionRecovery() throws Exception {
        String token = login();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long factionId = createFaction(token, "test-faction-" + suffix);
        long heroId = createHero(token, factionId, "Hero-" + suffix);

        mockMvc.perform(get("/api/heroes/list")
                        .param("factionId", String.valueOf(factionId))
                        .param("keyword", suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(heroId))
                .andExpect(jsonPath("$.data.list[0].factionId").value(factionId));

        Map<String, Object> edited = heroBody(factionId, "Hero-" + suffix);
        edited.put("nickname", "Edited-" + suffix);
        edited.put("status", 0);
        mockMvc.perform(put("/api/heroes/edit/{id}", heroId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edited)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Edited-" + suffix))
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(put("/api/heroes/remove/{id}", heroId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.factionId").doesNotExist());

        mockMvc.perform(put("/api/heroes/assign/{id}", heroId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"factionId\":" + factionId + ",\"role\":\"captain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.factionId").value(factionId))
                .andExpect(jsonPath("$.data.role").value("captain"));

        mockMvc.perform(delete("/api/factions/delete/{id}", factionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("存在挂载英雄，不能删除，请先迁移成员"));

        mockMvc.perform(put("/api/heroes/remove/{id}", heroId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/factions/delete/{id}", factionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/factions/deleted-list")
                        .param("keyword", suffix)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(factionId));

        mockMvc.perform(put("/api/factions/restore/{id}", factionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(factionId));

        mockMvc.perform(put("/api/heroes/assign/{id}", heroId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"factionId\":" + factionId + ",\"role\":\"captain\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/factions/status/{id}", factionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0,\"cascade\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(get("/api/heroes/detail/{id}", heroId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(put("/api/heroes/remove/{id}", heroId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/heroes/delete/{id}", heroId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/heroes/detail/{id}", heroId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void rejectsAnonymousHeroRequests() throws Exception {
        mockMvc.perform(get("/api/heroes/list"))
                .andExpect(status().isUnauthorized());
    }

    private long createFaction(String token, String code) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parentId", 0);
        body.put("name", "Faction-" + code);
        body.put("code", code);
        body.put("iconUrl", "");
        body.put("themeColor", "#C89B3C");
        body.put("description", "test");
        body.put("sort", 99);
        body.put("status", 1);
        String response = mockMvc.perform(post("/api/factions/add")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createHero(String token, long factionId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/heroes/add")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heroBody(factionId, name))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("factionId").asLong()).isEqualTo(factionId);
        return data.path("id").asLong();
    }

    private Map<String, Object> heroBody(long factionId, String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("avatarUrl", "https://example.com/avatar.png");
        body.put("name", name);
        body.put("nickname", "Nickname");
        body.put("role", "member");
        body.put("factionId", factionId);
        body.put("gender", 1);
        body.put("introduction", "test hero");
        body.put("status", 1);
        return body;
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"adming\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
