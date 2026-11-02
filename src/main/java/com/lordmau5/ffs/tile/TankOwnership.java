package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

import com.lordmau5.ffs.util.Position3D;

/** Master/slave link state and valve-list helpers, extracted from the block entity. */
public final class TankOwnership {

    private boolean master;
    private Position3D masterValvePos;
    private TileEntityValve cachedMaster;

    public boolean isMaster() {
        return this.master;
    }

    public void setMaster(boolean master) {
        this.master = master;
        if (master) {
            this.masterValvePos = null;
            this.cachedMaster = null;
        }
    }

    public Position3D getMasterValvePos() {
        return this.masterValvePos;
    }

    public void setMasterPos(Position3D masterValvePos) {
        this.masterValvePos = masterValvePos;
        this.cachedMaster = null;
    }

    public void invalidate() {
        this.cachedMaster = null;
    }

    public TileEntityValve getMaster(TileEntityValve valve) {
        if (this.master) {
            return valve;
        }

        if (this.cachedMaster != null && !this.cachedMaster.isInvalid()) {
            return this.cachedMaster;
        }
        this.cachedMaster = null;

        if (this.masterValvePos != null && valve.getWorldObj() != null) {
            TileEntity tile = valve.getWorldObj()
                .getTileEntity(this.masterValvePos.getX(), this.masterValvePos.getY(), this.masterValvePos.getZ());
            this.cachedMaster = tile instanceof TileEntityValve ? (TileEntityValve) tile : null;
        }

        return this.cachedMaster;
    }

    public List<TileEntityValve> getAllValves(TileEntityValve valve, List<TileEntityValve> otherValves) {
        if (!this.master) {
            TileEntityValve masterValve = getMaster(valve);
            if (masterValve != null && masterValve != valve) {
                return masterValve.getAllValves();
            }
        }

        List<TileEntityValve> valves = new ArrayList<>();
        valves.add(valve);
        if (otherValves == null) {
            return valves;
        }

        for (TileEntityValve other : otherValves) {
            if (other != null && other != valve && !valves.contains(other)) {
                valves.add(other);
            }
        }

        return valves;
    }

    public List<TileEntityValve> getValvesByName(TileEntityValve valve, List<TileEntityValve> otherValves,
        String name) {
        if (!this.master) {
            TileEntityValve masterValve = getMaster(valve);
            return masterValve == null ? new ArrayList<>() : masterValve.getValvesByName(name);
        }

        List<TileEntityValve> valves = new ArrayList<>();
        for (TileEntityValve other : getAllValves(valve, otherValves)) {
            if (other.getValveName()
                .toLowerCase()
                .equals(name.toLowerCase())) {
                valves.add(other);
            }
        }

        return valves;
    }
}
