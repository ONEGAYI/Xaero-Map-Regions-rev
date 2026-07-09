package com.suian.xaeroregionsrev.network.buffer;

/**
 * 平台无关的网络缓冲区接口，抽象 FriendlyByteBuf 的 payload 编解码方法子集。
 * 各平台子项目提供适配实现（如 FriendlyByteBufPacketBuffer）。
 */
public interface PacketBuffer {
    void writeUtf(String value, int maxLength);
    String readUtf(int maxLength);

    void writeInt(int value);
    int readInt();

    void writeLong(long value);
    long readLong();

    void writeBoolean(boolean value);
    boolean readBoolean();

    void writeVarInt(int value);
    int readVarInt();
}
