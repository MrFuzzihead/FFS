package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.LongSet;
import com.lordmau5.ffs.util.Position3D;
import com.lordmau5.ffs.util.TankGeometry;
import com.lordmau5.ffs.util.TankMath;
import com.lordmau5.ffs.util.TankStructure;

/** Tank formation and teardown orchestration, extracted from the block entity. */
public final class TankBuilder {

    private TankBuilder() {}

    public static void build(TileEntityValve tank, ForgeDirection inside) {
        if (!tank.getWorldObj().isRemote) {
            // Drop the old slaves first; otherwise they keep pointing at this valve
            // and report themselves valid for the rebuilt tank.
            invalidatePreviousSlaves(tank);
            tank.setValid(false);
            tank.setFluidCapacityInternal(0);
            tank.setInteriorPositions(null);
            tank.tankFrames.clear();
            tank.otherValves.clear();
            if (inside != null) {
                tank.setInside(inside);
            }

            tank.setBuilding(true);
            try {
                if (calculateInside(tank) && setupTank(tank)) {
                    tank.initiated = false;
                    tank.updateBlockAndNeighbors();
                }
            } finally {
                tank.setBuilding(false);
            }
        }
    }

    public static boolean calculateInside(TileEntityValve tank) {
        TankGeometry.Shape shape = TankGeometry.measure(
            (x, y, z) -> tank.getWorldObj()
                .isAirBlock(x, y, z),
            tank.xCoord,
            tank.yCoord,
            tank.zCoord,
            tank.getInside(),
            tank.getMaxSize());
        if (!shape.valid) {
            return false;
        }

        System.arraycopy(shape.lengths, 0, tank.getLength(), 0, tank.getLength().length);
        tank.bottomDiagFrame = shape.bottomDiag;
        tank.topDiagFrame = shape.topDiag;
        tank.tankHeight = shape.tankHeight;
        return true;
    }

    private static void setSlaveValveInside(TileEntityValve tank, LongSet interior, TileEntityValve slave) {
        List<Position3D> possibleAirBlocks = new ArrayList<>();

        for (ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            if (tank.getWorldObj()
                .isAirBlock(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ)) {
                possibleAirBlocks.add(
                    new Position3D(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ));
            }
        }

        Position3D insideAir = null;

        for (Position3D pos : possibleAirBlocks) {
            if (interior.contains(
                LongSet.pack(
                    pos.getX() - tank.bottomDiagFrame.getX(),
                    pos.getY() - tank.bottomDiagFrame.getY(),
                    pos.getZ() - tank.bottomDiagFrame.getZ()))) {
                insideAir = pos;
                break;
            }
        }

        if (insideAir != null) {
            Position3D dist = insideAir.getDistance(new Position3D(slave.xCoord, slave.yCoord, slave.zCoord));

            for (ForgeDirection drx : ForgeDirection.VALID_DIRECTIONS) {
                if (dist.equals(new Position3D(drx.offsetX, drx.offsetY, drx.offsetZ))) {
                    slave.setInside(drx);
                    break;
                }
            }
        }
    }

    public static void updateCornerFrames(TileEntityValve tank) {
        tank.bottomDiagFrame = new Position3D(
            tank.xCoord + tank.getInside().offsetX
                + tank.getLength()[ForgeDirection.WEST.ordinal()] * ForgeDirection.WEST.offsetX
                + ForgeDirection.WEST.offsetX,
            tank.yCoord + tank.getInside().offsetY
                + tank.getLength()[ForgeDirection.DOWN.ordinal()] * ForgeDirection.DOWN.offsetY
                + ForgeDirection.DOWN.offsetY,
            tank.zCoord + tank.getInside().offsetZ
                + tank.getLength()[ForgeDirection.NORTH.ordinal()] * ForgeDirection.NORTH.offsetZ
                + ForgeDirection.NORTH.offsetZ);
        tank.topDiagFrame = new Position3D(
            tank.xCoord + tank.getInside().offsetX
                + tank.getLength()[ForgeDirection.EAST.ordinal()] * ForgeDirection.EAST.offsetX
                + ForgeDirection.EAST.offsetX,
            tank.yCoord + tank.getInside().offsetY
                + tank.getLength()[ForgeDirection.UP.ordinal()] * ForgeDirection.UP.offsetY
                + ForgeDirection.UP.offsetY,
            tank.zCoord + tank.getInside().offsetZ
                + tank.getLength()[ForgeDirection.SOUTH.ordinal()] * ForgeDirection.SOUTH.offsetZ
                + ForgeDirection.SOUTH.offsetZ);
    }

