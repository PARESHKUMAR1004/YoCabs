package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExploreIntegrationTest extends MarketplaceFixtures {

    private static final String BHUBANESWAR = "latitude=20.2961&longitude=85.8245";
    private static final String DELHI = "latitude=28.6139&longitude=77.2090";

    @Test
    void partnersWorkingAroundAPointAreListedAndOthersAreNot() throws Exception {
        Partner partner = createActivePartner("SUV", 7);

        // Public: no sign-in needed to look around.
        MvcResult near = get("/api/v1/explore/partners?" + BHUBANESWAR, null);
        assertEquals(200, status(near));

        List<?> mine = json(near, "$[?(@.id=='" + partner.id() + "')]");
        assertEquals(1, mine.size());
        Map<?, ?> card = (Map<?, ?>) mine.getFirst();
        assertEquals(1, ((Number) card.get("vehicleCount")).intValue());
        assertEquals("SUV", ((List<?>) card.get("categories")).getFirst());

        // Nobody works around Delhi.
        List<?> far = json(get("/api/v1/explore/partners?" + DELHI, null), "$[?(@.id=='" + partner.id() + "')]");
        assertTrue(far.isEmpty());
    }

    @Test
    void aPartnersVehiclesShowTheirDetailsAndPhotos() throws Exception {
        Partner partner = createActivePartner("SEDAN", 4);

        UUID photo = uploadFile(partner.ownerToken(), "VEHICLE", partner.vehicleId(),
                "VEHICLE_PHOTO", "front.png", "image/png", PNG_BYTES);

        MvcResult detail = get("/api/v1/explore/partners/" + partner.id() + "?" + BHUBANESWAR, null);
        assertEquals(200, status(detail));
        assertEquals(partner.id().toString(), json(detail, "$.partner.id"));
        assertEquals(1, ((List<?>) json(detail, "$.vehicles")).size());
        assertEquals(partner.vehicleId().toString(), json(detail, "$.vehicles[0].id"));
        assertEquals(4, ((Number) json(detail, "$.vehicles[0].passengerCapacity")).intValue());

        String path = "/api/v1/vehicles/" + partner.vehicleId() + "/photos/" + photo;
        assertEquals(path, json(detail, "$.vehicles[0].photos[0]"));
        // The partner's card borrows a vehicle's photo.
        assertEquals(path, json(detail, "$.partner.coverPhoto"));
    }

    @Test
    void aPartnerWhoDoesNotOperateThereCannotBeOpenedFromThere() throws Exception {
        Partner partner = createActivePartner("SUV", 7);

        assertEquals(404, status(get("/api/v1/explore/partners/" + partner.id() + "?" + DELHI, null)));
        assertEquals(400, status(get("/api/v1/explore/partners?latitude=120&longitude=85", null)));
    }
}
