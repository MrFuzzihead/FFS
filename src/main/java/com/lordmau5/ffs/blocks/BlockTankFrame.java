package com.lordmau5.ffs.blocks;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import com.cricketcraft.chisel.api.IFacade;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.TankFrameRenderer;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.ModNames;

import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.Method;

@Interface(iface = "com.cricketcraft.chisel.api.IFacade", modid = "chisel")
public class BlockTankFrame extends Block implements IFacade {

    public BlockTankFrame() {
        super(Material.rock);
        this.setBlockName("blockTankFrame");
        this.setBlockTextureName(ModNames.resource("blockValve"));
    }

    public boolean hasTileEntity(int metadata) {
        return true;
    }

    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityTankFrame();
    }

    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            frame.setBlock(null);
            frame.breakFrame();
            frame.onBreak();
        }

        super.onBlockExploded(world, x, y, z, explosion);
    }

    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            // Drops are spawned by the server; the client call is only a prediction.
            if (!world.isRemote && !player.capabilities.isCreativeMode) {
                Block block = frame.getStoredBlock();
                if (block != null) {
                    ArrayList<ItemStack> items = new ArrayList<>();
                    int meta = frame.getStoredMetadata();
                    int fortune = EnchantmentHelper.getFortuneModifier(player);
                    boolean silkTouch = block.canSilkHarvest(world, player, x, y, z, meta)
                        && EnchantmentHelper.getSilkTouchModifier(player);

                    if (silkTouch) {
                        items.add(new ItemStack(Item.getItemFromBlock(block), 1, meta));
                    } else {
                        items.addAll(block.getDrops(world, x, y, z, meta, fortune));
                    }

                    // Fire the event with the final drop list so handlers can remove or
                    // replace drops; the old code fired it before adding the drops.
                    ForgeEventFactory
                        .fireBlockHarvesting(items, world, block, x, y, z, meta, fortune, 1.0F, silkTouch, player);

                    for (ItemStack is : items) {
                        this.dropBlockAsItem(world, x, y, z, is);
                    }
                }
            }

            frame.onBreak();
        }

        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
        return null;
    }

    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            Block stored = frame.getStoredBlock();
            if (stored != null) {
                return stored.getPlayerRelativeBlockHardness(player, world, x, y, z);
            }
        }

        return super.getPlayerRelativeBlockHardness(player, world, x, y, z);
    }

    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        return true;
    }

    public int getRenderBlockPass() {
        return 1;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }

    public boolean canRenderInPass(int pass) {
        // The stored block decides its own pass inside the renderer. The chunk
        // builder calls this for every pass, but does not give renderWorldBlock
        // the pass number, so hand it to the renderer explicitly.
        TankFrameRenderer.setPass(pass);
        return true;
    }

    public int getRenderType() {
        return TankFrameRenderer.id;
    }

    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)) {
            return true;
        } else if (player.isSneaking()) {
            return false;
        } else {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            if (frame != null && frame.getValve() != null) {
                TileEntityValve valve = frame.getValve();
                if (valve.isValid()) {
                    if (GenericUtil.isFluidContainer(player.getHeldItem())) {
                        return GenericUtil.fluidContainerHandler(world, x, y, z, valve, player);
                    }

                    player.openGui(FancyFluidStorage.instance, 0, world, x, y, z);
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            Block stored = ((TileEntityTankFrame) tile).getStoredBlock();
            if (stored != null) {
                return stored.getPickBlock(target, world, x, y, z, player);
            }
        }

        return null;
    }

    public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            Block stored = ((TileEntityTankFrame) tile).getStoredBlock();
            if (stored != null) {
                return stored.getFlammability(world, x, y, z, face);
            }
        }

        return 0;
    }

    public float getExplosionResistance(Entity par1Entity, World world, int x, int y, int z, double explosionX,
        double explosionY, double explosionZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            Block stored = ((TileEntityTankFrame) tile).getStoredBlock();
            if (stored != null) {
                return stored.getExplosionResistance(par1Entity, world, x, y, z, explosionX, explosionY, explosionZ);
            }
        }

        return super.getExplosionResistance(par1Entity);
    }

    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) world.getTileEntity(x, y, z);
            frame.onBreak();
        }

        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityTankFrame) {
            TileEntityValve valve = ((TileEntityTankFrame) tile).getValve();
            if (valve != null) {
                valve.onNeighborChangedAt(tileX, tileY, tileZ);
            }
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityTankFrame) {
            TileEntityValve valve = ((TileEntityTankFrame) tile).getValve();
            if (valve != null) {
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    valve.onNeighborChangedAt(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
                }
            }
        }
    }

    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Method(modid = "chisel")
    public Block getFacade(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if (frame.getValve() == null) {
                return null;
            }

            return frame.getStoredBlock();
        }

        return null;
    }

    @Method(modid = "chisel")
    public int getFacadeMetadata(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileEntityTankFrame) {
            TileEntityTankFrame frame = (TileEntityTankFrame) tile;
            if (frame.getValve() == null) {
                return 0;
            }

            return frame.getStoredMetadata();
        }

        return 0;
    }
}
