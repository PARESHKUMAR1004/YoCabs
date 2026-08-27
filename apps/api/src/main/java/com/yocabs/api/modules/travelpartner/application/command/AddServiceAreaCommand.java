package com.yocabs.api.modules.travelpartner.application.command;

import java.util.UUID;

public record AddServiceAreaCommand(
        UUID travelPartnerId,
        String name,
        double latitude,
        double longitude,
        double radiusKm
) {
}