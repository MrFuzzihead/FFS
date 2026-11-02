package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.LongSet;
import com.lordmau5.ffs.util.Position3D;
import com.lordmau5.ffs.util.TankGeometry;
import com.lordmau5.ffs.util.TankMath;
import com.lordmau5.ffs.util.TankStructure;
import com.rwtema.funkylocomotion.api.IMoveCheck;

import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.IPipeTile.PipeType;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.common.Optional.Method;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.SimpleComponent;

@InterfaceList({ @Interface(iface = "buildcraft.api.transport.IPipeConnection", modid = "BuildCraftAPI|Transport"),
    @Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"),
    @Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers"),
    @Interface(iface = "li.cil.oc.api.network.ManagedPeripheral", modid = "OpenComputers"),
    @Interface(iface = "com.rwtema.funkylocomotion.api.IMoveCheck", modid = "funkylocomotion") })
public class TileEntityValve extends TileEntity
    implements IFluidTank, IFluidHandler, IPipeConnection, IPeripheral, SimpleComponent, ManagedPeripheral, IMoveCheck {

    private final int maxSize = FancyFluidStorage.instance.MAX_SIZE;
    protected int mbPerVirtualTank = FancyFluidStorage.instance.MB_PER_TANK_BLOCK;
    protected int minBurnableTemp = FancyFluidStorage.instance.MIN_BURNABLE_TEMPERATURE;
    private int frameBurnability = 0;
    private String valveName = "";
    public boolean isValid;
    private boolean isMaster;
    private Position3D masterValvePos;
    public boolean initiated;
    public int tankHeight = 0;
    public int valveHeightPosition = 0;
    private final AutoOutputState autoOutput = new AutoOutputState();
    private int randomBurnTicks = 100;
    private int randomLeakTicks = 1200;
    private long lastRainTick = -1L;
    private int cachedPrecipHeight = -1;
    private long lastPrecipCheck = -1L;
    private boolean building;
    private ForgeDirection inside = ForgeDirection.UNKNOWN;
    private TileEntityValve master;
    public List<TileEntityTankFrame> tankFrames;
    public List<TileEntityValve> otherValves;
    /** Packed interior positions, relative to {@link #bottomDiagFrame}. */
    private LongSet interiorPositions;
    private int[] length = new int[6];
    public Position3D bottomDiagFrame;
    public Position3D topDiagFrame;
    public int initialWaitTick = 20;
    private FluidStack fluidStack;
    private int fluidTemperature = 0;
    private int fluidCapacity = 0;
    private int lastComparatorOut = 0;

    public TileEntityValve() {
        this.tankFrames = new ArrayList<>();
        this.otherValves = new ArrayList<>();
    }

    public void validate() {
        super.validate();
        this.initiated = true;
        this.initialWaitTick = 20;
    }

    public void updateEntity() {
        if (!this.worldObj.isRemote) {
            if (this.autoOutput.isPendingSync()) {
                TileEntityValve master = this.getMaster();
                long now = this.worldObj.getTotalWorldTime();
                if (master != null && this.autoOutput.consumeSync(now, 2)) {
                    // Throttle fluid sync: pipes can change the tank every tick, but the
                    // client only needs a handful of updates per second.
                    master.markForUpdate(true);
                }
            }

            if (this.initiated && this.isMaster()) {
                if (this.bottomDiagFrame != null && this.topDiagFrame != null) {
                    Chunk chunkBottom = this.worldObj
                        .getChunkFromBlockCoords(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getZ());
                    Chunk chunkTop = this.worldObj
                        .getChunkFromBlockCoords(this.topDiagFrame.getX(), this.topDiagFrame.getZ());
                    int minChunkX = Math.min(chunkBottom.xPosition, chunkTop.xPosition);
                    int maxChunkX = Math.max(chunkBottom.xPosition, chunkTop.xPosition);
                    int minChunkZ = Math.min(chunkBottom.zPosition, chunkTop.zPosition);
                    int maxChunkZ = Math.max(chunkBottom.zPosition, chunkTop.zPosition);

                    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                            this.worldObj.getChunkProvider()
                                .loadChunk(chunkX, chunkZ);
                        }
                    }
                }

                if (this.initialWaitTick-- <= 0) {
                    this.initiated = false;
                    this.buildTank(this.inside);
                    return;
                }
            }

            if (this.isValid()) {
                if (this.getFluid() != null) {
                    TankAutoOutput.tick(this);

                    TankHazards.tickRain(this);

                    if (this.isMaster()) {
                        TankHazards.tickBurn(this);
                        TankHazards.tickLeak(this);
                    }
                }
            }
        }
    }

    List<TileEntityValve> getAllValves() {
        if (!this.isMaster()) {
            TileEntityValve master = this.getMaster();
            if (master != null && master != this) {
                return master.getAllValves();
            }
        }

        List<TileEntityValve> valves = new ArrayList<>();
        valves.add(this);
        if (this.otherValves == null) {
            return valves;
        }

        for (TileEntityValve valve : this.otherValves) {
            if (valve != null && valve != this && !valves.contains(valve)) {
                valves.add(valve);
            }
        }

        return valves;
    }

    List<TileEntityValve> getValvesByName(String name) {
        if (!this.isMaster()) {
            TileEntityValve master = this.getMaster();
            return master == null ? new ArrayList<>() : master.getValvesByName(name);
        } else {
            List<TileEntityValve> valves = new ArrayList<>();
            if (this.getAllValves()
                .isEmpty()) {
                return valves;
            } else {
                for (TileEntityValve valve : this.getAllValves()) {
                    if (valve.getValveName()
                        .toLowerCase()
                        .equals(name.toLowerCase())) {
                        valves.add(valve);
                    }
                }

                return valves;
            }
        }
    }

    public String getValveName() {
        if (this.valveName.isEmpty()) {
            this.setValveName(GenericUtil.getUniqueValveName(this));
        }

        return this.valveName;
    }

    public void setValveName(String valveName) {
        this.valveName = valveName;
    }

    public void setNeedsUpdate() {
        this.autoOutput.requestSync();
    }

    public int getTankHeight() {
        if (this.isMaster()) {
            return this.tankHeight;
        }

        TileEntityValve master = this.getMaster();
        return master == null ? 0 : master.tankHeight;
    }

    public void setInside(ForgeDirection inside) {
        this.inside = inside;
    }

    public ForgeDirection getInside() {
        return this.inside;
    }

    public void buildTank(ForgeDirection inside) {
        if (!this.worldObj.isRemote) {
            // Drop the old slaves first; otherwise they keep pointing at this valve
            // and report themselves valid for the rebuilt tank.
            this.invalidatePreviousSlaves();
            this.setValid(false);
            this.fluidCapacity = 0;
            this.interiorPositions = null;
            this.tankFrames.clear();
            this.otherValves.clear();
            if (inside != null) {
                this.setInside(inside);
            }

            this.building = true;
            try {
                if (this.calculateInside() && this.setupTank()) {
                    this.initiated = false;
                    this.updateBlockAndNeighbors();
                }
            } finally {
                this.building = false;
            }
        }
    }

    public boolean calculateInside() {
        TankGeometry.Shape shape = TankGeometry.measure(
            (x, y, z) -> this.worldObj.isAirBlock(x, y, z),
            this.xCoord,
            this.yCoord,
            this.zCoord,
            this.inside,
            this.maxSize);
        if (!shape.valid) {
            return false;
        }

        System.arraycopy(shape.lengths, 0, this.length, 0, this.length.length);
        this.bottomDiagFrame = shape.bottomDiag;
        this.topDiagFrame = shape.topDiag;
        this.tankHeight = shape.tankHeight;
        return true;
    }

    private void setSlaveValveInside(LongSet interior, TileEntityValve slave) {
        List<Position3D> possibleAirBlocks = new ArrayList<>();

        for (ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            if (this.worldObj
                .isAirBlock(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ)) {
                possibleAirBlocks.add(
                    new Position3D(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ));
            }
        }

        Position3D insideAir = null;

        for (Position3D pos : possibleAirBlocks) {
            if (interior.contains(
                LongSet.pack(
                    pos.getX() - this.bottomDiagFrame.getX(),
                    pos.getY() - this.bottomDiagFrame.getY(),
                    pos.getZ() - this.bottomDiagFrame.getZ()))) {
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

    public void updateCornerFrames() {
        this.bottomDiagFrame = new Position3D(
            this.xCoord + this.inside.offsetX
                + this.length[ForgeDirection.WEST.ordinal()] * ForgeDirection.WEST.offsetX
                + ForgeDirection.WEST.offsetX,
            this.yCoord + this.inside.offsetY
                + this.length[ForgeDirection.DOWN.ordinal()] * ForgeDirection.DOWN.offsetY
                + ForgeDirection.DOWN.offsetY,
            this.zCoord + this.inside.offsetZ
                + this.length[ForgeDirection.NORTH.ordinal()] * ForgeDirection.NORTH.offsetZ
                + ForgeDirection.NORTH.offsetZ);
        this.topDiagFrame = new Position3D(
            this.xCoord + this.inside.offsetX
                + this.length[ForgeDirection.EAST.ordinal()] * ForgeDirection.EAST.offsetX
                + ForgeDirection.EAST.offsetX,
            this.yCoord + this.inside.offsetY
                + this.length[ForgeDirection.UP.ordinal()] * ForgeDirection.UP.offsetY
                + ForgeDirection.UP.offsetY,
            this.zCoord + this.inside.offsetZ
                + this.length[ForgeDirection.SOUTH.ordinal()] * ForgeDirection.SOUTH.offsetZ
                + ForgeDirection.SOUTH.offsetZ);
    }

    private boolean setupTank() {
        this.updateCornerFrames();
        TankStructure structure = TankStructure.scan(this.worldObj, this.bottomDiagFrame, this.topDiagFrame);
        if (!structure.validBounds) {
            return false;
        }
        this.otherValves = new ArrayList<>();
        this.tankFrames = new ArrayList<>();
        Position3D pos = new Position3D(this.xCoord, this.yCoord, this.zCoord);
        this.valveHeightPosition = Math.abs(
            this.bottomDiagFrame.getDistance(pos)
                .getY());
        this.tankHeight = this.topDiagFrame.getDistance(this.bottomDiagFrame)
            .getY() - 1;
        ExtendedBlock bottomDiagBlock = new ExtendedBlock(
            this.worldObj
                .getBlock(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ()),
            this.worldObj.getBlockMetadata(
                this.bottomDiagFrame.getX(),
                this.bottomDiagFrame.getY(),
                this.bottomDiagFrame.getZ()));
        ExtendedBlock topDiagBlock = new ExtendedBlock(
            this.worldObj.getBlock(this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ()),
            this.worldObj
                .getBlockMetadata(this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ()));
        this.frameBurnability = bottomDiagBlock.getBlock()
            .getFlammability(
                this.worldObj,
                this.bottomDiagFrame.getX(),
                this.bottomDiagFrame.getY(),
                this.bottomDiagFrame.getZ(),
                ForgeDirection.UNKNOWN);
        if (bottomDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = this.worldObj
                .getTileEntity(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ());
            if (tile != null && tile instanceof TileEntityTankFrame) {
                ExtendedBlock stored = ((TileEntityTankFrame) tile).getBlock();
                if (stored == null || !stored.hasBlock()) {
                    return false;
                }
                bottomDiagBlock = stored;
            }
        }

        if (topDiagBlock.getBlock() instanceof BlockTankFrame) {
            TileEntity tile = this.worldObj
                .getTileEntity(this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ());
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

        if (!GenericUtil.isValidTankBlock(this.worldObj, this.bottomDiagFrame, bottomDiagBlock)) {
            return false;
        } else
            if (!GenericUtil.areTankBlocksValid(bottomDiagBlock, topDiagBlock, this.worldObj, this.bottomDiagFrame)) {
                return false;
            } else {
                if (!structure.interiorAirValid) {
                    return false;
                }

                this.fluidCapacity = TankMath.capacityInt(
                    FancyFluidStorage.instance.INSIDE_CAPACITY,
                    structure.edges.size(),
                    structure.faces.size(),
                    structure.interiorCount,
                    this.mbPerVirtualTank);

                for (TankStructure.ShellCell cell : structure.edges) {
                    Position3D fPos = cell.pos;
                    ExtendedBlock fBlock = cell.block;
                    if (!fBlock.hasBlock()) {
                        return false;
                    }

                    int burnability = fBlock.getBlock()
                        .getFlammability(this.worldObj, fPos.getX(), fPos.getY(), fPos.getZ(), ForgeDirection.UNKNOWN);
                    if (burnability > this.frameBurnability) {
                        this.frameBurnability = burnability;
                    }

                    if (fBlock.getBlock() instanceof BlockTankFrame) {
                        TileEntity tile = this.worldObj.getTileEntity(fPos.getX(), fPos.getY(), fPos.getZ());
                        if (tile != null && tile instanceof TileEntityTankFrame) {
                            fBlock = ((TileEntityTankFrame) tile).getBlock();
                        }
                    }

                    if (fBlock == null || !fBlock.hasBlock()) {
                        return false;
                    }

                    if (!GenericUtil.areTankBlocksValid(fBlock, bottomDiagBlock, this.worldObj, fPos)) {
                        return false;
                    }
                }

                List<TileEntityValve> valves = new ArrayList<>();

                for (TankStructure.ShellCell cell : structure.faces) {
                    pos = cell.pos;
                    ExtendedBlock check = cell.block;
                    int burnabilityx = check.getBlock()
                        .getFlammability(this.worldObj, pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN);
                    if (burnabilityx > this.frameBurnability) {
                        this.frameBurnability = burnabilityx;
                    }

                    if (!GenericUtil.areTankBlocksValid(check, bottomDiagBlock, this.worldObj, pos)
                        && !GenericUtil.isBlockGlass(check.getBlock(), check.getMetadata())) {
                        TileEntity tile = this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tile == null) {
                            return false;
                        }

                        if (tile instanceof TileEntityValve) {
                            TileEntityValve valve = (TileEntityValve) tile;
                            if (valve != this) {
                                TileEntityValve owner = valve.getMaster();
                                if (owner != null && owner != this && owner.isValid()) {
                                    // This valve already belongs to another live tank;
                                    // absorbing it would merge/steal that tank.
                                    return false;
                                }
                                if (valve.fluidStack != null) {
                                    this.fluidStack = valve.fluidStack;
                                    this.updateFluidTemperature();
                                }

                                valves.add(valve);
                            }
                        } else if (!(tile instanceof TileEntityTankFrame)) {
                            return false;
                        }
                    }
                }

                if (this.fluidStack != null) {
                    this.fluidStack.amount = Math.min(this.fluidStack.amount, this.fluidCapacity);
                    if (this.fluidStack.amount <= 0) {
                        this.fluidStack = null;
                    }
                }

                for (TileEntityValve valve : valves) {
                    pos = new Position3D(valve.xCoord, valve.yCoord, valve.zCoord);
                    valve.invalidatePreviousSlaves();
                    valve.setMaster(false);
                    valve.valveHeightPosition = Math.abs(
                        this.bottomDiagFrame.getDistance(pos)
                            .getY());
                    valve.setMasterPos(new Position3D(this.xCoord, this.yCoord, this.zCoord));
                    this.setSlaveValveInside(structure.interior, valve);
                }

                this.setMaster(true);

                for (TankStructure.ShellCell setTiles : structure.edges) {
                    pos = setTiles.pos;
                    TileEntityTankFrame tankFrame;
                    if (setTiles.block.getBlock() != FancyFluidStorage.blockTankFrame) {
                        this.worldObj.setBlock(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            FancyFluidStorage.blockTankFrame,
                            setTiles.block.getMetadata(),
                            2);
                        tankFrame = (TileEntityTankFrame) this.worldObj
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null) {
                            return false;
                        }
                        tankFrame.initialize(this, setTiles.block);
                    } else {
                        tankFrame = (TileEntityTankFrame) this.worldObj
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null || tankFrame.isOwnedByOther(this)) {
                            return false;
                        }
                        if (tankFrame.setValvePos(new Position3D(this.xCoord, this.yCoord, this.zCoord))) {
                            tankFrame.markForUpdate();
                        }
                    }

                    this.tankFrames.add(tankFrame);
                }

                for (TankStructure.ShellCell setTiles : structure.faces) {
                    pos = setTiles.pos;
                    TileEntity tilex = this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                    if (tilex != null) {
                        if (tilex instanceof TileEntityValve && tilex != this) {
                            this.otherValves.add((TileEntityValve) tilex);
                        } else if (tilex instanceof TileEntityTankFrame) {
                            TileEntityTankFrame tankFrame = (TileEntityTankFrame) tilex;
                            if (tankFrame.isOwnedByOther(this)) {
                                return false;
                            }
                            if (tankFrame.setValvePos(new Position3D(this.xCoord, this.yCoord, this.zCoord))) {
                                tankFrame.markForUpdate();
                            }
                            this.tankFrames.add(tankFrame);
                        } else if (GenericUtil.isTileEntityAcceptable(setTiles.block.getBlock(), tilex)) {
                            this.worldObj.setBlock(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                FancyFluidStorage.blockTankFrame,
                                setTiles.block.getMetadata(),
                                2);
                            TileEntityTankFrame tankFrame = (TileEntityTankFrame) this.worldObj
                                .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                            if (tankFrame == null) {
                                return false;
                            }
                            tankFrame.initialize(this, setTiles.block);
                            this.tankFrames.add(tankFrame);
                        }
                    } else {
                        this.worldObj.setBlock(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            FancyFluidStorage.blockTankFrame,
                            setTiles.block.getMetadata(),
                            2);
                        TileEntityTankFrame tankFrame = (TileEntityTankFrame) this.worldObj
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null) {
                            return false;
                        }
                        tankFrame.initialize(this, setTiles.block);
                        this.tankFrames.add(tankFrame);
                    }
                }

                this.interiorPositions = structure.interior;
                this.setValid(true);
                return true;
            }
    }

    public void breakTank(TileEntity frame) {
        if (!this.worldObj.isRemote) {
            if (!this.isMaster() && this.getMaster() != null) {
                if (this.getMaster() != this) {
                    this.getMaster()
                        .breakTank(frame);
                }
            } else {
                // Capture the fluid before invalidating: getFluid() returns null once
                // isValid is false, which silently destroyed the hand-off.
                FluidStack fluid = this.fluidStack;
                this.setValid(false);

                for (TileEntityValve valve : this.otherValves) {
                    valve.fluidStack = fluid;
                    valve.updateFluidTemperature();
                    valve.master = null;
                    valve.setValid(false);
                    valve.updateBlockAndNeighbors();
                }

                for (TileEntityTankFrame tankFrame : this.tankFrames) {
                    if (frame != tankFrame) {
                        tankFrame.breakFrame();
                    }
                }

                this.tankFrames.clear();
                this.otherValves.clear();
                this.interiorPositions = null;
                this.updateBlockAndNeighbors();
            }
        }
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    /** Called by the shell blocks when a neighbour changes. */
    public void onNeighborChangedAt(int x, int y, int z) {
        if (this.worldObj == null || this.worldObj.isRemote || this.building) {
            return;
        }

        TileEntityValve master = this.getMaster();
        if (master == null || master.building
            || !master.isValid()
            || master.interiorPositions == null
            || master.bottomDiagFrame == null) {
            return;
        }

        // A non-air block inside the interior invalidates the tank immediately
        // instead of waiting for the next chunk load / manual rebuild.
        if (master.interiorPositions.contains(
            LongSet.pack(
                x - master.bottomDiagFrame.getX(),
                y - master.bottomDiagFrame.getY(),
                z - master.bottomDiagFrame.getZ()))
            && !master.worldObj.isAirBlock(x, y, z)) {
            master.breakTank(null);
        }
    }

    private void invalidatePreviousSlaves() {
        if (this.otherValves == null) {
            return;
        }

        for (TileEntityValve valve : this.otherValves) {
            if (valve == null || valve == this) {
                continue;
            }

            valve.masterValvePos = null;
            valve.master = null;
            valve.setMaster(false);
            valve.setValid(false);
            valve.updateBlockAndNeighbors();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.master = null;
        if (this.tankFrames != null) {
            this.tankFrames.clear();
        }
        if (this.otherValves != null) {
            this.otherValves.clear();
        }
    }

    public boolean isValid() {
        TileEntityValve master = this.getMaster();
        return master != null && master.isValid;
    }

    public void updateBlockAndNeighbors() {
        this.updateBlockAndNeighbors(false);
    }

    private void markForUpdate(boolean onlyThis) {
        if (!onlyThis || this.lastComparatorOut != this.getComparatorOutput()) {
            this.lastComparatorOut = this.getComparatorOutput();

            for (TileEntityValve valve : this.otherValves) {
                valve.updateBlockAndNeighbors();
            }
        }

        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    public void updateBlockAndNeighbors(boolean onlyThis) {
        if (!this.worldObj.isRemote) {
            this.markForUpdate(onlyThis);
            if (this.otherValves != null) {
                for (TileEntityValve otherValve : this.otherValves) {
                    otherValve.isValid = this.isValid;
                    otherValve.markForUpdate(true);
                }
            }

            ForgeDirection outside = this.getInside()
                .getOpposite();
            TileEntity outsideTile = this.worldObj.getTileEntity(
                this.xCoord + outside.offsetX,
                this.yCoord + outside.offsetY,
                this.zCoord + outside.offsetZ);
            if (outsideTile != null && FancyFluidStorage.proxy.BUILDCRAFT_LOADED && outsideTile instanceof IPipeTile) {
                ((IPipeTile) outsideTile).scheduleNeighborChange();
            }

            this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, FancyFluidStorage.blockValve);
            this.worldObj.markBlockForUpdate(
                this.xCoord + outside.offsetX,
                this.yCoord + outside.offsetY,
                this.zCoord + outside.offsetZ);
        }
    }

    public boolean isMaster() {
        return this.isMaster;
    }

    public TileEntityValve getMaster() {
        if (this.isMaster()) {
            return this;
        }

        if (this.master != null && !this.master.isInvalid()) {
            return this.master;
        }
        this.master = null;

        if (this.masterValvePos != null && this.worldObj != null) {
            TileEntity tile = this.worldObj
                .getTileEntity(this.masterValvePos.getX(), this.masterValvePos.getY(), this.masterValvePos.getZ());
            this.master = tile instanceof TileEntityValve ? (TileEntityValve) tile : null;
        }

        return this.master;
    }

    void setMaster(boolean master) {
        this.isMaster = master;
        if (master) {
            this.masterValvePos = null;
            this.master = null;
        }
    }

    public void setMasterPos(Position3D masterValvePos) {
        this.masterValvePos = masterValvePos;
        this.master = null;
    }

    public boolean getAutoOutput() {
        if (!this.isValid()) {
            return false;
        }

        TileEntityValve master = this.getMaster();
        return master != null && master.autoOutput.isEnabled();
    }

    boolean getAutoOutputFlag() {
        return this.autoOutput.isEnabled();
    }

    void setAutoOutputFlag(boolean enabled) {
        this.autoOutput.setEnabled(enabled);
    }

    public void setAutoOutput(boolean autoOutput) {
        TileEntityValve master = this.getMaster();
        if (master != null && master != this) {
            master.setAutoOutput(autoOutput);
            return;
        }

        this.autoOutput.setEnabled(autoOutput);
        if (this.worldObj != null) {
            this.updateBlockAndNeighbors(true);
        }
    }

    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        ValveNbt.apply(this, tag, GenericUtil.getUniqueValveName(this));
    }

    public void writeToNBT(NBTTagCompound tag) {
        ValveNbt.writeFrom(this, tag);
        super.writeToNBT(tag);
    }

    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        ValveNbt.apply(this, pkt.func_148857_g(), GenericUtil.getUniqueValveName(this));
    }

    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        ValveNbt.writeFrom(this, tag);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, tag);
    }

    public AxisAlignedBB getRenderBoundingBox() {
        if (!this.isMaster() || this.bottomDiagFrame == null || this.topDiagFrame == null) {
            return super.getRenderBoundingBox();
        }

        return AxisAlignedBB.getBoundingBox(
            this.bottomDiagFrame.getX(),
            this.bottomDiagFrame.getY(),
            this.bottomDiagFrame.getZ(),
            this.topDiagFrame.getX() + 1,
            this.topDiagFrame.getY() + 1,
            this.topDiagFrame.getZ() + 1);
    }

    public int getFluidLuminosity() {
        FluidStack fstack = this.getFluid();
        if (fstack == null) {
            return 0;
        } else {
            Fluid fluid = fstack.getFluid();
            return fluid == null ? 0 : fluid.getLuminosity(fstack);
        }
    }

    public void updateFluidTemperature() {
        FluidStack fstack = this.fluidStack;
        if (fstack != null) {
            Fluid fluid = fstack.getFluid();
            if (fluid != null) {
                this.fluidTemperature = fluid.getTemperature(fstack);
            }
        }
    }

    public FluidStack getFluid() {
        TileEntityValve master = this.getMaster();
        if (master == null || !master.isValid) {
            return null;
        }
        return master.fluidStack;
    }

    public int getFluidAmount() {
        FluidStack fluid = this.getFluid();
        return fluid == null ? 0 : fluid.amount;
    }

    public int getCapacity() {
        TileEntityValve master = this.getMaster();
        return master == null || !master.isValid ? 0 : master.fluidCapacity;
    }

    public FluidTankInfo getInfo() {
        return !this.isValid() ? null : new FluidTankInfo(this.getMaster());
    }

    FluidStack getFluidStackInternal() {
        return this.fluidStack;
    }

    void setFluidStackInternal(FluidStack fluidStack) {
        this.fluidStack = fluidStack;
    }

    int getFluidCapacityInternal() {
        return this.fluidCapacity;
    }

    void setFluidCapacityInternal(int fluidCapacity) {
        this.fluidCapacity = fluidCapacity;
    }

    Position3D getMasterValvePos() {
        return this.masterValvePos;
    }

    int getFluidTemperature() {
        return this.fluidTemperature;
    }

    int getMinBurnableTemp() {
        return this.minBurnableTemp;
    }

    int getFrameBurnability() {
        return this.frameBurnability;
    }

    void setFrameBurnability(int frameBurnability) {
        this.frameBurnability = frameBurnability;
    }

    int getRandomBurnTicks() {
        return this.randomBurnTicks;
    }

    void setRandomBurnTicks(int randomBurnTicks) {
        this.randomBurnTicks = randomBurnTicks;
    }

    int getRandomLeakTicks() {
        return this.randomLeakTicks;
    }

    void setRandomLeakTicks(int randomLeakTicks) {
        this.randomLeakTicks = randomLeakTicks;
    }

    long getLastRainTick() {
        return this.lastRainTick;
    }

    void setLastRainTick(long lastRainTick) {
        this.lastRainTick = lastRainTick;
    }

    int getCachedPrecipHeight() {
        return this.cachedPrecipHeight;
    }

    void setCachedPrecipHeight(int cachedPrecipHeight) {
        this.cachedPrecipHeight = cachedPrecipHeight;
    }

    long getLastPrecipCheck() {
        return this.lastPrecipCheck;
    }

    void setLastPrecipCheck(long lastPrecipCheck) {
        this.lastPrecipCheck = lastPrecipCheck;
    }

    LongSet getInteriorPositions() {
        return this.interiorPositions;
    }

    public int fill(FluidStack resource, boolean doFill) {
        return TankFluidHandler.fill(this, resource, doFill);
    }

    public FluidStack drain(int maxDrain, boolean doDrain) {
        return TankFluidHandler.drain(this, maxDrain, doDrain);
    }

    public double getFillPercentage() {
        return TankMath.fillPercentage(this.getFluidAmount(), this.getCapacity());
    }

    public int fillFromContainer(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null) {
            return 0;
        }

        if (!this.canFillIncludingContainers(from, resource.getFluid())) {
            return 0;
        } else {
            return this.getMaster() == this ? this.fill(resource, doFill)
                : this.getMaster()
                    .fill(resource, doFill);
        }
    }

    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null) {
            return 0;
        }

        if (!this.canFill(from, resource.getFluid())) {
            return 0;
        } else {
            return this.getMaster() == this ? this.fill(resource, doFill)
                : this.getMaster()
                    .fill(resource, doFill);
        }
    }

    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.getFluid() == null) {
            return null;
        }

        FluidStack stored = this.getFluid();
        if (stored == null || !stored.isFluidEqual(resource)) {
            return null;
        }

        return this.getMaster() == this ? this.drain(resource.amount, doDrain)
            : this.getMaster()
                .drain(resource.amount, doDrain);
    }

    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return this.getMaster() == this ? this.drain(maxDrain, doDrain)
            : this.getMaster()
                .drain(maxDrain, doDrain);
    }

    public boolean canFillIncludingContainers(ForgeDirection from, Fluid fluid) {
        return TankFluidHandler.canFillIncludingContainers(this, from, fluid);
    }

    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return TankFluidHandler.canFill(this, from, fluid);
    }

    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return TankFluidHandler.canDrain(this, from, fluid);
    }

    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        if (!this.isValid()) {
            return new FluidTankInfo[0];
        } else {
            return this.getMaster() == this ? new FluidTankInfo[] { this.getInfo() }
                : this.getMaster()
                    .getTankInfo(from);
        }
    }

    @Method(modid = "BuildCraftAPI|Transport")
    public ConnectOverride overridePipeConnection(PipeType pipeType, ForgeDirection from) {
        return !this.isValid() ? ConnectOverride.DISCONNECT : ConnectOverride.CONNECT;
    }

    public String[] methodNames() {
        return new String[] { "getFluidName", "getFluidAmount", "getFluidCapacity", "setAutoOutput", "doesAutoOutput" };
    }

    @Method(modid = "ComputerCraft")
    public String getType() {
        return "ffs_valve";
    }

    @Method(modid = "ComputerCraft")
    public String[] getMethodNames() {
        return this.methodNames();
    }

    @Method(modid = "ComputerCraft")
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
        throws LuaException, InterruptedException {
        switch (method) {
            case 0:
                String fluid = ValveCommands.fluidName(this);
                return fluid == null ? null : new Object[] { fluid };
            case 1:
                return new Object[] { ValveCommands.fluidAmount(this) };
            case 2:
                return new Object[] { ValveCommands.fluidCapacity(this) };
            case 3:
                if (arguments.length == 1) {
                    if (!(arguments[0] instanceof Boolean)) {
                        throw new LuaException(
                            "expected argument 1 to be of type \"boolean\", found \"" + arguments[0].getClass()
                                .getSimpleName() + "\"");
                    }

                    boolean value = (Boolean) arguments[0];
                    ValveCommands.setAutoOutput(this, value);
                    return new Object[] { value };
                } else if (arguments.length != 2) {
                    throw new LuaException(
                        "insufficient number of arguments found - expected 1 or 2, got " + arguments.length);
                } else if (!(arguments[0] instanceof String)) {
                    throw new LuaException(
                        "expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass()
                            .getSimpleName() + "\"");
                } else if (!(arguments[1] instanceof Boolean)) {
                    throw new LuaException(
                        "expected argument 2 to be of type \"boolean\", found \"" + arguments[1].getClass()
                            .getSimpleName() + "\"");
                }

                List<String> valveNames = ValveCommands
                    .setAutoOutputByName(this, (String) arguments[0], (Boolean) arguments[1]);
                if (valveNames == null) {
                    throw new LuaException("no valves found");
                }

                return new Object[] { valveNames };
            case 4:
                if (arguments.length == 0) {
                    return new Object[] { ValveCommands.autoOutputs(this, null) };
                } else if (arguments.length != 1) {
                    throw new LuaException(
                        "insufficient number of arguments found - expected 1, got " + arguments.length);
                } else if (!(arguments[0] instanceof String)) {
                    throw new LuaException(
                        "expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass()
                            .getSimpleName() + "\"");
                }

                Map<String, Boolean> valveOutputs = ValveCommands.autoOutputs(this, (String) arguments[0]);
                if (valveOutputs == null) {
                    throw new LuaException("no valves found");
                }

                return new Object[] { valveOutputs };
            default:
                return null;
        }
    }

    @Method(modid = "ComputerCraft")
    public void attach(IComputerAccess computer) {}

    @Method(modid = "ComputerCraft")
    public void detach(IComputerAccess computer) {}

    @Method(modid = "ComputerCraft")
    public boolean equals(IPeripheral other) {
        return other == this;
    }

    @Method(modid = "OpenComputers")
    public String getComponentName() {
        return "ffs_valve";
    }

    @Method(modid = "OpenComputers")
    public String[] methods() {
        return this.methodNames();
    }

    @Method(modid = "OpenComputers")
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        switch (method) {
            case "getFluidName":
                String fluid = ValveCommands.fluidName(this);
                return fluid == null ? null : new Object[] { fluid };
            case "getFluidAmount":
                return new Object[] { ValveCommands.fluidAmount(this) };
            case "getFluidCapacity":
                return new Object[] { ValveCommands.fluidCapacity(this) };
            case "setAutoOutput":
                if (args.count() == 1) {
                    if (!args.isBoolean(0)) {
                        throw new Exception(
                            "expected argument 1 to be of type \"boolean\", found \"" + args.checkAny(0)
                                .getClass()
                                .getSimpleName() + "\"");
                    }

                    boolean value = args.checkBoolean(0);
                    ValveCommands.setAutoOutput(this, value);
                    return new Object[] { value };
                } else if (args.count() != 2) {
                    throw new Exception(
                        "insufficient number of arguments found - expected 1 or 2, got " + args.count());
                } else if (!args.isString(0)) {
                    throw new Exception(
                        "expected argument 1 to be of type \"String\", found \"" + args.checkAny(0)
                            .getClass()
                            .getSimpleName() + "\"");
                } else if (!args.isBoolean(1)) {
                    throw new Exception(
                        "expected argument 2 to be of type \"boolean\", found \"" + args.checkAny(1)
                            .getClass()
                            .getSimpleName() + "\"");
                }

                List<String> valveNames = ValveCommands
                    .setAutoOutputByName(this, args.checkString(0), args.checkBoolean(1));
                if (valveNames == null) {
                    throw new Exception("no valves found");
                }

                return new Object[] { valveNames };
            case "doesAutoOutput":
                if (args.count() == 0) {
                    return new Object[] { ValveCommands.autoOutputs(this, null) };
                } else if (args.count() != 1) {
                    throw new Exception("insufficient number of arguments found - expected 1, got " + args.count());
                } else if (!args.isString(0)) {
                    throw new Exception(
                        "expected argument 1 to be of type \"String\", found \"" + args.checkAny(0)
                            .getClass()
                            .getSimpleName() + "\"");
                }

                Map<String, Boolean> valveOutputs = ValveCommands.autoOutputs(this, args.checkString(0));
                if (valveOutputs == null) {
                    throw new Exception("no valves found");
                }

                return new Object[] { valveOutputs };
            default:
                return null;
        }
    }

    @Method(modid = "funkylocomotion")
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }

    public int getComparatorOutput() {
        return TankMath.comparatorOutput(this.getFluidAmount(), this.getCapacity());
    }
}
