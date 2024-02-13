/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.network;

import static com.velocitypowered.proxy.network.Connections.*;


import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.websocket.HttpGetSniffer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.LegacyPingDecoder;
import com.velocitypowered.proxy.protocol.netty.LegacyPingEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Server channel initializer.
 */
@SuppressWarnings("WeakerAccess")
public class ServerChannelInitializer extends ChannelInitializer<Channel> {

    private static final Logger LOGGER = LogManager.getLogger("ServerChannelInitializer");
    public static final AttributeKey<String> X_REAL_IP = AttributeKey.newInstance("x-real-iip");

    private final VelocityServer server;

  public ServerChannelInitializer(final VelocityServer server) {
    this.server = server;
  }

  @Override
  protected void initChannel(final Channel ch) {
    ch.pipeline()
        .addLast(WS_HTTP_GET_SNIFFER, new HttpGetSniffer((ctx, httpRequest) -> {
          final String realIp = httpRequest.headers().get("x-real-ip");
          LOGGER.info("Received x-real-ip header: {}", realIp);
          ctx.channel().attr(X_REAL_IP).set(realIp);
        }))
        .addLast(LEGACY_PING_DECODER, new LegacyPingDecoder())
        .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
        .addLast(READ_TIMEOUT,
            new ReadTimeoutHandler(this.server.getConfiguration().getReadTimeout(),
                TimeUnit.MILLISECONDS))
        .addLast(LEGACY_PING_ENCODER, LegacyPingEncoder.INSTANCE)
        .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
        .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolUtils.Direction.SERVERBOUND))
        .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolUtils.Direction.CLIENTBOUND));


    final MinecraftConnection connection = new MinecraftConnection(ch, this.server);

    connection.setActiveSessionHandler(StateRegistry.HANDSHAKE,
        new HandshakeSessionHandler(connection, this.server));
    ch.pipeline().addLast(Connections.HANDLER, connection);

    if (this.server.getConfiguration().isProxyProtocol()) {
      ch.pipeline().addFirst(new HAProxyMessageDecoder());
    }
  }
}
