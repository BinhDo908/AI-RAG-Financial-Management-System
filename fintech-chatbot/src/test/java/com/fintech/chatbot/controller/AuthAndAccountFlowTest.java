package com.fintech.chatbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử end-to-end qua HTTP cho UC-D01, D02, D03, C05, C06 (H2 + Flyway + Spring Security thật).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndAccountFlowTest {

    private static final String PASSWORD = "Secret123";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenAccessProfileThenLogoutInvalidatesToken() throws Exception {
        String email = uniqueEmail();
        String token = register(email);

        mvc.perform(get("/api/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("USER"));

        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void registerRejectsDuplicateEmailAndWeakPassword() throws Exception {
        String email = uniqueEmail();
        register(email);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(email, PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(uniqueEmail(), "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void fiveWrongPasswordsLockLoginTemporarily() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int i = 0; i < 4; i++) {
            login(email, "WrongPass1").andExpect(status().isUnauthorized());
        }
        login(email, "WrongPass1").andExpect(status().isTooManyRequests());
        // Kể cả mật khẩu đúng cũng bị chặn trong thời gian khóa
        login(email, PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void changePasswordInvalidatesOldTokenAndAcceptsNewPassword() throws Exception {
        String email = uniqueEmail();
        String token = register(email);

        mvc.perform(put("/api/me/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD,
                                "newPassword", "NewSecret456", "confirmPassword", "NewSecret456"))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        login(email, "NewSecret456").andExpect(status().isOk());
    }

    @Test
    void exportContainsDefaultWalletAndDeleteRemovesAccount() throws Exception {
        String email = uniqueEmail();
        String token = register(email);

        MvcResult export = mvc.perform(get("/api/me/export").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        List<String> entries = new ArrayList<>();
        String walletsJson = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(export.getResponse().getContentAsByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
                if (entry.getName().equals("wallets.json")) {
                    walletsJson = new String(zip.readAllBytes());
                }
            }
        }
        assertThat(entries).contains("profile.json", "wallets.json", "transactions.json", "messages.json");
        assertThat(walletsJson).contains("Tiền mặt").doesNotContain("passwordHash");

        mvc.perform(delete("/api/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", PASSWORD, "confirmation", "sai"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DELETE_CONFIRMATION_INVALID"));

        mvc.perform(delete("/api/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", PASSWORD, "confirmation", "XOA TAI KHOAN"))))
                .andExpect(status().isOk());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        String userToken = register(uniqueEmail());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanLockUserAndLockedUserLosesAccess() throws Exception {
        String adminToken = tokenOf(login("admin@test.local", "AdminPass123").andExpect(status().isOk()).andReturn());
        String email = uniqueEmail();
        String userToken = register(email);
        long userId = objectMapper.readTree(
                        mvc.perform(get("/api/me").header("Authorization", bearer(userToken))).andReturn()
                                .getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mvc.perform(get("/api/admin/users").param("keyword", email).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1));

        mvc.perform(post("/api/admin/users/{id}/lock", userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        mvc.perform(get("/api/me").header("Authorization", bearer(userToken))).andExpect(status().isUnauthorized());
        login(email, PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));

        mvc.perform(post("/api/admin/users/{id}/unlock", userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void adminManagesDefaultCategories() throws Exception {
        String adminToken = tokenOf(login("admin@test.local", "AdminPass123").andReturn());
        String name = "Thú cưng " + UUID.randomUUID().toString().substring(0, 6);

        MvcResult created = mvc.perform(post("/api/admin/categories").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "type", "EXPENSE", "icon", "paw"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.defaultCategory").value(true))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();

        mvc.perform(post("/api/admin/categories").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name.toUpperCase(), "type", "EXPENSE"))))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/admin/categories/{id}/hide", id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true));

        mvc.perform(delete("/api/admin/categories/{id}", id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private String register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(email, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
        return tokenOf(result);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private String tokenOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    private static Map<String, Object> registerBody(String email, String password) {
        return Map.of("fullName", "Người Dùng Test", "email", email,
                "password", password, "confirmPassword", password, "acceptTerms", true);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
