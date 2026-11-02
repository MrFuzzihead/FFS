package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.LongSet;
import com.lordmau5.ffs.util.ModNames;
import com.lordmau5.ffs.util.NbtHelper;
import com.lordmau5.ffs.util.Position3D;
import com.lordmau5.ffs.util.TankGeometry;
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
    private boolean isMaster;
    private Position3D masterValvePos;
    public boolean initiated;
    private boolean needsUpdate;
    public int tankHeight = 0;
    public int valveHeightPosition = 0;
    private boolean autoOutput;
    private int randomBurnTicks = 100;
    private int randomLeakTicks = 1200;
    private long lastRainTick = -1L;
    private long lastFluidSyncTick = -1L;
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
            if (this.needsUpdate) {
                TileEntityValve master = this.getMaster();
                long now = this.worldObj.getTotalWorldTime();
                if (master != null && TankMath.shouldSyncFluid(now, this.lastFluidSyncTick, 2)) {
                    // Throttle fluid sync: pipes can change the tank every tick, but the
                    // client only needs a handful of updates per second.
                    master.markForUpdate(true);
                    this.lastFluidSyncTick = now;
                    this.needsUpdate = false;
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
                    if ((this.getAutoOutput() || this.valveHeightPosition == 0) && this.getFluidAmount() != 0) {
                        float height = (float) this.getFluidAmount() / this.getCapacity();
                        boolean isNegativeDensity = this.getFluid()
                            .getFluid()
                            .getDensity(this.getFluid()) < 0;
                        if (GenericUtil
                            .canAutoOutput(height, this.getTankHeight(), this.valveHeightPosition, isNegativeDensity)) {
                            ForgeDirection out = this.inside.getOpposite();
                            TileEntity tile = this.worldObj.getTileEntity(
                                this.xCoord + out.offsetX,
                                this.yCoord + out.offsetY,
                                this.zCoord + out.offsetZ);
                            if (tile != null && (tile instanceof TileEntityValve || this.getAutoOutput()
                                || this.valveHeightPosition != 0)) {
                                int maxAmount = 0;
                                if (tile instanceof TileEntityValve) {
                                    maxAmount = 1000;
                                } else if (tile instanceof IFluidHandler) {
                                    maxAmount = 50;
                                }

                                if (maxAmount != 0) {
                                    IFluidHandler handler = (IFluidHandler) tile;
                                    FluidStack fillStack = this.getFluid()
                                        .copy();
                                    fillStack.amount = Math.min(this.getFluidAmount(), maxAmount);
                                    int accepted = handler.fill(this.inside, fillStack, true);
                                    if (accepted > 0) {
                                        this.drain(accepted, true);
                                    }
                                }
                            }
                        }
                    }

                    if (this.getFluid() != null && this.getFluid()
                        .getFluid() == FluidRegistry.WATER && this.worldObj.isRaining()) {
                        TileEntityValve master = this.getMaster();
                        long worldTime = this.worldObj.getTotalWorldTime();
                        if (this.cachedPrecipHeight < 0 || worldTime - this.lastPrecipCheck >= 20) {
                            this.cachedPrecipHeight = this.worldObj.getPrecipitationHeight(this.xCoord, this.zCoord);
                            this.lastPrecipCheck = worldTime;
                        }
                        if (master != null && master.lastRainTick != worldTime
                            && this.yCoord == this.cachedPrecipHeight - 1) {
                            // Only one valve per tank may collect rain per tick; the old
                            // code added the full rate for every exposed valve.
                            master.lastRainTick = worldTime;
                            int rate = (int) Math.floor(
                                this.worldObj.rainingStrength * 5.0F
                                    * this.worldObj.getBiomeGenForCoords(this.xCoord, this.zCoord).rainfall);
                            FluidStack waterStack = this.getFluid()
                                .copy();
                            waterStack.amount = rate * 10;
                            this.fill(waterStack, true);
                        }
                    }

                    if (this.isMaster()) {
                        if (FancyFluidStorage.instance.SET_WORLD_ON_FIRE && this.minBurnableTemp > 0
                            && this.fluidTemperature >= this.minBurnableTemp
                            && this.frameBurnability > 0
                            && this.randomBurnTicks-- <= 0) {
                            this.randomBurnTicks = 100;
                            Random random = this.worldObj.rand;
                            int temperatureDiff = this.fluidTemperature - this.minBurnableTemp;
                            int chanceOfBurnability = 300 - this.frameBurnability;
                            int rand = random.nextInt(300) + temperatureDiff
                                + (int) Math.floor((float) this.getFluidAmount() / this.getCapacity() * 300.0F);
                            if (rand >= chanceOfBurnability) {
                                boolean successfullyBurned = false;
                                List<TileEntityTankFrame> remainingFrames = new ArrayList<>();
                                remainingFrames.addAll(this.tankFrames);
                                List<TileEntityTankFrame> removingFrames = new ArrayList<>();

                                while (!successfullyBurned && remainingFrames.size() != 0) {
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
                                    remainingFrames.addAll(this.tankFrames);
                                    List<Position3D> firePos = new ArrayList<>();
                                    int ix = 0;

                                    while (ix < 3 && remainingFrames.size() != 0) {
                                        int id = random.nextInt(remainingFrames.size());
                                        TileEntityTankFrame frame = remainingFrames.get(id);
                                        Block frameBlock = frame.getStoredBlock();
                                        if (frameBlock != null && frameBlock.isFlammable(
                                            this.worldObj,
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
                                        if (this.worldObj.getBlock(pos.getX(), pos.getY(), pos.getZ())
                                            .isFlammable(
                                                this.worldObj,
                                                pos.getX(),
                                                pos.getY(),
                                                pos.getZ(),
                                                ForgeDirection.UNKNOWN)) {
                                            this.worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), Blocks.fire);
                                        }
                                    }
                                }

                                this.frameBurnability = 0;
                                if (FancyFluidStorage.instance.SET_WORLD_ON_FIRE) {
                                    this.worldObj.playSoundEffect(
                                        this.xCoord + 0.5,
                                        this.yCoord + 0.5,
                                        this.zCoord + 0.5,
                                        ModNames.resource("fire"),
                                        1.0F,
                                        this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
                                }
                            }
                        }

                        if (FancyFluidStorage.instance.SHOULD_TANKS_LEAK && this.randomLeakTicks-- <= 0
                            && this.fluidStack != null
                            && this.fluidStack.getFluid()
                                .canBePlacedInWorld()) {
                            this.randomLeakTicks = 1200;
                            Random random = this.worldObj.rand;
                            int amt = random.nextInt(3) + 1;
                            List<TileEntityTankFrame> validFrames = new ArrayList<>();
                            List<TileEntityTankFrame> remainingFrames = new ArrayList<>();
                            remainingFrames.addAll(this.tankFrames);
                            int ix = 0;

                            while (ix < amt && remainingFrames.size() != 0) {
                                int id = random.nextInt(remainingFrames.size());
                                TileEntityTankFrame frame = remainingFrames.get(id);
                                Block block = frame.getStoredBlock();
                                if (block != null && GenericUtil.canBlockLeak(block)
                                    && !frame.getNeighborBlockOrAir(
                                        this.fluidStack.getFluid()
                                            .getBlock())
                                        .isEmpty()
                                    && TankMath.isValidLeakHardness(
                                        block.getBlockHardness(
                                            this.worldObj,
                                            frame.xCoord,
                                            frame.yCoord,
                                            frame.zCoord))) {
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
                                    block.getBlockHardness(this.worldObj, frame.xCoord, frame.yCoord, frame.zCoord));
                                int rand = random.nextInt(hardness) + 1;
                                int diff = (int) Math
                                    .ceil(50.0F * ((float) this.getFluidAmount() / this.getCapacity()));
                                if (rand >= hardness - diff) {
                                    List<ForgeDirection> dirs = frame.getNeighborBlockOrAir(
                                        this.fluidStack.getFluid()
                                            .getBlock());
                                    if (dirs.size() != 0) {
                                        ForgeDirection leakDir;
                                        if (dirs.size() > 1) {
                                            leakDir = dirs.get(random.nextInt(dirs.size()));
                                        } else {
                                            leakDir = dirs.get(0);
                                        }

                                        Position3D leakPos = new Position3D(
                                            frame.xCoord + leakDir.offsetX,
                                            frame.yCoord + leakDir.offsetY,
                                            frame.zCoord + leakDir.offsetZ);
                                        if ((this.interiorPositions == null || !this.interiorPositions.contains(
                                            LongSet.pack(
                                                leakPos.getX() - this.bottomDiagFrame.getX(),
                                                leakPos.getY() - this.bottomDiagFrame.getY(),
                                                leakPos.getZ() - this.bottomDiagFrame.getZ())))
                                            && this.fluidStack.amount >= 1000) {
                                            this.worldObj.setBlock(
                                                frame.xCoord + leakDir.offsetX,
                                                frame.yCoord + leakDir.offsetY,
                                                frame.zCoord + leakDir.offsetZ,
                                                this.fluidStack.getFluid()
                                                    .getBlock(),
                                                0,
                                                3);
                                            this.worldObj.notifyBlockOfNeighborChange(
                                                frame.xCoord + leakDir.offsetX,
                                                frame.yCoord + leakDir.offsetY,
                                                frame.zCoord + leakDir.offsetZ,
                                                this.fluidStack.getFluid()
                                                    .getBlock());
                                            this.drain(1000, true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private List<TileEntityValve> getAllValves() {
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

    private List<TileEntityValve> getValvesByName(String name) {
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
        this.needsUpdate = true;
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

    private Map<Position3D, ExtendedBlock>[] fetchMaps() {
        return GenericUtil.getTankFrame(this.worldObj, this.bottomDiagFrame, this.topDiagFrame);
    }

    private boolean setupTank() {
        this.updateCornerFrames();
        Map<Position3D, ExtendedBlock>[] maps = this.fetchMaps();
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
                for (Entry<Position3D, ExtendedBlock> airCheck : maps[2].entrySet()) {
                    pos = airCheck.getKey();
                    if (!this.worldObj.isAirBlock(pos.getX(), pos.getY(), pos.getZ()) && !airCheck.getValue()
                        .getBlock()
                        .getUnlocalizedName()
                        .equals("railcraft.residual.heat")) {
                        return false;
                    }
                }

                this.fluidCapacity = TankMath.capacityInt(
                    FancyFluidStorage.instance.INSIDE_CAPACITY,
                    maps[0].size(),
                    maps[1].size(),
                    maps[2].size(),
                    this.mbPerVirtualTank);

                LongSet interior = new LongSet(maps[2].size());
                int baseX = this.bottomDiagFrame.getX();
                int baseY = this.bottomDiagFrame.getY();
                int baseZ = this.bottomDiagFrame.getZ();
                for (Position3D interiorPos : maps[2].keySet()) {
                    interior.add(
                        LongSet
                            .pack(interiorPos.getX() - baseX, interiorPos.getY() - baseY, interiorPos.getZ() - baseZ));
                }

                for (Entry<Position3D, ExtendedBlock> frameCheck : maps[0].entrySet()) {
                    Position3D fPos = frameCheck.getKey();
                    ExtendedBlock fBlock = frameCheck.getValue();
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

                for (Entry<Position3D, ExtendedBlock> insideFrameCheck : maps[1].entrySet()) {
                    pos = insideFrameCheck.getKey();
                    ExtendedBlock check = insideFrameCheck.getValue();
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
                    this.setSlaveValveInside(interior, valve);
                }

                this.setMaster(true);

                for (Entry<Position3D, ExtendedBlock> setTiles : maps[0].entrySet()) {
                    pos = setTiles.getKey();
                    TileEntityTankFrame tankFrame;
                    if (setTiles.getValue()
                        .getBlock() != FancyFluidStorage.blockTankFrame) {
                        this.worldObj.setBlock(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            FancyFluidStorage.blockTankFrame,
                            setTiles.getValue()
                                .getMetadata(),
                            2);
                        tankFrame = (TileEntityTankFrame) this.worldObj
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null) {
                            return false;
                        }
                        tankFrame.initialize(this, setTiles.getValue());
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

                for (Entry<Position3D, ExtendedBlock> setTiles : maps[1].entrySet()) {
                    pos = setTiles.getKey();
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
                        } else if (GenericUtil.isTileEntityAcceptable(
                            setTiles.getValue()
                                .getBlock(),
                            tilex)) {
                                this.worldObj.setBlock(
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ(),
                                    FancyFluidStorage.blockTankFrame,
                                    setTiles.getValue()
                                        .getMetadata(),
                                    2);
                                TileEntityTankFrame tankFrame = (TileEntityTankFrame) this.worldObj
                                    .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                                if (tankFrame == null) {
                                    return false;
                                }
                                tankFrame.initialize(this, setTiles.getValue());
                                this.tankFrames.add(tankFrame);
                            }
                    } else {
                        this.worldObj.setBlock(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            FancyFluidStorage.blockTankFrame,
                            setTiles.getValue()
                                .getMetadata(),
                            2);
                        TileEntityTankFrame tankFrame = (TileEntityTankFrame) this.worldObj
                            .getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        if (tankFrame == null) {
                            return false;
                        }
                        tankFrame.initialize(this, setTiles.getValue());
                        this.tankFrames.add(tankFrame);
                    }
                }

                this.interiorPositions = interior;
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
        return master != null && master.autoOutput;
    }

    public void setAutoOutput(boolean autoOutput) {
        TileEntityValve master = this.getMaster();
        if (master != null && master != this) {
            master.setAutoOutput(autoOutput);
            return;
        }

        this.autoOutput = autoOutput;
        this.updateBlockAndNeighbors(true);
    }

    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.isValid = tag.getBoolean("isValid");
        this.inside = ForgeDirection.getOrientation(tag.getInteger("inside"));
        this.isMaster = tag.getBoolean("master");
        this.valveHeightPosition = Math.max(0, tag.getInteger("valveHeightPosition"));
        if (this.isMaster()) {
            this.tankHeight = Math.max(0, tag.getInteger("tankHeight"));
            this.fluidCapacity = Math.max(0, tag.getInteger("fluidCapacity"));

            FluidStack loaded = NbtHelper.readFluidStack(tag, NbtHelper.FLUID_TAG, this.fluidCapacity);
            if (loaded == null && (tag.hasKey("fluidName") || tag.hasKey("fluidID"))) {
                loaded = NbtHelper.readLegacyFluid(tag, "fluidName", "fluidID", "fluidAmount", this.fluidCapacity);
            }
            this.fluidStack = loaded;
            this.updateFluidTemperature();
        } else {
            // Always re-read the link. The old code skipped this when a master was
            // already cached, so client S35 packets could never re-point a valve.
            this.setMasterPos(NbtHelper.readPosition(tag, "masterValve"));
        }

        this.autoOutput = tag.getBoolean("autoOutput");
        if (tag.hasKey("valveName")) {
            this.setValveName(tag.getString("valveName"));
        } else {
            this.setValveName(GenericUtil.getUniqueValveName(this));
        }

        if (tag.hasKey("bottomDiagF")) {
            this.bottomDiagFrame = NbtHelper.readPosition(tag, "bottomDiagF");
            this.topDiagFrame = NbtHelper.readPosition(tag, "topDiagF");
            if (this.bottomDiagFrame == null || this.topDiagFrame == null) {
                this.bottomDiagFrame = null;
                this.topDiagFrame = null;
            }
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("isValid", this.isValid);
        tag.setInteger("inside", this.inside.ordinal());
        tag.setBoolean("master", this.isMaster());
        tag.setInteger("valveHeightPosition", this.valveHeightPosition);
        if (this.isMaster()) {
            String fluidName = this.fluidStack == null ? null : FluidRegistry.getFluidName(this.fluidStack);
            tag.setBoolean("hasFluid", this.fluidStack != null);
            if (fluidName != null) {
                tag.setString("fluidName", fluidName);
                tag.setInteger("fluidAmount", this.fluidStack.amount);
            }
            NbtHelper.writeFluidStack(tag, NbtHelper.FLUID_TAG, this.fluidStack);

            tag.setInteger("tankHeight", this.tankHeight);
            tag.setInteger("fluidCapacity", this.fluidCapacity);
        } else {
            // Persist the link even when the master chunk is unloaded; resolving the
            // TE here can force a chunk load during a save.
            Position3D master = this.masterValvePos;
            if (master == null) {
                TileEntityValve resolved = this.getMaster();
                if (resolved != null) {
                    master = new Position3D(resolved.xCoord, resolved.yCoord, resolved.zCoord);
                }
            }
            NbtHelper.writePosition(tag, "masterValve", master);
        }

        tag.setBoolean("autoOutput", this.autoOutput);
        if (!this.getValveName()
            .isEmpty()) {
            tag.setString("valveName", this.getValveName());
        }

        NbtHelper.writePosition(tag, "bottomDiagF", this.bottomDiagFrame);
        NbtHelper.writePosition(tag, "topDiagF", this.topDiagFrame);

        super.writeToNBT(tag);
    }

    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
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

    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return 0;
        }

        if (this.getMaster() != this) {
            return this.getMaster()
                .fill(resource, doFill);
        } else if (this.isValid() && (this.fluidStack == null || this.fluidStack.isFluidEqual(resource))) {
            if (!doFill) {
                return this.fluidStack == null ? Math.min(this.fluidCapacity, resource.amount)
                    : Math.min(this.fluidCapacity - this.fluidStack.amount, resource.amount);
            } else if (this.fluidStack == null) {
                this.fluidStack = new FluidStack(resource, Math.min(this.fluidCapacity, resource.amount));
                this.updateFluidTemperature();
                this.setNeedsUpdate();
                return this.fluidStack.amount;
            } else {
                int filled = this.fluidCapacity - this.fluidStack.amount;
                if (resource.amount < filled) {
                    this.fluidStack.amount = this.fluidStack.amount + resource.amount;
                    filled = resource.amount;
                } else {
                    this.fluidStack.amount = this.fluidCapacity;
                }

                this.getMaster()
                    .setNeedsUpdate();
                return filled;
            }
        } else {
            return 0;
        }
    }

    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }

        if (this.getMaster() == this) {
            if (this.isValid() && this.fluidStack != null) {
                int drained = maxDrain;
                if (this.fluidStack.amount < maxDrain) {
                    drained = this.fluidStack.amount;
                }

                FluidStack stack = new FluidStack(this.fluidStack, drained);
                if (doDrain) {
                    this.fluidStack.amount -= drained;
                    if (this.fluidStack.amount <= 0) {
                        this.fluidStack = null;
                        this.updateFluidTemperature();
                    }

                    this.getMaster()
                        .setNeedsUpdate();
                }

                return stack;
            } else {
                return null;
            }
        } else {
            return this.getMaster()
                .drain(maxDrain, doDrain);
        }
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
        if (fluid == null || !this.isValid()) {
            return false;
        } else if (this.getFluid() != null && this.getFluid()
            .getFluid() != fluid) {
                return false;
            } else if (this.getFluidAmount() >= this.getCapacity()) {
                return false;
            } else {
                return true;
            }
    }

    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (!this.canFillIncludingContainers(from, fluid)) {
            return false;
        }

        FluidStack stack = this.getFluid();
        boolean negativeDensity = stack != null && stack.getFluid() != null
            && stack.getFluid()
                .getDensity(stack) < 0;

        return TankMath.canFillFromHeight(
            this.valveHeightPosition,
            this.getTankHeight(),
            TankMath.fillFraction(this.getFluidAmount(), this.getCapacity()),
            this.getAutoOutput(),
            negativeDensity);
    }

    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        if (fluid == null || !this.isValid()) {
            return false;
        } else {
            return this.getFluid() == null ? false
                : this.getFluid()
                    .getFluid() == fluid && this.getFluidAmount() > 0;
        }
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
                if (this.getFluid() == null) {
                    return null;
                }

                return new Object[] { this.getFluid()
                    .getLocalizedName() };
            case 1:
                return new Object[] { this.getFluidAmount() };
            case 2:
                return new Object[] { this.getCapacity() };
            case 3:
                if (arguments.length == 1) {
                    if (!(arguments[0] instanceof Boolean)) {
                        throw new LuaException(
                            "expected argument 1 to be of type \"boolean\", found \"" + arguments[0].getClass()
                                .getSimpleName() + "\"");
                    } else {
                        for (TileEntityValve valve : this.getAllValves()) {
                            valve.setAutoOutput((Boolean) arguments[0]);
                        }

                        return new Object[] { (Boolean) arguments[0] };
                    }
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
                } else {
                    List<TileEntityValve> valves = this.getValvesByName((String) arguments[0]);
                    if (valves.isEmpty()) {
                        throw new LuaException("no valves found");
                    } else {
                        List<String> valveNames = new ArrayList<>();

                        for (TileEntityValve valve : valves) {
                            valve.setAutoOutput((Boolean) arguments[1]);
                            valveNames.add(valve.getValveName());
                        }

                        return new Object[] { valveNames };
                    }
                }
            case 4:
                if (arguments.length == 0) {
                    Map<String, Boolean> valveOutputs = new HashMap<>();

                    for (TileEntityValve valve : this.getAllValves()) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[] { valveOutputs };
                } else if (arguments.length != 1) {
                    throw new LuaException(
                        "insufficient number of arguments found - expected 1, got " + arguments.length);
                } else if (!(arguments[0] instanceof String)) {
                    throw new LuaException(
                        "expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass()
                            .getSimpleName() + "\"");
                } else {
                    List<TileEntityValve> valves = this.getValvesByName((String) arguments[0]);
                    if (valves.isEmpty()) {
                        throw new LuaException("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();

                    for (TileEntityValve valve : valves) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[] { valveOutputs };
                }
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
                if (this.getFluid() == null) {
                    return null;
                }

                return new Object[] { this.getFluid()
                    .getLocalizedName() };
            case "getFluidAmount":
                return new Object[] { this.getFluidAmount() };
            case "getFluidCapacity":
                return new Object[] { this.getCapacity() };
            case "setAutoOutput":
                if (args.count() == 1) {
                    if (!args.isBoolean(0)) {
                        throw new Exception(
                            "expected argument 1 to be of type \"boolean\", found \"" + args.checkAny(0)
                                .getClass()
                                .getSimpleName() + "\"");
                    } else {
                        for (TileEntityValve valve : this.getAllValves()) {
                            valve.setAutoOutput(args.checkBoolean(0));
                        }

                        return new Object[] { args.checkBoolean(0) };
                    }
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
                } else {
                    List<TileEntityValve> valves = this.getValvesByName(args.checkString(0));
                    if (valves.isEmpty()) {
                        throw new Exception("no valves found");
                    } else {
                        List<String> valveNames = new ArrayList<>();

                        for (TileEntityValve valve : valves) {
                            valve.setAutoOutput(args.checkBoolean(1));
                            valveNames.add(valve.getValveName());
                        }

                        return new Object[] { valveNames };
                    }
                }
            case "doesAutoOutput":
                if (args.count() == 0) {
                    Map<String, Boolean> valveOutputs = new HashMap<>();

                    for (TileEntityValve valve : this.getAllValves()) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[] { valveOutputs };
                } else if (args.count() != 1) {
                    throw new Exception("insufficient number of arguments found - expected 1, got " + args.count());
                } else if (!args.isString(0)) {
                    throw new Exception(
                        "expected argument 1 to be of type \"String\", found \"" + args.checkAny(0)
                            .getClass()
                            .getSimpleName() + "\"");
                } else {
                    List<TileEntityValve> valves = this.getValvesByName(args.checkString(0));
                    if (valves.isEmpty()) {
                        throw new Exception("no valves found");
                    }

                    Map<String, Boolean> valveOutputs = new HashMap<>();

                    for (TileEntityValve valve : valves) {
                        valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
                    }

                    return new Object[] { valveOutputs };
                }
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
