package com.velocitypowered.proxy.network.ws;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.ByteBuf;

public abstract class WebSocketHandler extends ChannelDuplexHandler {
    public final String outboundPrefix;
    public final String inboundPrefix;

    public WebSocketHandler(String inboundPrefix, String outboundPrefix) {
        this.inboundPrefix = inboundPrefix;
        this.outboundPrefix = outboundPrefix;
    }

    protected abstract void sendWsFrame(ChannelHandlerContext ctx, WebSocketFrame frame, ChannelPromise promise) throws Exception;

    @Override
    public final void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            sendWsFrame(ctx, new BinaryWebSocketFrame(byteBuf), promise);
        } else {
            // DefaultFullHttpResponse
            ctx.write(msg, promise);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof WebSocketFrame) {
            if (msg instanceof BinaryWebSocketFrame) {
                ByteBuf content = ((WebSocketFrame) msg).content();
                ctx.fireChannelRead(content);
            }
        }
    }

    public static class WebSocketServerHandler extends WebSocketHandler {
        public WebSocketServerHandler() {
            super("C->S", "S->C");
        }

        @Override
        protected void sendWsFrame(ChannelHandlerContext ctx, WebSocketFrame frame, ChannelPromise promise) {
            ctx.write(frame, promise);
        }
    }
}