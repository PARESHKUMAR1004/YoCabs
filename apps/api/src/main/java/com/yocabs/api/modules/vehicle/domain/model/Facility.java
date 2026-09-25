package com.yocabs.api.modules.vehicle.domain.model;

import java.util.regex.Pattern;

/** A catalogue entry vehicles can advertise (admin-managed data). */
public record Facility(String code, String name, boolean active) {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");

    public Facility {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Facility code must be 2-40 characters: capital letters, digits and underscores"
            );
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Facility name is required (max 100 characters)");
        }
        name = name.trim();
    }

    public Facility withActive(boolean active) {
        return new Facility(code, name, active);
    }
}
