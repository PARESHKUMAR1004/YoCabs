package com.yocabs.api.modules.travelpartner.application.command;

import java.util.UUID;

public record RemoveServiceAreaCommand(
        UUID travelPartnerId,
        UUID serviceAreaId
) {
}