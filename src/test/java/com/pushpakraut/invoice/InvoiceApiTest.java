package com.pushpakraut.invoice;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.jwt.secret=test-only-secret-0123456789abcdef0123", "app.auth-attempts-per-ip=8"})
@AutoConfigureMockMvc
class InvoiceApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    @Test
    void protectedRoutesNeedToken() throws Exception {
        mvc.perform(get("/api/invoices")).andExpect(status().isUnauthorized());
    }

    @Test
    void fullInvoiceFlowAndTotals() throws Exception {
        String token = register("alice@example.com");

        String customer = mvc.perform(post("/api/customers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme Ltd\",\"email\":\"billing@acme.test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long customerId = json.readTree(customer).get("id").asLong();

        String invoice = mvc.perform(post("/api/invoices").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "dueDate": "2026-12-31",
                                 "items": [{"description":"API work","quantity":10,"unitPrice":20.00},
                                           {"description":"Hosting","quantity":1,"unitPrice":5.50}]}
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(205.50))
                .andReturn().getResponse().getContentAsString();
        long invoiceId = json.readTree(invoice).get("id").asLong();

        mvc.perform(patch("/api/invoices/" + invoiceId + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(jsonPath("$.status").value("PAID"));

        mvc.perform(get("/api/invoices/summary").header("Authorization", token))
                .andExpect(jsonPath("$.paid").value(205.50))
                .andExpect(jsonPath("$.outstanding").value(0));

        mvc.perform(delete("/api/customers/" + customerId).header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Customer has invoices. Delete them first."));

        // Another user must not see Alice's invoice
        String bob = register("bob@example.com");
        mvc.perform(get("/api/invoices/" + invoiceId).header("Authorization", bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongPasswordRejected() throws Exception {
        register("carol@example.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"carol@example.com\",\"password\":\"wrongpass1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void errorsKeepTheirRealStatus() throws Exception {
        String token = register("dave@example.com");
        mvc.perform(get("/api/invoices/999").header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/invoices").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dave@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void loginAttemptsAreRateLimitedPerIp() throws Exception {
        int last = 0;
        for (int i = 0; i < 9; i++) {
            last = mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nobody@example.com\",\"password\":\"guess12345\"}"))
                    .andReturn().getResponse().getStatus();
        }
        org.junit.jupiter.api.Assertions.assertEquals(429, last);
    }

    @Test
    void absurdPricesAreRejected() throws Exception {
        String token = register("erin@example.com");
        mvc.perform(post("/api/invoices").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"dueDate\":\"2026-12-31\",\"items\":[{\"description\":\"x\",\"quantity\":1,\"unitPrice\":1e30}]}"))
                .andExpect(status().isBadRequest());
    }
}
