package com.yocabs.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardRateIntegrationTest extends com.yocabs.api.support.MarketplaceFixtures {

    @Test
    void onlyAnAdminCanSetAStandardRateAndAnyoneCanReadIt() throws Exception {
        String tourist = otpLogin(randomMobile());
        String admin = adminToken();

        // Nothing set yet for this category.
        List<?> before = json(get("/api/v1/standard-rates", null), "$[?(@.category=='SUV')]");
        assertTrue(before.isEmpty());

        assertEquals(403,
                status(put("/api/v1/admin/standard-rates/SUV", tourist, "{\"perKmRate\":18.50}")));

        MvcResult set = put("/api/v1/admin/standard-rates/SUV", admin, "{\"perKmRate\":18.50}");
        assertEquals(200, status(set));
        assertEquals("SUV", json(set, "$.category"));
        assertEquals(0, new java.math.BigDecimal("18.50").compareTo(decimal(set, "$.perKmRate")));

        // Public, no sign-in needed, and reflects the update.
        MvcResult listed = get("/api/v1/standard-rates", null);
        assertEquals(200, status(listed));
        List<?> mine = json(listed, "$[?(@.category=='SUV')]");
        assertEquals(1, mine.size());

        // Setting it again for the same category replaces it rather than adding a row.
        put("/api/v1/admin/standard-rates/SUV", admin, "{\"perKmRate\":20}");
        assertEquals(1, ((List<?>) json(get("/api/v1/standard-rates", null), "$[?(@.category=='SUV')]")).size());

        assertEquals(400, status(put("/api/v1/admin/standard-rates/SUV", admin, "{\"perKmRate\":0}")));
        assertEquals(400, status(put("/api/v1/admin/standard-rates/SUV", admin, "{}")));
    }

    @Test
    void aSearchShowsTheStandardFareOnlyForCategoriesAnAdminHasPriced() throws Exception {
        String admin = adminToken();
        put("/api/v1/admin/standard-rates/SEDAN", admin, "{\"perKmRate\":15}");

        Partner sedan = createActivePartner("SEDAN", 4);
        // MUV is never priced by the other test in this class, so it reliably has no benchmark.
        Partner unpriced = createActivePartner("MUV", 7);

        MvcResult search = post("/api/v1/trip-search", null,
                "{\"pickup\":{\"description\":\"A\",\"latitude\":20.2444,\"longitude\":85.8178},"
                        + "\"destination\":{\"description\":\"B\",\"latitude\":19.8135,\"longitude\":85.8312},"
                        + "\"startDate\":\"" + today() + "\",\"endDate\":\"" + today() + "\",\"passengerCount\":4,"
                        + "\"tripType\":\"CHAUFFEUR_ONE_WAY\"}");
        assertEquals(200, status(search));

        var distanceKm = decimal(search, "$.distanceKm");

        List<?> sedanMatches =
                json(search, "$.options[?(@.vehicleId=='" + sedan.vehicleId() + "')].standardAmount");
        assertEquals(1, sedanMatches.size());
        assertEquals(0,
                distanceKm.multiply(new java.math.BigDecimal("15"))
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                        .compareTo(new java.math.BigDecimal(String.valueOf(sedanMatches.getFirst()))));

        List<?> unpricedMatches =
                json(search, "$.options[?(@.vehicleId=='" + unpriced.vehicleId() + "')].standardAmount");
        assertEquals(1, unpricedMatches.size());
        assertEquals(null, unpricedMatches.getFirst());
    }

    private MvcResult put(String path, String token, String json) throws Exception {
        return perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                        .header("Authorization", token == null ? "" : "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json));
    }
}
