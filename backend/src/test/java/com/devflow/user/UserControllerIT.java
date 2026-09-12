package com.devflow.user;

import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.user.entity.Role;
import com.devflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestAccounts accounts;

    @BeforeEach
    void reset() {
        accounts.deleteEverything();
    }

    @Test
    void adminCanChangeAnotherUsersRole() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var target = accounts.register("teammate", Role.DEVELOPER);

        mockMvc.perform(put("/api/users/" + target.id())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Teammate","role":"VIEWER","active":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        assertThat(userRepository.findById(target.id()).orElseThrow().getRole()).isEqualTo(Role.VIEWER);
    }

    @Test
    void developerCannotChangeRoles() throws Exception {
        accounts.registerAdmin("boss");
        var developer = accounts.register("dev", Role.DEVELOPER);
        var target = accounts.register("victim", Role.VIEWER);

        mockMvc.perform(put("/api/users/" + target.id())
                        .header("Authorization", "Bearer " + developer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Victim","role":"ADMIN","active":true}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));

        assertThat(userRepository.findById(target.id()).orElseThrow().getRole()).isEqualTo(Role.VIEWER);
    }

    @Test
    void viewerCannotChangeRoles() throws Exception {
        accounts.registerAdmin("boss");
        var viewer = accounts.register("readonly", Role.VIEWER);
        var target = accounts.register("victim", Role.VIEWER);

        mockMvc.perform(put("/api/users/" + target.id())
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Victim","role":"ADMIN","active":true}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotStripTheirOwnAdminRights() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var admin = userRepository.findByUsername("boss").orElseThrow();

        mockMvc.perform(put("/api/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Boss","role":"DEVELOPER","active":true}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("You cannot remove your own administrator access"));

        assertThat(userRepository.findByUsername("boss").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void adminCannotDeactivateThemselves() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var admin = userRepository.findByUsername("boss").orElseThrow();

        mockMvc.perform(put("/api/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Boss","role":"ADMIN","active":false}"""))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void anyAuthenticatedUserCanListAccounts() throws Exception {
        accounts.registerAdmin("boss");
        var viewer = accounts.register("readonly", Role.VIEWER);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + viewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void listingSupportsFilteringAndPagination() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        accounts.register("alice", Role.DEVELOPER);
        accounts.register("bob", Role.VIEWER);

        mockMvc.perform(get("/api/users?role=VIEWER").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("bob"));

        mockMvc.perform(get("/api/users?q=ali").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/users?size=2&page=0").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void userCanChangeOwnPasswordAndOldOneStopsWorking() throws Exception {
        String token = accounts.registerAdmin("boss");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Sup3rSecret!","newPassword":"Ev3nBetterPass!"}"""))
                .andExpect(status().isNoContent());

        accounts.expectLoginFails("boss", "Sup3rSecret!");
        accounts.expectLoginSucceeds("boss", "Ev3nBetterPass!");
    }

    @Test
    void changingPasswordWithWrongCurrentPasswordIsRejected() throws Exception {
        String token = accounts.registerAdmin("boss");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"NotMyPassword1","newPassword":"Ev3nBetterPass!"}"""))
                .andExpect(status().isUnauthorized());
    }
}
