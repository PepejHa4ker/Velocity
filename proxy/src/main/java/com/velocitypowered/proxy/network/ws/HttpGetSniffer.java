package com.velocitypowered.proxy.network.ws;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;

public class HttpGetSniffer extends ByteToMessageDecoder {
    public final static boolean disableVanillaTCP =
            System.getProperty("wsmc.disableVanillaTCP", "false").equalsIgnoreCase("true");

    private final BiConsumer<ChannelHandlerContext, HttpRequest> onWsmcHandshake;

    public HttpGetSniffer(BiConsumer<ChannelHandlerContext, HttpRequest> onWsmcHandshake) {
        this.onWsmcHandshake = onWsmcHandshake;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() > 3) {
            byte[] byteBuffer = new byte[3];
            in.markReaderIndex();
            in.readBytes(byteBuffer, 0, 3);
            in.resetReaderIndex();
            String methodString = new String(byteBuffer, StandardCharsets.US_ASCII);

            if (methodString.equalsIgnoreCase("GET")) {
                ctx.pipeline().replace(this, "WsmcHttpCodec", new HttpServerCodec());
                ctx.pipeline().addAfter("WsmcHttpCodec", "WsmcHttpHandler", new HttpServerHandler(this.onWsmcHandshake));
            } else {
                if (HttpGetSniffer.disableVanillaTCP) {
                    throw new RuntimeException("Vanilla TCP connection has been disabled by WSMC.");
                }

                ctx.pipeline().remove(this);
            }
        }
    }
}