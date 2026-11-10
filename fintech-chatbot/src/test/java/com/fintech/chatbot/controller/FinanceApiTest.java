package com.fintech.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra lớp HTTP của giai đoạn 2: validation, mã lỗi JSON, định dạng phản hồi.
 * Logic nghiệp vụ chi tiết nằm ở FinanceFlowTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private long walletId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "api-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Api Test", "email", email, "password", "Secret123",
                                "confirmPassword", "Secret123", "acceptTerms", true))))
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + objectMapper.readTree(body).path("data").path("accessToken").asText();
        String wallets = call(get("/api/wallets")).andReturn().getResponse().getContentAsString();
        walletId = objectMapper.readTree(wallets).path("data").path("wallets").get(0).path("id").asLong();
    }

    @Test
    void transactionValidationReturnsFieldErrors() throws Exception {
        call(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("type", "EXPENSE", "amount", 0, "walletId", walletId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.amount").exists());

        call(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("type", "EXPENSE", "amount", 50000, "walletId", walletId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_REQUIRED"));
    }

    @Test
    void walletListAndReportsRespond() throws Exception {
        call(get("/api/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wallets[0].name").value("Tiền mặt"))
                .andExpect(jsonPath("$.data.totalBalance").value(0));

        call(get("/api/reports/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyTrend.length()").value(6));

        call(get("/api/budgets").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_MONTH"));
    }

    @Test
    void exportWithoutDataReturns404AndCalculatorWorks() throws Exception {
        call(get("/api/transactions/export").param("from", "2020-01-01").param("to", "2020-01-31")
                .param("format", "CSV"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NO_DATA_TO_EXPORT"));

        call(post("/api/tools/loan/annuity").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("principal", 100_000_000, "annualRatePercent", 12, "months", 12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstPayment").value(8884879))
                .andExpect(jsonPath("$.data.schedule.length()").value(12));

        call(post("/api/tools/loan/annuity").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("principal", 1000, "annualRatePercent", 150, "months", 12))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aiPermissionsDefaultToAllowedAndCanBeChanged() throws Exception {
        call(get("/api/me/ai-permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowTransactions").value(true));

        call(put("/api/me/ai-permissions").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("allowTransactions", false, "allowBudgets", true, "allowDebts", false,
                        "allowGoals", true, "allowProfile", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowTransactions").value(false))
                .andExpect(jsonPath("$.data.allowDebts").value(false));
    }

    @Test
    void conversationsCrud() throws Exception {
        String created = call(post("/api/conversations")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Hội thoại mới"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").path("id").asLong();

        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/conversations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("title", "Kế hoạch tiết kiệm"))))
                .andExpect(status().isOk());

        call(get("/api/conversations").param("keyword", "tiết kiệm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1));

        call(get("/api/conversations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(0));
    }

    @Test
    void excelExportHasAttachmentHeader() throws Exception {
        long foodId = objectMapper.readTree(call(get("/api/categories").param("type", "EXPENSE"))
                .andReturn().getResponse().getContentAsString()).path("data").get(0).path("id").asLong();
        call(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("type", "EXPENSE", "amount", 50000, "walletId", walletId,
                        "categoryId", foodId, "txnDate", "2026-10-01"))))
                .andExpect(status().isCreated());

        call(get("/api/transactions/export").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")));
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.header("Authorization", token));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
