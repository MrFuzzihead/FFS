package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.LongSet;
import com.lordmau5.ffs.util.ModNames;
import com.lordmau5.ffs.util.Position3D;
import com.lordmau5.ffs.util.TankMath;

/** Rain, burning and leaking behaviour, extracted from the block entity. */
public final class TankHazards {

    private TankHazards() {}

    public static void tickRain(TileEntityValve valve) {
        if (valve.getWorldObj() == null) {
            return;
        }

        FluidStack fluid = valve.getFluid();
        if (fluid == null || fluid.getFluid() != FluidRegistry.WATER
            || !valve.getWorldObj()
                .isRaining()) {
            return;
        }

        TileEntityValve master = valve.getMaster();
        long worldTime = valve.getWorldObj()
            .getTotalWorldTime();
        if (valve.getCachedPrecipHeight() < 0 || worldTime - valve.getLastPrecipCheck() >= 20) {
            valve.setCachedPrecipHeight(
                valve.getWorldObj()
                    .getPrecipitationHeight(valve.xCoord, valve.zCoord));
            valve.setLastPrecipCheck(worldTime);
        }

        if (master != null && master.getLastRainTick() != worldTime
            && valve.yCoord == valve.getCachedPrecipHeight() - 1) {
            // Only one valve per tank may collect rain per tick.
            master.setLastRainTick(worldTime);
            int rate = (int) Math.floor(
                valve.getWorldObj().rainingStrength * 5.0F
                    * valve.getWorldObj()
                        .getBiomeGenForCoords(valve.xCoord, valve.zCoord).rainfall);
            FluidStack waterStack = fluid.copy();
            waterStack.amount = rate * 10;
            valve.fill(waterStack, true);
        }
    }

