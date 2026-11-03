package com.lordmau5.ffs.network.handlers.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.PacketValidation;
import com.lordmau5.ffs.network.ServerTaskQueue;
import com.lordmau5.ffs.tile.TileEntityValve;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class UpdateAutoOutput_Server extends SimpleChannelInboundHandler<FFSPacket.Server.UpdateAutoOutput> {

    protected void channelRead0(ChannelHandlerContext ctx, FFSPacket.Server.UpdateAutoOutput msg) throws Exception {
        final EntityPlayerMP player = NetworkHandler.getPlayer(ctx);
        if (player == null) {
            return;
        }

        final int x = msg.x;
        final int y = msg.y;
        final int z = msg.z;
        final boolean autoOutput = msg.autoOutput;

        // FML channel handlers run on the Netty thread in 1.7.10.
        ServerTaskQueue.schedule(() -> {
            World world = player.worldObj;
            if (world == null || !PacketValidation
                .isWithinReach(x, y, z, player.posX, player.posY, player.posZ, PacketValidation.MAX_REACH)) {
                return;
            }

            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityValve) {
                ((TileEntityValve) tile).setAutoOutput(autoOutput);
            }
        });
    }
}
