package com.yocabs.api.modules.travelpartner.application.command;

import java.util.UUID;

public record UpdateServiceAreaCommand(
        UUID travelPartnerId,
        UUID serviceAreaId,
        String name,
        double latitude,
        double longitude,
        double radiusKm
) {
}