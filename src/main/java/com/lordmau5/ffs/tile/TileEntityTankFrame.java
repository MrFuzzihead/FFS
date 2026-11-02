package com.lordmau5.ffs.tile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.Position3D;
import com.rwtema.funkylocomotion.api.IMoveCheck;

import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.common.Optional.Method;

@InterfaceList({ @Interface(iface = "com.rwtema.funkylocomotion.api.IMoveCheck", modid = "funkylocomotion") })
public class TileEntityTankFrame extends TileEntity implements IMoveCheck {

    private ExtendedBlock block;
    private Position3D valvePos;
    private TileEntityValve masterValve;

    public void initialize(TileEntityValve masterValve, ExtendedBlock block) {
        this.masterValve = masterValve;
        this.block = block;
        if (masterValve != null) {
            this.valvePos = new Position3D(masterValve.xCoord, masterValve.yCoord, masterValve.zCoord);
        }
    }

    /** The stored block, or {@code null} if this frame was saved by a removed mod. */
    public Block getStoredBlock() {
        return this.block == null ? null : this.block.getBlock();
    }

    public int getStoredMetadata() {
        return this.block == null ? 0 : this.block.getMetadata();
    }

    public boolean hasBlock() {
        return this.block != null && this.block.getBlock() != null;
    }

    public boolean isFrameInvalid() {
        if (this.worldObj == null) {
            return true;
        }

        TileEntity tile = this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord);
        return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
    }

    public List<ForgeDirection> getNeighborBlockOrAir(Block block) {
        List<ForgeDirection> dirs = new ArrayList<>();
        if (this.worldObj == null || block == null) {
            return dirs;
        }

        for (ForgeDirection dr : ForgeDirection.VALID_DIRECTIONS) {
            if (block == Blocks.air) {
                if (this.worldObj
                    .isAirBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ)) {
                    dirs.add(dr);
                }
            } else {
                Block otherBlock = this.worldObj
                    .getBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ);
                if (block == otherBlock || this.worldObj
                    .isAirBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ)) {
                    dirs.add(dr);
                }
            }
        }

        return dirs;
    }

    public boolean tryBurning() {
        Block block = this.getStoredBlock();
        if (block == null || this.worldObj == null) {
            return false;
        } else {
            for (ForgeDirection dr : this.getNeighborBlockOrAir(Blocks.air)) {
                if (block.isFlammable(this.worldObj, this.xCoord, this.yCoord, this.zCoord, dr)) {
                    this.worldObj.setBlock(
                        this.xCoord + dr.offsetX,
                        this.yCoord + dr.offsetY,
                        this.zCoord + dr.offsetZ,
                        Blocks.fire,
                        0,
                        3);
                    return true;
                }
            }

            return false;
        }
    }

    public void breakFrame() {
        if (this.worldObj != null && !this.isFrameInvalid()) {
            this.worldObj.removeTileEntity(this.xCoord, this.yCoord, this.zCoord);
            if (this.block != null && this.block.getBlock() != null) {
                this.worldObj.setBlock(
                    this.xCoord,
                    this.yCoord,
                    this.zCoord,
                    this.block.getBlock(),
                    this.block.getMetadata(),
                    3);
            } else {
                this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
            }
        }
    }

    public void onBreak() {
        if (this.worldObj != null && !this.worldObj.isRemote && this.getValve() != null) {
            this.masterValve.breakTank(this);
        }
    }

    /** @return true when the valve link actually changed */
    public boolean setValvePos(Position3D valvePos) {
        boolean changed = this.valvePos == null ? valvePos != null : !this.valvePos.equals(valvePos);
        this.valvePos = valvePos;
        this.masterValve = null;
        return changed;
    }

    public Position3D getValvePos() {
        return this.valvePos;
    }

    /**
     * {@code true} when this frame already belongs to a different, still-valid
     * tank. Shared walls must not be silently stolen by the next tank that forms.
     */
    public boolean isOwnedByOther(TileEntityValve master) {
        if (this.valvePos == null) {
            return false;
        }

        if (master != null && this.valvePos.equals(new Position3D(master.xCoord, master.yCoord, master.zCoord))) {
            return false;
        }

        TileEntityValve other = this.getValve();
        return other != null && other.isValid();
    }

    public TileEntityValve getValve() {
        if (this.masterValve != null && this.masterValve.isInvalid()) {
            this.masterValve = null;
        }

        if (this.masterValve == null && this.valvePos != null && this.worldObj != null) {
            TileEntity tile = this.worldObj
                .getTileEntity(this.valvePos.getX(), this.valvePos.getY(), this.valvePos.getZ());
            this.masterValve = tile instanceof TileEntityValve ? (TileEntityValve) tile : null;
        }

        return this.masterValve;
    }

    public void setBlock(ExtendedBlock block) {
        this.block = block;
    }

    public ExtendedBlock getBlock() {
        return this.block;
    }

    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("valveX") && tag.hasKey("valveY") && tag.hasKey("valveZ")) {
            this.setValvePos(
                new Position3D(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ")));
        } else {
            this.valvePos = null;
        }

        if (tag.hasKey("blockId")) {
            Block stored = Block.getBlockById(tag.getInteger("blockId"));
            // A removed mod leaves an unknown id behind. Treat it as "no block" so
            // the renderer/drops/pick-block paths can fall back safely.
            this.block = stored == null || stored == Blocks.air ? null
                : new ExtendedBlock(stored, tag.getInteger("metadata"));
        } else {
            this.block = null;
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        Position3D valve = this.valvePos;
        if (valve == null) {
            TileEntityValve resolved = this.getValve();
            if (resolved != null) {
                valve = new Position3D(resolved.xCoord, resolved.yCoord, resolved.zCoord);
            }
        }

        if (valve != null) {
            tag.setInteger("valveX", valve.getX());
            tag.setInteger("valveY", valve.getY());
            tag.setInteger("valveZ", valve.getZ());
        }

        if (this.hasBlock()) {
            tag.setInteger("blockId", Block.getIdFromBlock(this.getStoredBlock()));
            tag.setInteger("metadata", this.getStoredMetadata());
        }
    }

    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, tag);
    }

    public void markForUpdate() {
        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    /**
     * Frames only serve rendering and NBT data; they have no per-tick work. Avoiding
     * the tick list removes thousands of idle updateEntity calls per large tank.
     */
    @Override
    public boolean canUpdate() {
        return false;
    }

    @Method(modid = "funkylocomotion")
    public boolean canMove(World worldObj, int x, int y, int z) {
        return false;
    }
}