    private static boolean setupTank(TileEntityValve tank) {
        updateCornerFrames(tank);
        TankStructure structure = TankStructure.scan(tank.getWorldObj(), tank.bottomDiagFrame, tank.topDiagFrame);
        if (!structure.validBounds) {
            return false;
        }
        tank.otherValves = new ArrayList<>();
        tank.tankFrames = new ArrayList<>();
        Position3D pos = new Position3D(tank.xCoord, tank.yCoord, tank.zCoord);
        tank.valveHeightPosition = Math.abs(
            tank.bottomDiagFrame.getDistance(pos)
                .getY());
        tank.tankHeight = tank.topDiagFrame.getDistance(tank.bottomDiagFrame)
            .getY() - 1;
        ExtendedBlock bottomDiagBlock = new ExtendedBlock(
            tank.getWorldObj()
                .getBlock(tank.bottomDiagFrame.getX(), tank.bottomDiagFrame.getY(), tank.bottomDiagFrame.getZ()),
            tank.getWorldObj()
                .getBlockMetadata(
                    tank.bottomDiagFrame.getX(),
                    tank.bottomDiagFrame.getY(),
                    tank.bottomDiagFrame.getZ()));
        ExtendedBlock topDiagBlock = new ExtendedBlock(
            tank.getWorldObj()
                .getBlock(tank.topDiagFrame.getX(), tank.topDiagFrame.getY(), tank.topDiagFrame.getZ()),
            tank.getWorldObj()
                .getBlockMetadata(tank.topDiagFrame.getX(), tank.topDiagFrame.getY(), tank.topDiagFrame.getZ()));
        tank.setFrameBurnability(
            bottomDiagBlock.getBlock()
                .getFlammability(
                    tank.getWorldObj(),
                    tank.bottomDiagFrame.getX(),
                    tank.bottomDiagFrame.getY(),
                    tank.bottomDiagFrame.getZ(),
                    ForgeDirection.UNKNOWN));
        if (bottomDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = tank.getWorldObj()
                .getTileEntity(tank.bottomDiagFrame.getX(), tank.bottomDiagFrame.getY(), tank.bottomDiagFrame.getZ());
            if (tile != null && tile instanceof TileEntityTankFrame) {
                ExtendedBlock stored = ((TileEntityTankFrame) tile).getBlock();
                if (stored == null || !stored.hasBlock()) {
                    return false;
                }
                bottomDiagBlock = stored;
            }
        }

        if (topDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = tank.getWorldObj()
                .getTileEntity(tank.topDiagFrame.getX(), tank.topDiagFrame.getY(), tank.topDiagFrame.getZ());
            if (tile != null && tile instanceof TileEntityTankFrame) {
                ExtendedBlock stored = ((TileEntityTankFrame) tile).getBlock();
                if (stored == null || !stored.hasBlock()) {
                    return false;
                }
                topDiagBlock = stored;
            }
        }

        if (bottomDiagBlock == null || topDiagBlock == null
            || !bottomDiagBlock.hasBlock()
            || !topDiagBlock.hasBlock()) {
            // A frame whose original block was removed from the pack; refuse to
            // rebuild instead of dereferencing a null block.
            return false;
        }

        if (!GenericUtil.isValidTankBlock(tank.getWorldObj(), tank.bottomDiagFrame, bottomDiagBlock)) {
            return false;
        } else if (!GenericUtil
            .areTankBlocksValid(bottomDiagBlock, topDiagBlock, tank.getWorldObj(), tank.bottomDiagFrame)) {
                return false;
            } else {
                if (!structure.interiorAirValid) {
                    return false;
                }

                tank.setFluidCapacityInternal(
                    TankMath.capacityInt(
                        FancyFluidStorage.instance.INSIDE_CAPACITY,
                        structure.edges.size(),
                        structure.faces.size(),
                        structure.interiorCount,
                        tank.mbPerVirtualTank));

                for (TankStructure.ShellCell cell : structure.edges) {
                    Position3D fPos = cell.pos;
                    ExtendedBlock fBlock = cell.block;
                    if (!fBlock.hasBlock()) {
                        return false;
                    }

                    int burnability = fBlock.getBlock()
                        .getFlammability(
                            tank.getWorldObj(),
                            fPos.getX(),
                            fPos.getY(),
                            fPos.getZ(),
                            ForgeDirection.UNKNOWN);
                    if (burnability > tank.getFrameBurnability()) {
                        tank.setFrameBurnability(burnability);
                    }

                    if (fBlock.getBlock() instanceof BlockTankFrame) {
                        TileEntity tile = tank.getWorldObj()
                            .getTileEntity(fPos.getX(), fPos.getY(), fPos.getZ());
                        if (tile != null && tile instanceof TileEntityTankFrame) {
                            fBlock = ((TileEntityTankFrame) tile).getBlock();
                        }
                    }

                    if (fBlock == null || !fBlock.hasBlock()) {
                        return false;
                    }

                    if (!GenericUtil.areTankBlocksValid(fBlock, bottomDiagBlock, tank.getWorldObj(), fPos)) {
                        return false;
                    }
                }

                List<TileEntityValve> valves = new ArrayList<>();

                for (TankStructure.ShellCell cell : structure.faces) {
                    pos = cell.pos;
                    ExtendedBlock check = cell.block;
                    int burnabilityx = check.getBlock()
                        .getFlammability(
                            tank.getWorldObj(),
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            ForgeDirection.UNKNOWN);
                    if (burnabilityx > tank.getFrameBurnability()) {
                        tank.setFrameBurnability(burnabilityx);
                    }

                    if (!GenericUtil.areTankBlocksValid(check, bottomDiagBlock, tank.getWorldObj(), pos)
                        && !GenericUtil.isBlockGlass(check.getBlock(), check.getMetadata())) {
                        TileEntity tile = tank.getWorldObj()
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tile == null) {
                            return false;
                        }

                        if (tile instanceof TileEntityValve) {
                            TileEntityValve valve = (TileEntityValve) tile;
                            if (valve != tank) {
                                TileEntityValve owner = valve.getMaster();
                                if (owner != null && owner != tank && owner.isValid()) {
                                    // This valve already belongs to another live tank;
                                    // absorbing it would merge/steal that tank.
                                    return false;
                                }
                                if (valve.getFluidStackInternal() != null) {
                                    tank.setFluidStackInternal(valve.getFluidStackInternal());
                                    tank.updateFluidTemperature();
                                }

                                valves.add(valve);
                            }
                        } else if (!(tile instanceof TileEntityTankFrame)) {
                            return false;
                        }
                    }
                }

                if (tank.getFluidStackInternal() != null) {
                    tank.getFluidStackInternal().amount = Math
                        .min(tank.getFluidStackInternal().amount, tank.getFluidCapacityInternal());
                    if (tank.getFluidStackInternal().amount <= 0) {
                        tank.setFluidStackInternal(null);
                    }
                }

                for (TileEntityValve valve : valves) {
                    pos = new Position3D(valve.xCoord, valve.yCoord, valve.zCoord);
                    invalidatePreviousSlaves(valve);
                    valve.setMaster(false);
                    valve.valveHeightPosition = Math.abs(
                        tank.bottomDiagFrame.getDistance(pos)
                            .getY());
                    valve.setMasterPos(new Position3D(tank.xCoord, tank.yCoord, tank.zCoord));
                    setSlaveValveInside(tank, structure.interior, valve);
                }

                tank.setMaster(true);

                for (TankStructure.ShellCell setTiles : structure.edges) {
                    pos = setTiles.pos;
                    TileEntityTankFrame tankFrame;
                    if (setTiles.block.getBlock() != FancyFluidStorage.blockTankFrame) {
                        tank.getWorldObj()
                            .setBlock(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                FancyFluidStorage.blockTankFrame,
                                setTiles.block.getMetadata(),
                                2);
                        tankFrame = (TileEntityTankFrame) tank.getWorldObj()
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null) {
                            return false;
                        }
                        tankFrame.initialize(tank, setTiles.block);
                    } else {
                        tankFrame = (TileEntityTankFrame) tank.getWorldObj()
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null || tankFrame.isOwnedByOther(tank)) {
                            return false;
                        }
                        if (tankFrame.setValvePos(new Position3D(tank.xCoord, tank.yCoord, tank.zCoord))) {
                            tankFrame.markForUpdate();
                        }
                    }

