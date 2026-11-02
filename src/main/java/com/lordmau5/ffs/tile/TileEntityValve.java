package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.List;

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
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.LongSet;
import com.lordmau5.ffs.util.Position3D;
import com.lordmau5.ffs.util.TankMath;
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
    public boolean initiated;
    public int tankHeight = 0;
    public int valveHeightPosition = 0;
    private final AutoOutputState autoOutput = new AutoOutputState();
    private final TankOwnership ownership = new TankOwnership();
    private int randomBurnTicks = 100;
    private int randomLeakTicks = 1200;
    private long lastRainTick = -1L;
    private int cachedPrecipHeight = -1;
    private long lastPrecipCheck = -1L;
    private boolean building;
    private ForgeDirection inside = ForgeDirection.UNKNOWN;
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
        return this.ownership.getAllValves(this, this.otherValves);
    }

    List<TileEntityValve> getValvesByName(String name) {
        return this.ownership.getValvesByName(this, this.otherValves, name);
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
        TankBuilder.build(this, inside);
    }

    public boolean calculateInside() {
        return TankBuilder.calculateInside(this);
    }

    public void updateCornerFrames() {
        TankBuilder.updateCornerFrames(this);
    }

    public void breakTank(TileEntity frame) {
        TankBuilder.breakTank(this, frame);
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

    @Override
    public void invalidate() {
        super.invalidate();
        this.ownership.invalidate();
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
        return this.ownership.isMaster();
    }

    public TileEntityValve getMaster() {
        return this.ownership.getMaster(this);
    }

    void setMaster(boolean master) {
        this.ownership.setMaster(master);
    }

    public void setMasterPos(Position3D masterValvePos) {
        this.ownership.setMasterPos(masterValvePos);
    }

    Position3D getMasterValvePos() {
        return this.ownership.getMasterValvePos();
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

    int getMaxSize() {
        return this.maxSize;
    }

    int[] getLength() {
        return this.length;
    }

    boolean isBuilding() {
        return this.building;
    }

    void setBuilding(boolean building) {
        this.building = building;
    }

    void setInteriorPositions(LongSet interiorPositions) {
        this.interiorPositions = interiorPositions;
    }

    void setFluidCapacityInternal(int fluidCapacity) {
        this.fluidCapacity = fluidCapacity;
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
        return ValveCommandAdapter.METHOD_NAMES.clone();
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
        if (method < 0 || method >= ValveCommandAdapter.METHOD_NAMES.length) {
            return null;
        }

        try {
            return ValveCommandAdapter
                .execute(this, ValveCommandAdapter.METHOD_NAMES[method], new ValveCommandAdapter.ObjectArgs(arguments));
        } catch (ValveCommandAdapter.CommandException e) {
            throw new LuaException(e.getMessage());
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
    public Object[] invoke(String method, Context context, final Arguments ocArgs) throws Exception {
        ValveCommandAdapter.Args args = new ValveCommandAdapter.Args() {

            @Override
            public int count() {
                return ocArgs.count();
            }

            @Override
            public boolean isBoolean(int index) {
                return ocArgs.isBoolean(index);
            }

            @Override
            public boolean getBoolean(int index) {
                return ocArgs.checkBoolean(index);
            }

            @Override
            public boolean isString(int index) {
                return ocArgs.isString(index);
            }

            @Override
            public String getString(int index) {
                return ocArgs.checkString(index);
            }

            @Override
            public Object get(int index) {
                return ocArgs.checkAny(index);
            }
        };

        try {
            return ValveCommandAdapter.execute(this, method, args);
        } catch (ValveCommandAdapter.CommandException e) {
            throw new Exception(e.getMessage());
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
