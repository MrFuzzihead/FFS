package com.lordmau5.ffs.blockentity.abstracts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class AbstractTankEntity extends BlockEntity {

    /**
     * Necessary stuff for the interfaces.
     * Current interface list:
     * INameableTile, IFacingTile
     */
    protected Direction tile_facing = null;
    protected int needsUpdate = 0;
    String tile_name = "";
    private BlockPos mainValvePos;

    public AbstractTankEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        AbstractTankEntity tile = (AbstractTankEntity) be;

        if (tile.needsUpdate > 0) {
            tile.markForUpdate();
        }
    }

    public void setNeedsUpdate() {
        if (this.needsUpdate == 0) {
            this.needsUpdate = 20;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        setNeedsUpdate();
    }

    public boolean isValid() {
        return getMainValve() != null && getMainValve().isValid();
    }

    void setValvePos(BlockPos mainValvePos) {
        this.mainValvePos = mainValvePos;
    }

    public AbstractTankValve getMainValve() {
        if (getLevel() != null && this.mainValvePos != null) {
            BlockEntity tile = getLevel().getBlockEntity(this.mainValvePos);
            return tile instanceof AbstractTankValve ? (AbstractTankValve) tile : null;
        }

        return null;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        setValvePos(tag.contains("valvePos") ? BlockPos.of(tag.getLong("valvePos")) : null);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (this.mainValvePos != null) {
            tag.putLong("valvePos", this.mainValvePos.asLong());
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);

        boolean oldIsValid = isValid();

        if (getLevel() != null && getLevel().isClientSide() && oldIsValid != isValid()) {
            markForUpdateNow();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public void doUpdate() {
    }

    public void markForUpdate() {
        if (getLevel() == null) {
            setNeedsUpdate();
            return;
        }

        if (--this.needsUpdate == 0) {
            BlockState state = getLevel().getBlockState(getBlockPos());
            getLevel().sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_ALL);
            doUpdate();
            setChanged();
        }
    }

    public void markForUpdateNow() {
        this.needsUpdate = 1;
        markForUpdate();
    }

    public void markForUpdateNow(int when) {
        this.needsUpdate = Math.min(when, 20);
        markForUpdate();
    }

    @Override
    public int hashCode() {
        return getBlockPos().hashCode();
    }

}
