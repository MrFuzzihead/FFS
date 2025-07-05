package com.lordmau5.ffs.network;

import com.google.common.collect.Sets;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankEntity;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankValve;
import com.lordmau5.ffs.blockentity.interfaces.INameableEntity;
import com.lordmau5.ffs.util.TankManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.HashSet;
import java.util.TreeMap;

public abstract class FFSPacket {
    public static abstract class Client {
        public static class OpenGUI implements CustomPacketPayload {
            public BlockPos pos;
            public boolean isValve;

            public static final Type<OpenGUI> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "open_gui"));


            public OpenGUI() {
            }

            public OpenGUI(AbstractTankEntity tile, boolean isValve) {
                this.pos = tile.getBlockPos();
                this.isValve = isValve;
            }

            public void write(FriendlyByteBuf buffer) {
                buffer.writeBlockPos(this.pos);
                buffer.writeBoolean(this.isValve);
            }

            public static OpenGUI decode(FriendlyByteBuf buffer) {
                OpenGUI packet = new OpenGUI();

                packet.pos = buffer.readBlockPos();
                packet.isValve = buffer.readBoolean();

                return packet;
            }

            public BlockPos getValvePos() {
                return pos;
            }

            public boolean getIsValve() {
                return isValve;
            }

            public static class Handler implements IPayloadHandler<OpenGUI> {
                @Override
                public void handle(OpenGUI payload, IPayloadContext context) {
                    context.enqueueWork(() -> {
                        FFSPacketClientHandler.handleOnOpenGUI(payload);
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }

        public static class OnTankBuild implements CustomPacketPayload {
            private BlockPos valvePos;
            private TreeMap<Integer, HashSet<BlockPos>> airBlocks;
            private TreeMap<Integer, HashSet<BlockPos>> frameBlocks;
            public static final Type<OnTankBuild> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "on_tank_build"));

            public OnTankBuild() {
            }

            public OnTankBuild(AbstractTankValve valve) {
                this.valvePos = valve.getBlockPos();
                this.airBlocks = valve.getAirBlocks();
                this.frameBlocks = valve.getFrameBlocks();
            }

            public void write(FriendlyByteBuf buffer) {
                buffer.writeLong(this.valvePos.asLong());

                buffer.writeInt(this.airBlocks.size());
                for (int layer : this.airBlocks.keySet()) {
                    buffer.writeInt(layer);

                    var layerAirBlocks = this.airBlocks.get(layer);
                    buffer.writeCollection(layerAirBlocks, (buf, pos) -> buf.writeBlockPos(pos));
                }

                buffer.writeInt(this.frameBlocks.size());
                for (int layer : this.frameBlocks.keySet()) {
                    buffer.writeInt(layer);

                    var layerFrameBlocks = this.frameBlocks.get(layer);
                    buffer.writeCollection(layerFrameBlocks, (buf, pos) -> buf.writeBlockPos(pos));
                }
            }

            public static OnTankBuild decode(FriendlyByteBuf buffer) {
                OnTankBuild packet = new OnTankBuild();

                packet.valvePos = BlockPos.of(buffer.readLong());

                packet.airBlocks = new TreeMap<>();
                int layerSize = buffer.readInt();
                for (int i = 0; i < layerSize; i++) {
                    int layer = buffer.readInt();

                    HashSet<BlockPos> layerBlocks = buffer.readCollection(Sets::newHashSetWithExpectedSize, reader -> BlockPos.of(reader.readLong()));

                    packet.airBlocks.put(layer, layerBlocks);
                }

                packet.frameBlocks = new TreeMap<>();
                layerSize = buffer.readInt();
                for (int i = 0; i < layerSize; i++) {
                    int layer = buffer.readInt();

                    HashSet<BlockPos> layerBlocks = buffer.readCollection(Sets::newHashSetWithExpectedSize, reader -> BlockPos.of(reader.readLong()));

                    packet.frameBlocks.put(layer, layerBlocks);
                }

                return packet;
            }

            public BlockPos getValvePos() {
                return valvePos;
            }

            public TreeMap<Integer, HashSet<BlockPos>> getAirBlocks() {
                return airBlocks;
            }

            public TreeMap<Integer, HashSet<BlockPos>> getFrameBlocks() {
                return frameBlocks;
            }

            public static class Handler implements IPayloadHandler<OnTankBuild> {
                @Override
                public void handle(OnTankBuild payload, IPayloadContext context) {
                    context.enqueueWork(() -> {
                        FFSPacketClientHandler.handleOnTankBuild(payload);
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }

        public static class OnTankBreak implements CustomPacketPayload {
            private BlockPos valvePos;
            public static final Type<OnTankBreak> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "on_tank_break"));

            public OnTankBreak() {
            }

            public OnTankBreak(AbstractTankValve valve) {
                this.valvePos = valve.getBlockPos();
            }


            public void write(FriendlyByteBuf buffer) {
                buffer.writeBlockPos(this.valvePos);
            }

            public static OnTankBreak decode(FriendlyByteBuf buffer) {
                OnTankBreak packet = new OnTankBreak();

                packet.valvePos = buffer.readBlockPos();

                return packet;
            }

            public BlockPos getValvePos() {
                return valvePos;
            }

            public static class Handler implements IPayloadHandler<OnTankBreak> {
                @Override
                public void handle(OnTankBreak payload, IPayloadContext context) {
                    context.enqueueWork(() -> {
                        FFSPacketClientHandler.handleOnTankBreak(payload);
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }

        public static class ClearTanks implements CustomPacketPayload {
            public static final Type<ClearTanks> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "clear_tanks"));

            public ClearTanks() {
            }


            public void write(FriendlyByteBuf buffer) {
            }

            public static ClearTanks decode(FriendlyByteBuf buffer) {
                return new ClearTanks();
            }

            public static class Handler implements IPayloadHandler<ClearTanks> {
                @Override
                public void handle(ClearTanks payload, IPayloadContext context) {
                    context.enqueueWork(() -> {
                        TankManager.INSTANCE.clear();
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }
    }

    public static class Server {
        public static class UpdateTileName implements CustomPacketPayload {
            private BlockPos pos;
            private String name;

            public static final Type<UpdateTileName> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "update_tile_name"));

            public UpdateTileName() {
            }

            public UpdateTileName(AbstractTankEntity tankTile, String name) {
                this.pos = tankTile.getBlockPos();
                this.name = name;
            }

            public void write(FriendlyByteBuf buffer) {
                buffer.writeBlockPos(this.pos);
                buffer.writeUtf(this.name);
            }

            public static UpdateTileName decode(FriendlyByteBuf buffer) {
                UpdateTileName packet = new UpdateTileName();

                packet.pos = buffer.readBlockPos();
                packet.name = buffer.readUtf();

                return packet;
            }

            public BlockPos getPos() {
                return pos;
            }

            public String getName() {
                return name;
            }

            public static class Handler implements IPayloadHandler<UpdateTileName> {
                @Override
                public void handle(UpdateTileName payload, IPayloadContext ctx) {
                    ctx.enqueueWork(() -> {
                        Player player = ctx.player();
                        Level level = player.level();

                        BlockEntity tile = level.getBlockEntity(payload.getPos());
                        if (tile instanceof AbstractTankEntity abstractTankTile && tile instanceof INameableEntity) {
                            ((INameableEntity) abstractTankTile).setTileName(payload.getName());
                            abstractTankTile.markForUpdateNow();
                        }
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }

        public static class UpdateFluidLock implements CustomPacketPayload
        {
            private BlockPos pos;
            private boolean fluidLock;

            public static final Type<UpdateFluidLock> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "update_fluid_lock"));

            public UpdateFluidLock() {
            }

            public UpdateFluidLock(AbstractTankValve valve) {
                this.pos = valve.getBlockPos();
                this.fluidLock = valve.getTankConfig().isFluidLocked();
            }


            public void write(FriendlyByteBuf buffer)
            {
                buffer.writeBlockPos(this.pos);
                buffer.writeBoolean(this.fluidLock);
            }


            public static UpdateFluidLock decode(FriendlyByteBuf buffer) {
                UpdateFluidLock packet = new UpdateFluidLock();

                packet.pos = buffer.readBlockPos();
                packet.fluidLock = buffer.readBoolean();

                return packet;
            }

            public BlockPos getPos() {
                return pos;
            }

            public boolean isFluidLock() {
                return fluidLock;
            }

            public static class Handler implements IPayloadHandler<UpdateFluidLock> {
                @Override
                public void handle(UpdateFluidLock payload, IPayloadContext ctx) {
                    ctx.enqueueWork(() -> {
                        Player player = ctx.player();
                        Level level = player.level();

                        BlockEntity tile = level.getBlockEntity(payload.getPos());
                        if (tile instanceof AbstractTankValve valve) {
                            valve.setFluidLock(payload.isFluidLock());
                        }
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }

        public static class OnTankRequest implements CustomPacketPayload {
            private BlockPos pos;
            public static final Type<OnTankRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "on_tank_request"));

            public OnTankRequest() {
            }

            public OnTankRequest(AbstractTankValve valve) {
                this.pos = valve.getBlockPos();
            }

            public void write(FriendlyByteBuf buffer) {
                buffer.writeBlockPos(this.pos);
            }

            public static OnTankRequest decode(FriendlyByteBuf buffer) {
                OnTankRequest packet = new OnTankRequest();

                packet.pos = buffer.readBlockPos();

                return packet;
            }

            public BlockPos getPos() {
                return pos;
            }

            public static class Handler implements IPayloadHandler<OnTankRequest> {
                @Override
                public void handle(OnTankRequest payload, IPayloadContext ctx) {
                    ctx.enqueueWork(() -> {
                        Player player = ctx.player();
                        Level level = player.level();

                        BlockEntity tile = level.getBlockEntity(payload.getPos());
                        if (tile instanceof AbstractTankValve) {
                            NetworkHandler.sendPacketToPlayer(new FFSPacket.Client.OnTankBuild((AbstractTankValve) tile), (ServerPlayer) player);
                        }
                    });
                }
            }

            @Override
            public Type<? extends CustomPacketPayload> type()
            {
                return TYPE;
            }
        }
    }
}
