package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileAndVehicleDetailsIntegrationTest extends MarketplaceFixtures {

    private String searchBody() {
        return "{\"pickup\":{\"description\":\"A\",\"latitude\":20.2444,\"longitude\":85.8178},"
                + "\"destination\":{\"description\":\"B\",\"latitude\":19.8135,\"longitude\":85.8312},"
                + "\"startDate\":\"" + today() + "\",\"endDate\":\"" + today() + "\",\"passengerCount\":4,"
                + "\"tripType\":\"CHAUFFEUR_ONE_WAY\"}";
    }

    private Object searchOption(UUID vehicleId, String field) throws Exception {
        MvcResult search = post("/api/v1/trip-search", null, searchBody());
        List<?> matches = json(search, "$.options[?(@.vehicleId=='" + vehicleId + "')]." + field);
        return matches.isEmpty() ? null : matches.getFirst();
    }

    // ---- profile -------------------------------------------------------------------------

    @Test
    void aUserCanReadAndUpdateTheirOwnProfile() throws Exception {
        String mobile = randomMobile();
        String tourist = otpLogin(mobile);

        MvcResult initial = get("/api/v1/profile", tourist);
        assertEquals("TOURIST", json(initial, "$.role"));
        assertEquals("+91" + mobile, json(initial, "$.mobile"));
        assertNull((Object) json(initial, "$.displayName"));

        String email = "asha-" + UUID.randomUUID() + "@example.com";
        MvcResult updated = perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/profile")
                        .header("Authorization", "Bearer " + tourist)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  Asha Das  \",\"email\":\"" + email.toUpperCase()
                                + "\",\"preferredLanguage\":\"OR\"}"));

        assertEquals(200, status(updated));
        assertEquals("Asha Das", json(updated, "$.displayName"));
        assertEquals(email, json(updated, "$.email"));
        assertEquals("or", json(updated, "$.preferredLanguage"));

        assertEquals("Asha Das", json(get("/api/v1/profile", tourist), "$.displayName"));
    }

    @Test
    void profileUpdatesAreValidatedAndEmailsAreUnique() throws Exception {
        String first = touristToken();
        String second = touristToken();
        String email = "shared-" + UUID.randomUUID() + "@example.com";

        assertEquals(200, status(put("/api/v1/profile", first, "{\"email\":\"" + email + "\"}")));

        assertEquals(409, status(put("/api/v1/profile", second, "{\"email\":\"" + email + "\"}")));
        assertEquals(400, status(put("/api/v1/profile", second, "{\"email\":\"not-an-email\"}")));
        assertEquals(400, status(put("/api/v1/profile", second, "{\"preferredLanguage\":\"english\"}")));
        assertEquals(400, status(put("/api/v1/profile", second, "{\"displayName\":\"   \"}")));

        // An empty email clears it and frees it for someone else.
        assertEquals(200, status(put("/api/v1/profile", first, "{\"email\":\"\"}")));
        assertNull((Object) json(get("/api/v1/profile", first), "$.email"));
        assertEquals(200, status(put("/api/v1/profile", second, "{\"email\":\"" + email + "\"}")));
    }

    @Test
    void anAdminsLoginEmailCannotBeChangedFromTheProfile() throws Exception {
        String admin = adminToken();

        assertEquals(400, status(put("/api/v1/profile", admin, "{\"email\":\"someone-else@example.com\"}")));
        assertEquals(200, status(put("/api/v1/profile", admin, "{\"displayName\":\"Root Admin\"}")));
        assertEquals(ADMIN_EMAIL, json(get("/api/v1/profile", admin), "$.email"));
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        assertEquals(401, status(get("/api/v1/profile", null)));
    }

    // ---- vehicle details & facilities ----------------------------------------------------

    @Test
    void partnersDescribeTheirVehiclesAndTouristsSeeItInSearch() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String path = "/api/v1/travel-partners/" + partner.id() + "/vehicles/" + partner.vehicleId() + "/profile";

        // The catalogue is seeded and readable by any signed-in user.
        MvcResult catalogue = get("/api/v1/facilities", partner.ownerToken());
        assertEquals(200, status(catalogue));
        assertTrue(body(catalogue).contains("MUSIC_SYSTEM"));

        assertEquals(200, status(get(path, partner.ownerToken())));

        MvcResult updated =
                put(path, partner.ownerToken(),
                        "{\"fuelType\":\"DIESEL\",\"transmission\":\"MANUAL\",\"modelYear\":2022,"
                                + "\"luggageCapacity\":4,\"facilityCodes\":[\"AC\",\"music_system\",\"WATER_BOTTLE\"]}");
        assertEquals(200, status(updated));
        assertEquals("DIESEL", json(updated, "$.fuelType"));
        assertEquals(3, ((List<?>) json(updated, "$.facilities")).size());

        // Search shows the same details to tourists (no login needed).
        assertEquals("DIESEL", searchOption(partner.vehicleId(), "fuelType"));
        assertEquals("MANUAL", searchOption(partner.vehicleId(), "transmission"));
        assertEquals(2022, ((Number) searchOption(partner.vehicleId(), "modelYear")).intValue());
        MvcResult search = post("/api/v1/trip-search", null, searchBody());
        List<?> codes = json(search, "$.options[?(@.vehicleId=='" + partner.vehicleId() + "')].facilities[*].code");
        assertEquals(List.of("AC", "MUSIC_SYSTEM", "WATER_BOTTLE").stream().sorted().toList(),
                codes.stream().map(Object::toString).sorted().toList());

        // Details replace as a whole; facilities are untouched when omitted.
        MvcResult keptFacilities = put(path, partner.ownerToken(), "{\"fuelType\":\"CNG\"}");
        assertEquals("CNG", json(keptFacilities, "$.fuelType"));
        assertNull((Object) json(keptFacilities, "$.transmission"));
        assertEquals(3, ((List<?>) json(keptFacilities, "$.facilities")).size());

        MvcResult cleared = put(path, partner.ownerToken(), "{\"fuelType\":\"CNG\",\"facilityCodes\":[]}");
        assertEquals(0, ((List<?>) json(cleared, "$.facilities")).size());
    }

    @Test
    void vehicleDetailsAreValidatedAndOwnershipEnforced() throws Exception {
        Partner partner = createActivePartner("SEDAN", 4);
        Partner other = createActivePartner("SEDAN", 4);
        String path = "/api/v1/travel-partners/" + partner.id() + "/vehicles/" + partner.vehicleId() + "/profile";

        assertEquals(400, status(put(path, partner.ownerToken(), "{\"facilityCodes\":[\"NOT_A_FACILITY\"]}")));
        assertEquals(400, status(put(path, partner.ownerToken(), "{\"modelYear\":1850}")));
        assertEquals(400, status(put(path, partner.ownerToken(), "{\"luggageCapacity\":900}")));
        assertEquals(400, status(put(path, partner.ownerToken(), "{\"fuelType\":\"STEAM\"}")));

        assertEquals(403, status(put(path, other.ownerToken(), "{\"fuelType\":\"DIESEL\"}")));
        assertEquals(403, status(get(path, touristToken())));

        // A vehicle addressed under the wrong partner does not exist for the caller.
        assertEquals(404, status(get(
                "/api/v1/travel-partners/" + other.id() + "/vehicles/" + partner.vehicleId() + "/profile",
                other.ownerToken())));
    }

    @Test
    void adminsManageTheFacilityCatalogue() throws Exception {
        String admin = adminToken();
        Partner partner = createActivePartner("SUV", 7);
        String path = "/api/v1/travel-partners/" + partner.id() + "/vehicles/" + partner.vehicleId() + "/profile";
        String code = "WIFI_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase().replace("-", "");

        assertEquals(403, status(post("/api/v1/admin/facilities", partner.ownerToken(),
                "{\"code\":\"" + code + "\",\"name\":\"Wi-Fi\"}")));
        assertEquals(400, status(post("/api/v1/admin/facilities", admin, "{\"code\":\"bad code\",\"name\":\"X\"}")));

        assertEquals(201, status(post("/api/v1/admin/facilities", admin,
                "{\"code\":\"" + code + "\",\"name\":\"Wi-Fi\"}")));
        assertEquals(409, status(post("/api/v1/admin/facilities", admin,
                "{\"code\":\"" + code + "\",\"name\":\"Wi-Fi\"}")));

        assertEquals(200, status(put(path, partner.ownerToken(), "{\"facilityCodes\":[\"" + code + "\"]}")));

        // A retired facility disappears from the catalogue, from new selections and from search.
        assertEquals(200, status(post("/api/v1/admin/facilities/" + code + "/deactivate", admin, "{}")));
        assertTrue(!body(get("/api/v1/facilities", partner.ownerToken())).contains(code));
        assertEquals(400, status(put(path, partner.ownerToken(), "{\"facilityCodes\":[\"" + code + "\"]}")));

        MvcResult search = post("/api/v1/trip-search", null, searchBody());
        assertEquals(0, ((List<?>) json(search,
                "$.options[?(@.vehicleId=='" + partner.vehicleId() + "')].facilities[?(@.code=='" + code + "')]")).size());
    }

    @Test
    void vehiclePhotosGoLiveOnUploadAndAnAdminCanTakeThemDown() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        Partner other = createActivePartner("SEDAN", 4);
        String admin = adminToken();

        // Only images, only for vehicles, only by the owner.
        assertEquals(400, status(uploadFileRaw(partner.ownerToken(), "VEHICLE", partner.vehicleId(),
                "VEHICLE_PHOTO", "front.pdf", "application/pdf", PDF_BYTES)));
        assertEquals(400, status(uploadFileRaw(partner.ownerToken(), "PARTNER", partner.id(),
                "VEHICLE_PHOTO", "front.png", "image/png", PNG_BYTES)));
        assertEquals(403, status(uploadFileRaw(other.ownerToken(), "VEHICLE", partner.vehicleId(),
                "VEHICLE_PHOTO", "front.png", "image/png", PNG_BYTES)));

        UUID photoId = uploadFile(partner.ownerToken(), "VEHICLE", partner.vehicleId(),
                "VEHICLE_PHOTO", "front.png", "image/png", PNG_BYTES);

        // Live straight away: the owner sees it, and so do tourists.
        String profilePath = "/api/v1/travel-partners/" + partner.id() + "/vehicles/" + partner.vehicleId() + "/profile";
        assertEquals("APPROVED", json(get(profilePath, partner.ownerToken()), "$.photos[0].status"));

        List<?> photos = (List<?>) searchOption(partner.vehicleId(), "photos");
        assertEquals(1, photos.size());
        assertEquals("/api/v1/vehicles/" + partner.vehicleId() + "/photos/" + photoId, photos.getFirst());

        MvcResult served = get("/api/v1/vehicles/" + partner.vehicleId() + "/photos/" + photoId, null);
        assertEquals(200, status(served));
        assertEquals("image/png", served.getResponse().getContentType());
        assertEquals(PNG_BYTES.length, served.getResponse().getContentAsByteArray().length);
        assertEquals("nosniff", served.getResponse().getHeader("X-Content-Type-Options"));

        // The URL is bound to the vehicle it was approved for.
        assertEquals(404, status(get("/api/v1/vehicles/" + other.vehicleId() + "/photos/" + photoId, null)));

        // A rejected photo is never public.
        UUID rejected = uploadFile(partner.ownerToken(), "VEHICLE", partner.vehicleId(),
                "VEHICLE_PHOTO", "blurry.png", "image/png", PNG_BYTES);
        post("/api/v1/admin/documents/" + rejected + "/reject", admin, "{\"reason\":\"Blurry\"}");
        assertEquals(404, status(get("/api/v1/vehicles/" + partner.vehicleId() + "/photos/" + rejected, null)));
        assertEquals(1, ((List<?>) searchOption(partner.vehicleId(), "photos")).size());

        // Other documents are never served through the public photo URL.
        UUID rcBook = uploadFile(partner.ownerToken(), "VEHICLE", partner.vehicleId(),
                "RC_BOOK", "rc.pdf", "application/pdf", PDF_BYTES);
        post("/api/v1/admin/documents/" + rcBook + "/approve", admin, "{}");
        assertEquals(404, status(get("/api/v1/vehicles/" + partner.vehicleId() + "/photos/" + rcBook, null)));

        // The owner can remove a photo; nobody else can, and legal documents are not removable.
        assertEquals(403, status(delete("/api/v1/documents/" + photoId, other.ownerToken())));
        assertEquals(409, status(delete("/api/v1/documents/" + rcBook, partner.ownerToken())));
        assertEquals(204, status(delete("/api/v1/documents/" + photoId, partner.ownerToken())));
        assertEquals(404, status(get("/api/v1/vehicles/" + partner.vehicleId() + "/photos/" + photoId, null)));
        assertEquals(0, ((List<?>) searchOption(partner.vehicleId(), "photos")).size());
    }

    private MvcResult put(String path, String token, String json) throws Exception {
        return perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json));
    }
}
