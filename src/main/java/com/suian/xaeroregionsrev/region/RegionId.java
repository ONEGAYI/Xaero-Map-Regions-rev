package com.suian.xaeroregionsrev.region;

import java.util.Locale;

public record RegionId(String value) {
    public RegionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Region id cannot be blank.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
