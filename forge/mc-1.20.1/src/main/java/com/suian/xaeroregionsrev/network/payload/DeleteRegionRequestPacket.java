package com.suian.xaeroregionsrev.network.payload;

import com.suian.xaeroregionsrev.network.buffer.FriendlyByteBufPacketBuffer;
import com.suian.xaeroregionsrev.network.data.DeleteRegionRequestData;
import com.suian.xaeroregionsrev.region.RegionId;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

/**
 * 包装 {@link DeleteRegionRequestData} 的 Forge 负载。
 * 编解码逻辑委托给公共模块，这里仅保留 SimpleChannel 所需的 encoder/decoder。
 */
public final class DeleteRegionRequestPacket {
    private final DeleteRegionRequestData data;

    public DeleteRegionRequestPacket(String idText) {
        this(new DeleteRegionRequestData(idText));
    }

    public DeleteRegionRequestPacket(RegionId id) {
        this(new DeleteRegionRequestData(id));
    }

    public DeleteRegionRequestPacket(DeleteRegionRequestData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public String idText() {
        return data.idText();
    }

    public RegionId id() {
        return data.id();
    }

    public DeleteRegionRequestData data() {
        return data;
    }

    public static void encode(DeleteRegionRequestPacket packet, FriendlyByteBuf buffer) {
        DeleteRegionRequestData.encode(new FriendlyByteBufPacketBuffer(buffer), packet.data);
    }

    public static DeleteRegionRequestPacket decode(FriendlyByteBuf buffer) {
        FriendlyByteBufPacketBuffer packetBuffer = new FriendlyByteBufPacketBuffer(buffer);
        return new DeleteRegionRequestPacket(DeleteRegionRequestData.decode(packetBuffer));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof DeleteRegionRequestPacket that && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "DeleteRegionRequestPacket" + data;
    }
}
