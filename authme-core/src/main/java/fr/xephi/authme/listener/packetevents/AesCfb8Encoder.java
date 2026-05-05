package fr.xephi.authme.listener.packetevents;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;

/**
 * Netty outbound handler that AES/CFB8-encrypts the byte stream to the client.
 * Must be inserted BEFORE the length-prepender so the prepended length is part of the
 * encrypted stream (matching Minecraft's protocol: the length varint is also encrypted).
 */
class AesCfb8Encoder extends MessageToByteEncoder<ByteBuf> {

    private final Cipher cipher;

    AesCfb8Encoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        byte[] input = new byte[in.readableBytes()];
        in.readBytes(input);
        out.writeBytes(cipher.update(input));
    }
}