                    tank.tankFrames.add(tankFrame);
                }

                for (TankStructure.ShellCell setTiles : structure.faces) {
                    pos = setTiles.pos;
                    TileEntity tilex = tank.getWorldObj()
                        .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                    if (tilex != null) {
                        if (tilex instanceof TileEntityValve && tilex != tank) {
                            tank.otherValves.add((TileEntityValve) tilex);
                        } else if (tilex instanceof TileEntityTankFrame) {
                            TileEntityTankFrame tankFrame = (TileEntityTankFrame) tilex;
                            if (tankFrame.isOwnedByOther(tank)) {
                                return false;
                            }
                            if (tankFrame.setValvePos(new Position3D(tank.xCoord, tank.yCoord, tank.zCoord))) {
                                tankFrame.markForUpdate();
                            }
                            tank.tankFrames.add(tankFrame);
                        } else if (GenericUtil.isTileEntityAcceptable(setTiles.block.getBlock(), tilex)) {
                            tank.getWorldObj()
                                .setBlock(
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ(),
                                    FancyFluidStorage.blockTankFrame,
                                    setTiles.block.getMetadata(),
                                    2);
                            TileEntityTankFrame tankFrame = (TileEntityTankFrame) tank.getWorldObj()
                                .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                            if (tankFrame == null) {
                                return false;
                            }
                            tankFrame.initialize(tank, setTiles.block);
                            tank.tankFrames.add(tankFrame);
                        }
                    } else {
                        tank.getWorldObj()
                            .setBlock(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                FancyFluidStorage.blockTankFrame,
                                setTiles.block.getMetadata(),
                                2);
                        TileEntityTankFrame tankFrame = (TileEntityTankFrame) tank.getWorldObj()
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null) {
                            return false;
                        }
                        tankFrame.initialize(tank, setTiles.block);
                        tank.tankFrames.add(tankFrame);
                    }
                }

                tank.setInteriorPositions(structure.interior);
                tank.setValid(true);
                return true;
            }
    }

    public static void breakTank(TileEntityValve tank, TileEntity frame) {
        if (!tank.getWorldObj().isRemote) {
            if (!tank.isMaster() && tank.getMaster() != null) {
                if (tank.getMaster() != tank) {
                    tank.getMaster()
                        .breakTank(frame);
                }
            } else {
                // Capture the fluid before invalidating: getFluid() returns null once
                // isValid is false, which silently destroyed the hand-off.
                FluidStack fluid = tank.getFluidStackInternal();
                tank.setValid(false);

                for (TileEntityValve valve : tank.otherValves) {
                    valve.setFluidStackInternal(fluid);
                    valve.updateFluidTemperature();
                    valve.setMasterPos(null);
                    valve.setValid(false);
                    valve.updateBlockAndNeighbors();
                }

                for (TileEntityTankFrame tankFrame : tank.tankFrames) {
                    if (frame != tankFrame) {
                        tankFrame.breakFrame();
                    }
                }

                tank.tankFrames.clear();
                tank.otherValves.clear();
                tank.setInteriorPositions(null);
                tank.updateBlockAndNeighbors();
            }
        }
    }

    private static void invalidatePreviousSlaves(TileEntityValve tank) {
        if (tank.otherValves == null) {
            return;
        }

        for (TileEntityValve valve : tank.otherValves) {
            if (valve == null || valve == tank) {
                continue;
            }

            valve.setMasterPos(null);
            valve.setMaster(false);
            valve.setValid(false);
            valve.updateBlockAndNeighbors();
        }
    }
}
