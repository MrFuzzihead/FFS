package com.lordmau5.ffs.blockentity.tanktiles;

import com.lordmau5.ffs.blockentity.abstracts.AbstractTankEntity;
import com.lordmau5.ffs.holder.FFSBlockEntities;
import com.lordmau5.ffs.util.FFSStateProps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityTankComputer extends AbstractTankEntity {

    public BlockEntityTankComputer(BlockPos pos, BlockState state) {
        super(FFSBlockEntities.tankComputer.get(), pos, state);
    }

    @Override
    public void doUpdate() {
        super.doUpdate();

        if (getLevel() == null) return;

        getLevel().setBlockAndUpdate(getBlockPos(), getBlockState()
                .setValue(FFSStateProps.TILE_VALID, isValid())
        );
    }
}
