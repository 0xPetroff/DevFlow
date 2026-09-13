package com.devflow.auth;

import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.user.entity.Role;
import com.devflow.user.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestAccounts accounts;

    @BeforeEach
    void reset() {
        accounts.deleteEverything();
    }

    @Test
    void registersFirstUserAsAdminAndReturnsTokens() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("founder@example.com", "founder")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(response).at("/user/email").asString()).isEqualTo("founder@example.com");
    }

    @Test
    void secondUserRegistersAsDeveloper() throws Exception {
        register("founder@example.com", "founder");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("dev@example.com", "developer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("DEVELOPER"));

        assertThat(userRepository.findByEmail("dev@example.com")).get()
                .extracting(u -> u.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    void passwordIsNeverStoredInPlaintext() throws Exception {
        register("founder@example.com", "founder");

        var stored = userRepository.findByEmail("founder@example.com").orElseThrow();
        assertThat(stored.getPasswordHash())
                .doesNotContain("Sup3rSecret!")
                .startsWith("$2");
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() throws Exception {
        register("founder@example.com", "founder");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("FOUNDER@example.com", "someone-else")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void rejectsWeakPasswordWithFieldLevelErrors() throws Exception {
        String payload = """
                {"email":"weak@example.com","username":"weakuser","password":"short","fullName":"Weak User"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists());
    }

    @Test
    void loginAcceptsEitherEmailOrUsername() throws Exception {
        register("founder@example.com", "founder");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"founder@example.com","password":"Sup3rSecret!"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"founder","password":"Sup3rSecret!"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordReturns401WithoutRevealingWhichFieldFailed() throws Exception {
        register("founder@example.com", "founder");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"founder","password":"WrongPassword1"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"ghost","password":"WrongPassword1"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void accessTokenGrantsAccessToProtectedEndpoint() throws Exception {
        JsonNode tokens = register("founder@example.com", "founder");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("founder"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void garbageTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTheTokenAndInvalidatesTheOldOne() throws Exception {
        JsonNode tokens = register("founder@example.com", "founder");
        String original = tokens.get("refreshToken").asString();

        String rotated = objectMapper.readTree(mockMvc.perform(post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", original))))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("refreshToken").asString();

        assertThat(rotated).isNotEqualTo(original);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", original))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reusingARevokedTokenRevokesTheWholeSessionFamily() throws Exception {
        JsonNode tokens = register("founder@example.com", "founder");
        String original = tokens.get("refreshToken").asString();

        String rotated = objectMapper.readTree(mockMvc.perform(post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", original))))
                        .andReturn().getResponse().getContentAsString())
                .get("refreshToken").asString();

        // Replaying the consumed token is treated as theft, so the successor dies too.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", original))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", rotated))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        JsonNode tokens = register("founder@example.com", "founder");
        String refreshToken = tokens.get("refreshToken").asString();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode register(String email, String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String registerPayload(String email, String username) {
        return """
                {"email":"%s","username":"%s","password":"Sup3rSecret!","fullName":"Test User"}
                """.formatted(email, username);
    }
}
