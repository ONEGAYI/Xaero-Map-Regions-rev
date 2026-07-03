package com.suian.xaeroregionsrev.region;

import java.util.Locale;
import java.util.Objects;

public final class RegionColorParser {
    private RegionColorParser() {
    }

    public static ArgbColor parse(String text) {
        Objects.requireNonNull(text, "Color text cannot be null.");
        String valueText = text.trim();
        if (valueText.startsWith("0x") || valueText.startsWith("0X")) {
            valueText = valueText.substring(2);
        } else if (valueText.startsWith("#")) {
            valueText = valueText.substring(1);
        }
        if (valueText.isBlank()) {
            throw new IllegalArgumentException("Color must not be blank.");
        }
        if (valueText.length() != 6 && valueText.length() != 8) {
            throw new IllegalArgumentException("Color must contain 6 or 8 hexadecimal digits.");
        }
        for (int i = 0; i < valueText.length(); i++) {
            if (Character.digit(valueText.charAt(i), 16) < 0) {
                throw new IllegalArgumentException("Color must contain only hexadecimal digits.");
            }
        }
        long value = Long.parseUnsignedLong(valueText, 16);
        if (valueText.length() == 6) {
            value |= 0xFF000000L;
        }
        return new ArgbColor((int) value);
    }

    public static String format(ArgbColor color) {
        Objects.requireNonNull(color, "Color cannot be null.");
        return String.format(Locale.ROOT, "#%08X", color.value());
    }
}
