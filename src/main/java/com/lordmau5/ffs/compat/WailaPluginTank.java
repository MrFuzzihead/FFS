package com.lordmau5.ffs.compat;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;

import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.Method;
import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.SpecialChars;

@Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
public class WailaPluginTank implements IWailaDataProvider {

    private static final String CONFIG_TEMPERATURE = "ffs.showTemperature";
    private static final String CONFIG_DIMENSIONS = "ffs.showDimensions";
    private static final String CONFIG_AUTO_OUTPUT = "ffs.showAutoOutput";
    private static final String CONFIG_ADVANCED = "ffs.advanced";
    private static final String CONFIG_HINT = "ffs.showHint";

    public static void init() {
        FMLInterModComms.sendMessage("Waila", "register", WailaPluginTank.class.getName() + ".registerPlugin");
    }

    @Method(modid = "Waila")
    public static void registerPlugin(IWailaRegistrar registrar) {
        WailaPluginTank instance = new WailaPluginTank();
        registrar.registerStackProvider(instance, TileEntityTankFrame.class);
        registrar.registerHeadProvider(instance, TileEntityValve.class);
        registrar.registerHeadProvider(instance, TileEntityTankFrame.class);
        registrar.registerBodyProvider(instance, TileEntityValve.class);
        registrar.registerBodyProvider(instance, TileEntityTankFrame.class);
        registrar.registerTailProvider(instance, TileEntityValve.class);
        registrar.registerTailProvider(instance, TileEntityTankFrame.class);
        registrar.registerNBTProvider(instance, TileEntityValve.class);
        registrar.registerNBTProvider(instance, TileEntityTankFrame.class);

        registrar.addConfig("FFS", CONFIG_TEMPERATURE, "Show fluid temperature", true);
        registrar.addConfig("FFS", CONFIG_DIMENSIONS, "Show tank dimensions", false);
        registrar.addConfig("FFS", CONFIG_AUTO_OUTPUT, "Show auto-output state", true);
        registrar.addConfig("FFS", CONFIG_ADVANCED, "Enable advanced tooltip", true);
        registrar.addConfig("FFS", CONFIG_HINT, "Show interaction hint", true);
    }

    @Method(modid = "Waila")
    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        TileEntity te = accessor.getTileEntity();
        if (!(te instanceof TileEntityTankFrame)) {
            return null;
        }

        ExtendedBlock block = ((TileEntityTankFrame) te).getBlock();
        if (block == null || block.getBlock() == null) {
            return null;
        }

        return new ItemStack(block.getBlock(), 1, block.getMetadata());
    }

    @Method(modid = "Waila")
    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        TileEntity te = accessor.getTileEntity();
        currenttip.clear();
        currenttip
            .add(translateRaw(te instanceof TileEntityTankFrame ? "waila.ffs.head_frame" : "waila.ffs.head_valve"));
        return currenttip;
    }

    @Method(modid = "Waila")
    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        TileEntity te = accessor.getTileEntity();
        boolean frame = te instanceof TileEntityTankFrame;
        WailaTankData.Data data = data(accessor);

        if (frame) {
            currenttip.add(translateRaw("waila.ffs.part_of_tank"));
        }

        if (data == null) {
            return currenttip;
        }

        if (!data.valid) {
            currenttip.add(translateRaw("waila.ffs.invalid"));
            return currenttip;
        }

        if (!frame) {
            currenttip.add(translate("waila.ffs.valve", SpecialChars.ITALIC + data.valveName + SpecialChars.RESET));
            if (config.getConfig(CONFIG_AUTO_OUTPUT)) {
                String state = (data.autoOutput ? SpecialChars.GREEN : SpecialChars.RED)
                    + translateRaw(data.autoOutput ? "waila.ffs.on" : "waila.ffs.off")
                    + SpecialChars.RESET;
                currenttip.add(translate("waila.ffs.auto_output", state));
            }
        }

        currenttip.add(translate("waila.ffs.fluid_line", fluidDisplay(data.fluidName)));
        currenttip.add(
            translate(
                "waila.ffs.amount",
                GenericUtil.intToFancyNumber(data.fluidAmount),
                GenericUtil.intToFancyNumber(data.capacity)));

        return currenttip;
    }

    @Method(modid = "Waila")
    @Override
    public boolean hasWailaAdvancedBody(ItemStack itemStack, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        WailaTankData.Data data = data(accessor);
        return data != null && data.valid && config.getConfig(CONFIG_ADVANCED);
    }

    @Method(modid = "Waila")
    @Override
    public List<String> getWailaAdvancedBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        WailaTankData.Data data = data(accessor);
        if (data == null || !data.valid) {
            return currenttip;
        }

        boolean frame = accessor.getTileEntity() instanceof TileEntityTankFrame;
        if (!frame) {
            currenttip.add(translateRaw(data.master ? "waila.ffs.master" : "waila.ffs.slave"));
            currenttip.add(translate("waila.ffs.tank_height", data.tankHeight, data.valveHeight));
        }

        if (config.getConfig(CONFIG_TEMPERATURE) && data.temperature > 0) {
            currenttip.add(translate("waila.ffs.temperature", data.temperature));
        }

        if (config.getConfig(CONFIG_DIMENSIONS) && data.width > 0) {
            currenttip.add(translate("waila.ffs.dimensions", data.width, data.height, data.depth));
        }

        return currenttip;
    }

    @Method(modid = "Waila")
    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        if (!(accessor.getTileEntity() instanceof TileEntityTankFrame) && config.getConfig(CONFIG_HINT)) {
            currenttip.add(translateRaw("waila.ffs.right_click"));
        }
        return currenttip;
    }

    @Method(modid = "Waila")
    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x,
        int y, int z) {
        TileEntityValve valve = null;
        boolean frame = false;
        if (te instanceof TileEntityValve) {
            valve = (TileEntityValve) te;
        } else if (te instanceof TileEntityTankFrame) {
            frame = true;
            valve = ((TileEntityTankFrame) te).getValve();
        }

        WailaTankData.write(tag, valve, frame);
        return tag;
    }

    private static WailaTankData.Data data(IWailaDataAccessor accessor) {
        WailaTankData.Data data = WailaTankData.read(accessor.getNBTData());
        if (data != null) {
            return data;
        }

        TileEntity te = accessor.getTileEntity();
        if (te instanceof TileEntityValve) {
            return WailaTankData.fromValve((TileEntityValve) te, false);
        }
        if (te instanceof TileEntityTankFrame) {
            return WailaTankData.fromValve(((TileEntityTankFrame) te).getValve(), true);
        }

        return null;
    }

    private static String fluidDisplay(String fluidName) {
        if (fluidName == null || fluidName.isEmpty()) {
            return translateRaw("waila.ffs.fluid_none");
        }

        Fluid fluid = FluidRegistry.getFluid(fluidName);
        return fluid == null ? fluidName : fluid.getLocalizedName(new FluidStack(fluid, 1));
    }

    private static String translate(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private static String translateRaw(String key) {
        return StatCollector.translateToLocal(key);
    }
}
