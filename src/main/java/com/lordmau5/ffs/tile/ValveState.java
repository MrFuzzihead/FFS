package com.lordmau5.ffs.tile;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.util.Position3D;

/** Plain data snapshot used by the NBT/sync codec. */
public final class ValveState {

    public boolean isValid;
    public ForgeDirection inside = ForgeDirection.UNKNOWN;
    public boolean isMaster;
    public int valveHeightPosition;
    public int tankHeight;
    public int fluidCapacity;
    public FluidStack fluidStack;
    public boolean autoOutput;
    public String valveName = "";
    public Position3D masterValvePos;
    public Position3D bottomDiagFrame;
    public Position3D topDiagFrame;

    public static ValveState capture(TileEntityValve valve) {
        ValveState state = new ValveState();
        state.isValid = valve.isValid;
        state.inside = valve.getInside();
        state.isMaster = valve.isMaster();
        state.valveHeightPosition = valve.valveHeightPosition;
        state.tankHeight = valve.tankHeight;
        state.fluidCapacity = valve.getFluidCapacityInternal();
        state.fluidStack = valve.getFluidStackInternal();
        state.autoOutput = valve.getAutoOutputFlag();
        state.valveName = valve.getValveName();
        state.masterValvePos = valve.getMasterValvePos();
        if (!state.isMaster && state.masterValvePos == null) {
            TileEntityValve resolved = valve.getMaster();
            if (resolved != null) {
                state.masterValvePos = new Position3D(resolved.xCoord, resolved.yCoord, resolved.zCoord);
            }
        }
        state.bottomDiagFrame = valve.bottomDiagFrame;
        state.topDiagFrame = valve.topDiagFrame;
        return state;
    }
}
