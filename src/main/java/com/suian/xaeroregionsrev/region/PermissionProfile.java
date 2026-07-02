package com.suian.xaeroregionsrev.region;

public record PermissionProfile(boolean operator, boolean creative) {
    public boolean canManageRegions() {
        return operator && creative;
    }
}
