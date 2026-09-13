package com.devflow.support;

import com.devflow.user.entity.Role;
import com.devflow.user.repository.UserRepository;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Creates authenticated accounts through the real HTTP API so tests exercise the true flow. */
@TestComponent
public class TestAccounts {

    public static final String PASSWORD = "Sup3rSecret!";

    private static final String[] TABLES = {
            "audit_logs", "refresh_tokens", "deployments", "environments", "comments",
            "issue_labels", "issues", "labels", "api_keys", "project_members", "projects", "users"
    };

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public TestAccounts(MockMvc mockMvc, JsonMapper jsonMapper,
                        UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Account(UUID id, String username, String token) {
    }

    /** Truncating beats deleteAll(): order-independent, and it resets every table in one statement. */
    public void deleteEverything() {
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", TABLES) + " RESTART IDENTITY CASCADE");
    }

    /** The very first account registered on an empty instance is promoted to ADMIN. */
    public String registerAdmin(String username) throws Exception {
        return registerRaw(username).token();
    }

    public Account register(String username, Role role) throws Exception {
        Account account = registerRaw(username);
        if (role != Role.DEVELOPER) {
            var user = userRepository.findByUsername(username).orElseThrow();
            user.setRole(role);
            userRepository.saveAndFlush(user);
            return new Account(account.id(), username, login(username));
        }
        return account;
    }

    public String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}""".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body).get("accessToken").asString();
    }

    public void expectLoginSucceeds(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk());
    }

    public void expectLoginFails(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isUnauthorized());
    }

    private Account registerRaw(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s@example.com","username":"%s","password":"%s","fullName":"%s"}"""
                                .formatted(username, username, PASSWORD, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var json = jsonMapper.readTree(body);
        return new Account(UUID.fromString(json.at("/user/id").asString()), username,
                json.get("accessToken").asString());
    }
}