    public static void tickBurn(TileEntityValve valve) {
        if (valve.getWorldObj() == null || !valve.isMaster()) {
            return;
        }

        if (!FancyFluidStorage.instance.SET_WORLD_ON_FIRE || valve.getMinBurnableTemp() <= 0
            || valve.getFluidTemperature() < valve.getMinBurnableTemp()
            || valve.getFrameBurnability() <= 0) {
            return;
        }

        int burnTicks = valve.getRandomBurnTicks();
        if (burnTicks > 0) {
            valve.setRandomBurnTicks(burnTicks - 1);
            return;
        }

        valve.setRandomBurnTicks(100);
        Random random = valve.getWorldObj().rand;
        int temperatureDiff = valve.getFluidTemperature() - valve.getMinBurnableTemp();
        int chanceOfBurnability = 300 - valve.getFrameBurnability();
        int rand = random.nextInt(300) + temperatureDiff
            + (int) Math.floor((float) valve.getFluidAmount() / valve.getCapacity() * 300.0F);
        if (rand < chanceOfBurnability) {
            return;
        }

        boolean successfullyBurned = false;
        List<TileEntityTankFrame> remainingFrames = new ArrayList<>();
        remainingFrames.addAll(valve.tankFrames);
        List<TileEntityTankFrame> removingFrames = new ArrayList<>();

        while (!successfullyBurned && !remainingFrames.isEmpty()) {
            boolean couldBurnOne = false;

            for (int i = 0; i < Math.min(10, remainingFrames.size()); i++) {
                int id = random.nextInt(remainingFrames.size());
                TileEntityTankFrame frame = remainingFrames.get(id);
                couldBurnOne = frame.tryBurning();
                if (!couldBurnOne) {
                    removingFrames.add(frame);
                }
            }

            remainingFrames.removeAll(removingFrames);
            removingFrames.clear();
            if (couldBurnOne) {
                successfullyBurned = true;
            }
        }

        if (!successfullyBurned) {
            remainingFrames.clear();
            remainingFrames.addAll(valve.tankFrames);
            List<Position3D> firePos = new ArrayList<>();
            int ix = 0;

            while (ix < 3 && !remainingFrames.isEmpty()) {
                int id = random.nextInt(remainingFrames.size());
                TileEntityTankFrame frame = remainingFrames.get(id);
                Block frameBlock = frame.getStoredBlock();
                if (frameBlock != null && frameBlock.isFlammable(
                    valve.getWorldObj(),
                    frame.xCoord,
                    frame.yCoord,
                    frame.zCoord,
                    ForgeDirection.UNKNOWN)) {
                    firePos.add(new Position3D(frame.xCoord, frame.yCoord, frame.zCoord));
                    ix++;
                } else {
                    remainingFrames.remove(id);
                }
            }

            for (Position3D pos : firePos) {
                if (valve.getWorldObj()
                    .getBlock(pos.getX(), pos.getY(), pos.getZ())
                    .isFlammable(valve.getWorldObj(), pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN)) {
                    valve.getWorldObj()
                        .setBlock(pos.getX(), pos.getY(), pos.getZ(), Blocks.fire);
                }
            }
        }

        valve.setFrameBurnability(0);
        if (FancyFluidStorage.instance.SET_WORLD_ON_FIRE) {
            valve.getWorldObj()
                .playSoundEffect(
                    valve.xCoord + 0.5,
                    valve.yCoord + 0.5,
                    valve.zCoord + 0.5,
                    ModNames.resource("fire"),
                    1.0F,
                    valve.getWorldObj().rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    public static void tickLeak(TileEntityValve valve) {
        if (valve.getWorldObj() == null || !valve.isMaster() || valve.bottomDiagFrame == null) {
            return;
        }

        if (!FancyFluidStorage.instance.SHOULD_TANKS_LEAK) {
            return;
        }

        int leakTicks = valve.getRandomLeakTicks();
        if (leakTicks > 0) {
            valve.setRandomLeakTicks(leakTicks - 1);
            return;
        }
        valve.setRandomLeakTicks(leakTicks - 1);

        FluidStack stack = valve.getFluidStackInternal();
        if (stack == null || !stack.getFluid()
            .canBePlacedInWorld()) {
            return;
        }

        valve.setRandomLeakTicks(1200);
        Random random = valve.getWorldObj().rand;
        int amt = random.nextInt(3) + 1;
        List<TileEntityTankFrame> validFrames = new ArrayList<>();
        List<TileEntityTankFrame> remainingFrames = new ArrayList<>();
        remainingFrames.addAll(valve.tankFrames);
        int ix = 0;

        while (ix < amt && !remainingFrames.isEmpty()) {
            int id = random.nextInt(remainingFrames.size());
            TileEntityTankFrame frame = remainingFrames.get(id);
            Block block = frame.getStoredBlock();
            if (block != null && GenericUtil.canBlockLeak(block)
                && !frame.getNeighborBlockOrAir(
                    stack.getFluid()
                        .getBlock())
                    .isEmpty()
                && TankMath.isValidLeakHardness(
                    block.getBlockHardness(valve.getWorldObj(), frame.xCoord, frame.yCoord, frame.zCoord))) {
                validFrames.add(frame);
                ix++;
            } else {
                remainingFrames.remove(id);
            }
        }

        for (TileEntityTankFrame frame : validFrames) {
            Block block = frame.getStoredBlock();
            if (block == null) {
                continue;
            }

            int hardness = TankMath.leakHardnessTicks(
                block.getBlockHardness(valve.getWorldObj(), frame.xCoord, frame.yCoord, frame.zCoord));
            int rand = random.nextInt(hardness) + 1;
            int diff = (int) Math.ceil(50.0F * ((float) valve.getFluidAmount() / valve.getCapacity()));
            if (rand >= hardness - diff) {
                List<ForgeDirection> dirs = frame.getNeighborBlockOrAir(
                    stack.getFluid()
                        .getBlock());
                if (dirs.isEmpty()) {
                    continue;
                }

                ForgeDirection leakDir = dirs.size() > 1 ? dirs.get(random.nextInt(dirs.size())) : dirs.get(0);
                Position3D leakPos = new Position3D(
                    frame.xCoord + leakDir.offsetX,
                    frame.yCoord + leakDir.offsetY,
                    frame.zCoord + leakDir.offsetZ);
                LongSet interior = valve.getInteriorPositions();
                if ((interior == null || !interior.contains(
                    LongSet.pack(
                        leakPos.getX() - valve.bottomDiagFrame.getX(),
                        leakPos.getY() - valve.bottomDiagFrame.getY(),
                        leakPos.getZ() - valve.bottomDiagFrame.getZ())))
                    && stack.amount >= 1000) {
                    valve.getWorldObj()
                        .setBlock(
                            frame.xCoord + leakDir.offsetX,
                            frame.yCoord + leakDir.offsetY,
                            frame.zCoord + leakDir.offsetZ,
                            stack.getFluid()
                                .getBlock(),
                            0,
                            3);
                    valve.getWorldObj()
                        .notifyBlockOfNeighborChange(
                            frame.xCoord + leakDir.offsetX,
                            frame.yCoord + leakDir.offsetY,
                            frame.zCoord + leakDir.offsetZ,
                            stack.getFluid()
                                .getBlock());
                    valve.drain(1000, true);
                }
            }
        }
    }
}
