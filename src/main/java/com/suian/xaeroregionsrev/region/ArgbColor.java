package com.suian.xaeroregionsrev.region;

public record ArgbColor(int value) {
    public int alpha() {
        return (value >>> 24) & 0xFF;
    }
}
