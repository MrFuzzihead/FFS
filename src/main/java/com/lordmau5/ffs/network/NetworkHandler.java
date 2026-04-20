package com.lordmau5.ffs.network;

import java.util.EnumMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;

import com.lordmau5.ffs.network.handlers.server.UpdateAutoOutput_Server;
import com.lordmau5.ffs.network.handlers.server.UpdateValveName_Server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

public class NetworkHandler {

    private static EnumMap<Side, FMLEmbeddedChannel> channels;

    public static void registerChannels(Side side) {
        channels = NetworkRegistry.INSTANCE.newChannel("ffs", new ChannelHandler[] { new PacketCodec() });
        ChannelPipeline pipeline = channels.get(Side.SERVER)
            .pipeline();
        String targetName = channels.get(Side.SERVER)
            .findChannelHandlerNameForType(PacketCodec.class);
        pipeline.addAfter(targetName, "UpdateAutoOutput_Server", new UpdateAutoOutput_Server());
        pipeline.addAfter(targetName, "UpdateValveName_Server", new UpdateValveName_Server());
        if (side.isClient()) {
            registerClientHandlers();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerClientHandlers() {
        ChannelPipeline pipeline = channels.get(Side.CLIENT)
            .pipeline();
        String targetName = channels.get(Side.CLIENT)
            .findChannelHandlerNameForType(PacketCodec.class);
    }

    public static Packet getProxyPacket(FFSPacket packet) {
        return channels.get(
            FMLCommonHandler.instance()
                .getEffectiveSide())
            .generatePacketFrom(packet);
    }

    public static void sendPacketToPlayer(FFSPacket packet, EntityPlayer player) {
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
            .set(player);
        channel.writeOutbound(new Object[] { packet });
    }

    public static void sendPacketToAllPlayers(FFSPacket packet) {
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(OutboundTarget.ALL);
        channel.writeOutbound(new Object[] { packet });
    }

    public static void sendPacketToServer(FFSPacket packet) {
        FMLEmbeddedChannel channel = channels.get(Side.CLIENT);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(OutboundTarget.TOSERVER);
        channel.writeOutbound(new Object[] { packet });
    }

    public static EntityPlayerMP getPlayer(ChannelHandlerContext ctx) {
        return ((NetHandlerPlayServer) ctx.channel()
            .attr(NetworkRegistry.NET_HANDLER)
            .get()).playerEntity;
    }
}
