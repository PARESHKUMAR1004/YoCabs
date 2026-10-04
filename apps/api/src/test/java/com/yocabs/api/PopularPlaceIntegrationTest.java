package com.yocabs.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopularPlaceIntegrationTest extends com.yocabs.api.support.MarketplaceFixtures {

    @Test
    void anyoneCanListThemButOnlyAnAdminCanCurateThem() throws Exception {
        String tourist = otpLogin(randomMobile());
        String admin = adminToken();

        // No sign-in needed to read the list, and the seeded places from the migration are there.
        MvcResult listed = get("/api/v1/popular-places", null);
        assertEquals(200, status(listed));
        assertTrue(((List<?>) json(listed, "$[*].name")).contains("Puri"));

        assertEquals(403, status(post("/api/v1/admin/popular-places", tourist,
                "{\"name\":\"Rock Garden\",\"subtitle\":\"Rourkela\",\"latitude\":22.26,\"longitude\":84.85,\"displayOrder\":9}")));

        MvcResult created = post("/api/v1/admin/popular-places", admin,
                "{\"name\":\"Rock Garden\",\"subtitle\":\"Rourkela\",\"latitude\":22.26,\"longitude\":84.85,\"displayOrder\":9}");
        assertEquals(200, status(created));
        String id = json(created, "$.id").toString();
        assertEquals("Rock Garden", json(created, "$.name"));

        MvcResult afterCreate = get("/api/v1/popular-places", null);
        assertTrue(((List<?>) json(afterCreate, "$[*].name")).contains("Rock Garden"));

        MvcResult updated = put("/api/v1/admin/popular-places/" + id, admin,
                "{\"name\":\"Rock Garden Renamed\",\"subtitle\":\"Rourkela\",\"latitude\":22.26,\"longitude\":84.85,\"displayOrder\":9}");
        assertEquals(200, status(updated));
        assertEquals("Rock Garden Renamed", json(updated, "$.name"));

        assertEquals(403, status(delete("/api/v1/admin/popular-places/" + id, tourist)));
        assertEquals(200, status(delete("/api/v1/admin/popular-places/" + id, admin)));

        MvcResult afterDelete = get("/api/v1/popular-places", null);
        assertTrue(!((List<?>) json(afterDelete, "$[*].name")).contains("Rock Garden Renamed"));

        assertEquals(400, status(post("/api/v1/admin/popular-places", admin,
                "{\"subtitle\":\"No name\",\"latitude\":22.26,\"longitude\":84.85}")));

        assertEquals(400, status(put("/api/v1/admin/popular-places/" + UUID.randomUUID(), admin,
                "{\"name\":\"Ghost\",\"latitude\":22.26,\"longitude\":84.85}")));
    }

    private MvcResult put(String path, String token, String json) throws Exception {
        return perform(
                MockMvcRequestBuilders.put(path)
                        .header("Authorization", token == null ? "" : "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }
}
