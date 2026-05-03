package fr.xephi.authme.listener.packetevents;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import java.util.List;

/**
 * Netty inbound handler that AES/CFB8-decrypts the raw byte stream from the client.
 * Must be inserted BEFORE the frame-splitter so the splitter sees plaintext frames.
 */
class AesCfb8Decoder extends MessageToMessageDecoder<ByteBuf> {

    private final Cipher cipher;

    AesCfb8Decoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        byte[] input = new byte[msg.readableBytes()];
        msg.readBytes(input);
        out.add(Unpooled.wrappedBuffer(cipher.update(input)));
    }
}
