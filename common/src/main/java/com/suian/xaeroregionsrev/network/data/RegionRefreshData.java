package com.suian.xaeroregionsrev.network.data;

import com.suian.xaeroregionsrev.network.buffer.PacketBuffer;

public record RegionRefreshData() {
    public static void encode(PacketBuffer buffer, RegionRefreshData data) {
    }

    public static RegionRefreshData decode(PacketBuffer buffer) {
        return new RegionRefreshData();
    }
}
