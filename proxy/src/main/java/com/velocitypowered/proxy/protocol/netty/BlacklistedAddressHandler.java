package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class BlacklistedAddressHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final VelocityServer server;

    private static final Logger log = LoggerFactory.getLogger(BlacklistedAddressHandler.class);
    public BlacklistedAddressHandler(VelocityServer server) {
        this.server = server;
    }
    @Override
    protected void channelRead0(
            final ChannelHandlerContext ctx,
            final DatagramPacket datagramPacket
    ) {
        InetSocketAddress receivedAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        if (server.getConfiguration().getBlockedAddresses().contains(receivedAddress.getHostString())) {
            log.info("{} Tried to ping the server. Connection closed due to address being blacklisted", receivedAddress.getHostString());
            ctx.close();
        }
    }
}
