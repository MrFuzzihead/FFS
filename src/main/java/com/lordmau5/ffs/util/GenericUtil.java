package com.lordmau5.ffs.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;

public class GenericUtil {

    private static List<Block> blacklistedBlocks;
    private static List<String> validTiles;
    private static List<ItemStack> glassList;

    public static void init() {
        glassList = OreDictionary.getOres("blockGlass");
        blacklistedBlocks = new ArrayList<>();
        blacklistedBlocks.add(Blocks.grass);
        blacklistedBlocks.add(Blocks.dirt);
        blacklistedBlocks.add(Blocks.bedrock);
        blacklistedBlocks.add(Blocks.redstone_lamp);
        blacklistedBlocks.add(Blocks.sponge);
        validTiles = new ArrayList<>();
        validTiles.add("blockFusedQuartz");
    }

    public static String getUniqueValveName(TileEntityValve valve) {
        return "valve_" + Integer.toHexString(new Position3D(valve.xCoord, valve.yCoord, valve.zCoord).hashCode());
    }

    public static boolean canAutoOutput(float height, int tankHeight, int valvePosition, boolean negativeDensity) {
        height *= tankHeight;
        return negativeDensity ? false : height > valvePosition - 0.5F;
    }

    public static boolean isBlockGlass(Block block, int metadata) {
        if (block != null && block.getMaterial() != Material.air) {
            if (block instanceof BlockGlass) {
                return true;
            } else {
                ItemStack is = new ItemStack(block, 1, metadata);
                if (block.getMaterial() == Material.glass && !is.getUnlocalizedName()
                    .contains("pane")) {
                    return true;
                } else {
                    for (ItemStack is2 : glassList) {
                        if (is2.getUnlocalizedName()
                            .equals(is.getUnlocalizedName())) {
                            return true;
                        }
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean areTankBlocksValid(ExtendedBlock bottomBlock, ExtendedBlock topBlock, World world,
        Position3D bottomPos) {
        if (!isValidTankBlock(world, bottomPos, bottomBlock)) {
            return false;
        } else {
            switch (FancyFluidStorage.instance.TANK_FRAME_MODE) {
                case SAME_BLOCK:
                    return bottomBlock.equals(topBlock);
                case DIFFERENT_METADATA:
                    return bottomBlock.equalsIgnoreMetadata(topBlock);
                case DIFFERENT_BLOCK:
                    return true;
                default:
                    return false;
            }
        }
    }

    public static boolean isTileEntityAcceptable(Block block, TileEntity tile) {
        for (String name : validTiles) {
            if (block.getUnlocalizedName()
                .toLowerCase()
                .contains(name.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isValidTankBlock(World world, Position3D pos, ExtendedBlock extendedBlock) {
        Block block = extendedBlock.getBlock();
        if (block.hasTileEntity(extendedBlock.getMetadata())) {
            TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (tile != null) {
                return tile instanceof TileEntityTankFrame || isTileEntityAcceptable(block, tile);
            }
        }

        if (blacklistedBlocks.contains(block)) {
            return false;
        } else if (block.getMaterial() == Material.sand) {
            return false;
        } else if (!block.isOpaqueCube()) {
            return false;
        } else if (!block.renderAsNormalBlock()) {
            return false;
        } else {
            return FancyFluidStorage.instance.TANK_FRAME_MODE == FancyFluidStorage.TankFrameMode.DIFFERENT_BLOCK ? true
                : block.func_149730_j();
        }
    }

    public static boolean canBlockLeak(Block block) {
        Material mat = block.getMaterial();
        return mat.equals(Material.grass) || mat.equals(Material.sponge)
            || mat.equals(Material.cloth)
            || mat.equals(Material.clay)
            || mat.equals(Material.gourd)
            || mat.equals(Material.sand);
    }

    public static boolean isFluidContainer(ItemStack playerItem) {
        return playerItem == null ? false
            : FluidContainerRegistry.isContainer(playerItem) || playerItem.getItem() instanceof IFluidContainerItem;
    }

    public static boolean fluidContainerHandler(World world, int x, int y, int z, TileEntityValve valve,
        EntityPlayer player) {
        ItemStack current = player.inventory.getCurrentItem();
        if (current == null) {
            return false;
        } else {
            if (FluidContainerRegistry.isContainer(current)) {
                FluidStack liquid = FluidContainerRegistry.getFluidForFilledItem(current);
                if (liquid != null) {
                    int qty = valve.fillFromContainer(ForgeDirection.UNKNOWN, liquid, true);
                    if (qty != 0 && !player.capabilities.isCreativeMode) {
                        if (current.stackSize > 1) {
                            if (!player.inventory
                                .addItemStackToInventory(FluidContainerRegistry.drainFluidContainer(current))) {
                                player.dropPlayerItemWithRandomChoice(
                                    FluidContainerRegistry.drainFluidContainer(current),
                                    false);
                            }

                            player.inventory
                                .setInventorySlotContents(player.inventory.currentItem, consumeItem(current));
                        } else {
                            player.inventory.setInventorySlotContents(
                                player.inventory.currentItem,
                                FluidContainerRegistry.drainFluidContainer(current));
                        }
                    }

                    return true;
                }

                FluidStack available = valve.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
                if (available != null) {
                    ItemStack filled = FluidContainerRegistry.fillFluidContainer(available, current);
                    liquid = FluidContainerRegistry.getFluidForFilledItem(filled);
                    if (liquid != null) {
                        if (!player.capabilities.isCreativeMode) {
                            if (current.stackSize > 1) {
                                if (!player.inventory.addItemStackToInventory(filled)) {
                                    return false;
                                }

                                player.inventory
                                    .setInventorySlotContents(player.inventory.currentItem, consumeItem(current));
                            } else {
                                player.inventory
                                    .setInventorySlotContents(player.inventory.currentItem, consumeItem(current));
                                player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
                            }
                        }

                        valve.drain(ForgeDirection.UNKNOWN, liquid.amount, true);
                        return true;
                    }
                }
            } else if (current.getItem() instanceof IFluidContainerItem) {
                if (current.stackSize != 1) {
                    return false;
                }

                if (!world.isRemote) {
                    IFluidContainerItem container = (IFluidContainerItem) current.getItem();
                    FluidStack liquidx = container.getFluid(current);
                    FluidStack tankLiquid = valve.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
                    boolean mustDrain = liquidx == null || liquidx.amount == 0;
                    boolean mustFill = tankLiquid == null || tankLiquid.amount == 0;
                    if (!mustDrain || !mustFill) {
                        if (mustDrain || !player.isSneaking()) {
                            liquidx = valve.drain(ForgeDirection.UNKNOWN, 1000, false);
                            int qtyToFill = container.fill(current, liquidx, true);
                            valve.drain(ForgeDirection.UNKNOWN, qtyToFill, true);
                        } else if (liquidx.amount > 0) {
                            int qty = valve.fill(ForgeDirection.UNKNOWN, liquidx, false);
                            valve.fill(ForgeDirection.UNKNOWN, container.drain(current, qty, true), true);
                        }
                    }
                }

                return true;
            }

            return false;
        }
    }

    public static ItemStack consumeItem(ItemStack stack) {
        if (stack.stackSize == 1) {
            return stack.getItem()
                .hasContainerItem(stack)
                    ? stack.getItem()
                        .getContainerItem(stack)
                    : null;
        } else {
            stack.splitStack(1);
            return stack;
        }
    }

    private static Map<Position3D, ExtendedBlock> getBlocksBetweenPoints(World world, Position3D pos1,
        Position3D pos2) {
        Map<Position3D, ExtendedBlock> blocks = new HashMap<>();
        Position3D distance = pos2.getDistance(pos1);
        int dX = distance.getX();
        int dY = distance.getY();
        int dZ = distance.getZ();

        for (int x = 0; x <= dX; x++) {
            for (int y = 0; y <= dY; y++) {
                for (int z = 0; z <= dZ; z++) {
                    Position3D pos = new Position3D(pos1.getX() + x, pos1.getY() + y, pos1.getZ() + z);
                    blocks.put(
                        pos,
                        new ExtendedBlock(
                            world.getBlock(pos.getX(), pos.getY(), pos.getZ()),
                            world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ())));
                }
            }
        }

        return blocks;
    }

    public static Map<Position3D, ExtendedBlock>[] getTankFrame(World world, Position3D bottomDiag,
        Position3D topDiag) {
        Map<Position3D, ExtendedBlock>[] maps = new HashMap[3];
        maps[0] = new HashMap<>();
        maps[1] = new HashMap<>();
        maps[2] = new HashMap<>();
        int x1 = bottomDiag.getX();
        int y1 = bottomDiag.getY();
        int z1 = bottomDiag.getZ();
        int x2 = topDiag.getX();
        int y2 = topDiag.getY();
        int z2 = topDiag.getZ();

        for (Entry<Position3D, ExtendedBlock> e : getBlocksBetweenPoints(
            world,
            new Position3D(x1, y1, z1),
            new Position3D(x2, y2, z2)).entrySet()) {
            Position3D p = e.getKey();
            if ((p.getX() != x1 && p.getX() != x2 || p.getY() != y1 && p.getY() != y2)
                && (p.getX() != x1 && p.getX() != x2 || p.getZ() != z1 && p.getZ() != z2)
                && (p.getY() != y1 && p.getY() != y2 || p.getZ() != z1 && p.getZ() != z2)) {
                if (p.getX() != x1 && p.getX() != x2
                    && p.getY() != y1
                    && p.getY() != y2
                    && p.getZ() != z1
                    && p.getZ() != z2) {
                    maps[2].put(p, e.getValue());
                } else {
                    maps[1].put(p, e.getValue());
                }
            } else {
                maps[0].put(p, e.getValue());
            }
        }

        return maps;
    }

    public static String intToFancyNumber(int number) {
        return NumberFormat.getIntegerInstance()
            .format((long) number);
    }
}
