package com.suian.xaeroregionsrev.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class PacketCodecs {
    private PacketCodecs() {
    }

    public static <B extends FriendlyByteBuf, T> StreamCodec<B, T> of(
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder
    ) {
        return new StreamCodec<>() {
            @Override
            public T decode(B buffer) {
                return decoder.apply(buffer);
            }

            @Override
            public void encode(B buffer, T value) {
                encoder.accept(value, buffer);
            }
        };
    }
}
