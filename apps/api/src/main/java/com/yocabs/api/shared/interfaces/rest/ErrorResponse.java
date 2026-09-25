package com.yocabs.api.shared.interfaces.rest;

/** Uniform API error body. */
public record ErrorResponse(
        String code,
        String message,
        String correlationId
) {
}
