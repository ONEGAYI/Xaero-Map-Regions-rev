package com.suian.xaeroregionsrev.network.buffer;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Forge 平台的 {@link PacketBuffer} 适配实现，
 * 将平台无关接口委托给 {@link FriendlyByteBuf}。
 * Forge 1.20.1 的 FriendlyByteBuf 与 1.21.1 所用方法签名一致。
 */
public final class FriendlyByteBufPacketBuffer implements PacketBuffer {
    private final FriendlyByteBuf buf;

    public FriendlyByteBufPacketBuffer(FriendlyByteBuf buf) {
        this.buf = buf;
    }

    public FriendlyByteBuf buf() {
        return buf;
    }

    @Override
    public void writeUtf(String value, int maxLength) {
        buf.writeUtf(value, maxLength);
    }

    @Override
    public String readUtf(int maxLength) {
        return buf.readUtf(maxLength);
    }

    @Override
    public void writeInt(int value) {
        buf.writeInt(value);
    }

    @Override
    public int readInt() {
        return buf.readInt();
    }

    @Override
    public void writeLong(long value) {
        buf.writeLong(value);
    }

    @Override
    public long readLong() {
        return buf.readLong();
    }

    @Override
    public void writeBoolean(boolean value) {
        buf.writeBoolean(value);
    }

    @Override
    public boolean readBoolean() {
        return buf.readBoolean();
    }

    @Override
    public void writeVarInt(int value) {
        buf.writeVarInt(value);
    }

    @Override
    public int readVarInt() {
        return buf.readVarInt();
    }
}
