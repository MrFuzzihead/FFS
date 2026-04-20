package com.lordmau5.ffs.network.handlers.server;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.tile.TileEntityValve;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class UpdateAutoOutput_Server extends SimpleChannelInboundHandler<FFSPacket.Server.UpdateAutoOutput> {

    protected void channelRead0(ChannelHandlerContext ctx, FFSPacket.Server.UpdateAutoOutput msg) throws Exception {
        World world = NetworkHandler.getPlayer(ctx).worldObj;
        if (world != null) {
            TileEntity tile = world.getTileEntity(msg.x, msg.y, msg.z);
            if (tile != null && tile instanceof TileEntityValve) {
                ((TileEntityValve) tile).setAutoOutput(msg.autoOutput);
            }
        }
    }
}
