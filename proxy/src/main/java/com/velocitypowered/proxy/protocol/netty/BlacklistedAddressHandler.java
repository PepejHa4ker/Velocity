package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.net.InetSocketAddress;

public class BlacklistedAddressHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final VelocityServer server;

    private static final ComponentLogger logger = ComponentLogger
            .logger(BlacklistedAddressHandler.class);

    public BlacklistedAddressHandler(VelocityServer server) {
        this.server = server;
    }
    @Override
    protected void channelRead0(
            final ChannelHandlerContext ctx,
            final DatagramPacket datagramPacket
    ) {
        InetSocketAddress receivedAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("PINGING {}, {}", receivedAddress, receivedAddress.getHostName());
        if (server.getConfiguration().getBlockedAddresses().contains(receivedAddress.getHostName())) {
            logger.info("{} Tried to ping the server. Connection closed due to address are blacklisted", receivedAddress.getHostName());
            ctx.close();
        }
    }
}
