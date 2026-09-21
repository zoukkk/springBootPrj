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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
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
        "spring.datasource.url=jdbc:sqlite:target/faction-flow-test.db",
        "spring.data.redis.database=15",
        "app.jwt.secret=faction-flow-test-secret-must-be-at-least-32-bytes"
})
@AutoConfigureMockMvc
class FactionFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clearTestRedisDatabase() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void supportsTreeSearchCrudAndHierarchyGuards() throws Exception {
        String token = login();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String rootCode = "alliance_" + suffix;
        String childCode = "branch_" + suffix;
        String grandchildCode = "squad_" + suffix;

        long rootId = create(token, factionBody(0, "联盟", rootCode, 1));
        long childId = create(token, factionBody(rootId, "分部", childCode, 1));
        long grandchildId = create(token, factionBody(childId, "小队", grandchildCode, 1));

        mockMvc.perform(post("/api/factions/add")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                factionBody(0, "重复编码", rootCode.toUpperCase(Locale.ROOT), 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.message").value("阵营编码已存在"));

        mockMvc.perform(get("/api/factions/detail/{id}", childId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(rootId))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.code").value(childCode));

        String treeResponse = mockMvc.perform(get("/api/factions/list")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode rootNode = findByCode(objectMapper.readTree(treeResponse).path("data"), rootCode);
        assertThat(rootNode).isNotNull();
        assertThat(rootNode.path("level").asInt()).isZero();
        assertThat(rootNode.path("children").get(0).path("level").asInt()).isEqualTo(1);
        assertThat(rootNode.path("children").get(0).path("children").get(0)
                .path("level").asInt()).isEqualTo(2);

        String filteredResponse = mockMvc.perform(get("/api/factions/list")
                        .param("keyword", grandchildCode)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode filteredData = objectMapper.readTree(filteredResponse).path("data");
        assertThat(findByCode(filteredData, rootCode)).isNotNull();
        assertThat(findByCode(filteredData, childCode)).isNotNull();
        assertThat(findByCode(filteredData, grandchildCode)).isNotNull();
        assertThat(findByCode(filteredData, "demacia")).isNull();

        mockMvc.perform(put("/api/factions/edit/{id}", rootId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                factionBody(rootId, "联盟", rootCode, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("父级阵营不能指向自身"));

        mockMvc.perform(put("/api/factions/edit/{id}", rootId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                factionBody(grandchildId, "联盟", rootCode, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("父级阵营不能指向当前阵营的子节点"));

        mockMvc.perform(delete("/api/factions/delete/{id}", rootId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("存在下属阵营，不能删除"));

        deleteSuccessfully(token, grandchildId);
        deleteSuccessfully(token, childId);
        deleteSuccessfully(token, rootId);

        mockMvc.perform(get("/api/factions/detail/{id}", rootId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        assertThat(jdbcTemplate.queryForObject(
                "select del_flag from biz_faction where id = ?", Integer.class, rootId)).isEqualTo(1);
    }

    @Test
    void seedsOfficialRegionIconsAndReturnsThemFromListAndDetail() throws Exception {
        String token = login();

        String response = mockMvc.perform(get("/api/factions/list")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        JsonNode runeterra = findByCode(data, "runeterra");
        JsonNode demacia = findByCode(data, "demacia");
        JsonNode piltover = findByCode(data, "piltover");
        JsonNode zaun = findByCode(data, "zaun");
        assertThat(runeterra).isNotNull();
        assertThat(demacia.path("iconUrl").asText()).endsWith("/icon-demacia.png");
        assertThat(piltover.path("iconUrl").asText()).endsWith("/icon-piltoverzaun.png");
        assertThat(zaun.path("iconUrl").asText()).isEqualTo(piltover.path("iconUrl").asText());
        assertThat(findByCode(data, "freljord")).isNotNull();
        assertThat(findByCode(data, "bandle-city")).isNotNull();
        assertThat(demacia.path("leaderHeroId").asLong()).isEqualTo(3L);
        assertThat(demacia.path("members").size()).isEqualTo(3);
        assertThat(demacia.path("members").get(0).path("id").asLong()).isEqualTo(1L);
        assertThat(demacia.path("members").get(0).path("factionId").asLong()).isEqualTo(demacia.path("id").asLong());
        assertThat(demacia.path("members").get(2).path("id").asLong()).isEqualTo(3L);

        mockMvc.perform(get("/api/factions/detail/{id}", demacia.path("id").asLong())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.iconUrl").value(
                        "https://dd.b.pvp.net/5_10_0/core/en_us/img/regions/icon-demacia.png"));
    }

    private long create(String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/factions/add")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void deleteSuccessfully(String token, long id) throws Exception {
        mockMvc.perform(delete("/api/factions/delete/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private Map<String, Object> factionBody(long parentId, String name, String code, int sort) {
        return Map.of(
                "parentId", parentId,
                "name", name,
                "code", code,
                "iconUrl", "",
                "themeColor", "#C89B3C",
                "description", name + "描述",
                "sort", sort,
                "status", 1);
    }

    private JsonNode findByCode(JsonNode nodes, String code) {
        for (JsonNode node : nodes) {
            if (code.equals(node.path("code").asText())) {
                return node;
            }
            JsonNode match = findByCode(node.path("children"), code);
            if (match != null) {
                return match;
            }
        }
        return null;
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
