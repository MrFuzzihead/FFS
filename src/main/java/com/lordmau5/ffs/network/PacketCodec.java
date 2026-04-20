package com.lordmau5.ffs.network;

import cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class PacketCodec extends FMLIndexedMessageToMessageCodec<FFSPacket> {

    int lastDiscriminator = 0;

    public PacketCodec() {
        this.addPacket(FFSPacket.Server.UpdateAutoOutput.class);
        this.addPacket(FFSPacket.Server.UpdateValveName.class);
    }

    void addPacket(Class<? extends FFSPacket> type) {
        this.addDiscriminator(this.lastDiscriminator, type);
        this.lastDiscriminator++;
    }

    public void encodeInto(ChannelHandlerContext ctx, FFSPacket msg, ByteBuf target) throws Exception {
        msg.encode(target);
    }

    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, FFSPacket msg) {
        msg.decode(source);
    }
}
