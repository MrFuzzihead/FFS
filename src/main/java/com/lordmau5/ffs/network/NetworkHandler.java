package com.lordmau5.ffs.network;

import com.lordmau5.ffs.FancyFluidStorage;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


public class NetworkHandler {
    private static int id = 0;

    private static final String PROTOCOL_VERSION = "1";

    public static void init(IEventBus bus) {
        bus.addListener(NetworkHandler::registerEvent);
    }

    public static void registerEvent(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(FancyFluidStorage.MOD_ID).versioned(PROTOCOL_VERSION);
        registerChannels(registrar);
    }

    public static void registerChannels(PayloadRegistrar registrar) {
        registerBiDirectionalHandlers(registrar);
        registerServerHandlers(registrar);
        registerClientHandlers(registrar);
    }

    private static void registerBiDirectionalHandlers(PayloadRegistrar INSTANCE) {
        // Update Fluid Lock
        INSTANCE.playBidirectional(
                FFSPacket.Server.UpdateFluidLock.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Server.UpdateFluidLock::decode),
                new FFSPacket.Server.UpdateFluidLock.Handler()
        );

        // Update Tile Name
        INSTANCE.playBidirectional(
                FFSPacket.Server.UpdateTileName.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Server.UpdateTileName::decode),
                new FFSPacket.Server.UpdateTileName.Handler()
        );
    }

    private static void registerServerHandlers(PayloadRegistrar INSTANCE) {
        // On Tank Request
        INSTANCE.playToServer(
                FFSPacket.Server.OnTankRequest.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Server.OnTankRequest::decode),
                new FFSPacket.Server.OnTankRequest.Handler()
        );
    }

    private static void registerClientHandlers(PayloadRegistrar INSTANCE) {
        // Open GUI
        INSTANCE.playToClient(
                FFSPacket.Client.OpenGUI.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Client.OpenGUI::decode),
                new FFSPacket.Client.OpenGUI.Handler()
        );

        // On Tank Build
        INSTANCE.playToClient(
                FFSPacket.Client.OnTankBuild.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Client.OnTankBuild::decode),
                new FFSPacket.Client.OnTankBuild.Handler()
        );

        // On Tank Break
        INSTANCE.playToClient(
                FFSPacket.Client.OnTankBreak.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Client.OnTankBreak::decode),
                new FFSPacket.Client.OnTankBreak.Handler()
        );

        // Clear Tanks
        INSTANCE.playToClient(
                FFSPacket.Client.ClearTanks.TYPE,
                StreamCodec.of((buff, packet) -> packet.write(buff), FFSPacket.Client.ClearTanks::decode),
                new FFSPacket.Client.ClearTanks.Handler()
        );
    }

    public static void sendPacketToPlayer(CustomPacketPayload msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    public static void sendPacketToAllPlayers(CustomPacketPayload msg) {
        PacketDistributor.sendToAllPlayers(msg);
    }

    public static void sendPacketToServer(CustomPacketPayload msg) {
        PacketDistributor.sendToServer(msg);
    }
}
