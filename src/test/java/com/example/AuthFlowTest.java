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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/auth-flow-test.db",
        "spring.data.redis.database=15",
        "app.jwt.secret=auth-flow-test-secret-must-be-at-least-32-bytes"
})
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void clearTestRedisDatabase() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"adming\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void registersNewUserWithBcryptAndDefaultRole() throws Exception {
        String username = "reg" + UUID.randomUUID().toString().substring(0, 8);
        String request = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", "123456"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String storedPassword = jdbcTemplate.queryForObject(
                "select password from sys_user where username = ?", String.class, username);
        assertThat(storedPassword).startsWith("$2");

        String token = login(username, "123456");
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.roles[0]").value("user"));
        mockMvc.perform(get("/api/auth/menus").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].path").value("/dashboard"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void supportsUserInfoMenusTwoSessionsAndLogout() throws Exception {
        String first = login();
        String second = login();

        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("adming"))
                .andExpect(jsonPath("$.data.roles[0]").value("admin"));

        mockMvc.perform(get("/api/auth/menus").header("Authorization", bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("首页"))
                .andExpect(jsonPath("$.data[1].name").value("系统管理"))
                .andExpect(jsonPath("$.data[1].children.length()").value(3))
                .andExpect(jsonPath("$.data[2].name").value("阵营管理"))
                .andExpect(jsonPath("$.data[2].path").value("/depts"))
                .andExpect(jsonPath("$.data[2].component").value("FactionView"));

        String third = login();
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(first)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(second)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(third)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(second)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", bearer(third)))
                .andExpect(status().isOk());
    }

    @Test
    void requiresBearerTokenForNewEndpoints() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/auth/userinfo").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/auth/userinfo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void filtersMenusForNormalUser() throws Exception {
        Long userId = jdbcTemplate.query(
                "select id from sys_user where username = ?",
                resultSet -> resultSet.next() ? resultSet.getLong(1) : null,
                "menu_test_user");
        if (userId == null) {
            jdbcTemplate.update(
                    "insert into sys_user(username, password, nickname, status) values(?, ?, ?, 1)",
                    "menu_test_user", passwordEncoder.encode("123456"), "菜单测试用户");
            userId = jdbcTemplate.queryForObject(
                    "select id from sys_user where username = ?", Long.class, "menu_test_user");
        }
        jdbcTemplate.update(
                "insert or ignore into sys_user_role(user_id, role_id) " +
                        "select ?, id from sys_role where code = 'user'",
                userId);

        String token = login("menu_test_user", "123456");
        mockMvc.perform(get("/api/auth/menus").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].path").value("/dashboard"));
    }

    @Test
    void keepsLegacyLoginAndRawTokenWorking() throws Exception {
        String response = mockMvc.perform(post("/user/login")
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).path("data").asText();

        mockMvc.perform(get("/category").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void returnsNotFoundForUnknownAuthenticatedEndpoint() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/not-found").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    private String login() throws Exception {
        return login("adming", "123456");
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        String token = json.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
