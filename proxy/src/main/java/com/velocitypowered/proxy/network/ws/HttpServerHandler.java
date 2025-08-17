package com.velocitypowered.proxy.network.ws;

import java.util.function.BiConsumer;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    public final static String wsmcEndpoint = System.getProperty("wsmc.wsmcEndpoint", null);

    /**
     * This will set your maximum allowable frame payload length.
     * Setting this value for big modpack.
     */
    public final static String maxFramePayloadLength = System.getProperty("wsmc.maxFramePayloadLength", "32768");

    /**
     * This will be called when a WebSocket upgrade is received.
     * Note that this does NOT guarantee a success WebSocket handshake.
     */
    private final BiConsumer<ChannelHandlerContext, HttpRequest> onWsmcHandshake;

    public HttpServerHandler(BiConsumer<ChannelHandlerContext, HttpRequest> onWsmcHandshake) {
        this.onWsmcHandshake = onWsmcHandshake;
    }

    private boolean isWsmcEndpoint(String endpoint) {
        if (HttpServerHandler.wsmcEndpoint == null)
            return true;

        // This has to be case-sensitive!
        return HttpServerHandler.wsmcEndpoint.equals(endpoint);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest httpRequest) {
            String endpoint = httpRequest.uri();

            HttpHeaders headers = httpRequest.headers();

            if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
                    && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))
                    && isWsmcEndpoint(endpoint)) {
                String url = "ws://" + httpRequest.headers().get("Host") + httpRequest.uri();

                if (this.onWsmcHandshake != null) {
                    this.onWsmcHandshake.accept(ctx, httpRequest);
                }

                ctx.pipeline().replace(this, "WsmcWebSocketServerHandler", new WebSocketHandler.WebSocketServerHandler());

                int maxFramePayloadLength = 65536;

                try {
                    maxFramePayloadLength = Integer.parseInt(HttpServerHandler.maxFramePayloadLength);
                } catch (Exception ignored) {
                }

                WebSocketServerHandshakerFactory wsFactory =
                        new WebSocketServerHandshakerFactory(url, null, true, maxFramePayloadLength);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);

                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), httpRequest);
                }

                // Here we assume that the server never actively sends anything before it receives anything from the client.
            } else {
                // Not a WebSocket upgrade request, send a default HTTP response
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK, Unpooled.copiedBuffer("HTTP default response", CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}