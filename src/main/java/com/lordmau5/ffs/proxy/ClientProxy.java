package com.lordmau5.ffs.proxy;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.lordmau5.ffs.client.TankFrameRenderer;
import com.lordmau5.ffs.client.ValveRenderer;
import com.lordmau5.ffs.compat.WailaPluginTank;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.ModNames;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Loader;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        ValveRenderer vr = new ValveRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityValve.class, vr);
        RenderingRegistry.registerBlockHandler(vr);
        RenderingRegistry.registerBlockHandler(new TankFrameRenderer());
    }

    @Override
    public void init() {
        if (Loader.isModLoaded("Waila")) {
            WailaPluginTank.init();
        }

        super.init();
    }

    @Override
    public void registerIcons(IIconRegister iR) {
        this.tex_Valve = iR.registerIcon(ModNames.resource("blockValve"));
        this.tex_ValveItem = iR.registerIcon(ModNames.resource("blockValve_Item"));
        this.tex_SlaveValve = new IIcon[] { iR.registerIcon(ModNames.resource("tankValve_0")),
            iR.registerIcon(ModNames.resource("tankValve_1")) };
        this.tex_MasterValve = new IIcon[] { iR.registerIcon(ModNames.resource("tankMaster_0")),
            iR.registerIcon(ModNames.resource("tankMaster_1")) };
    }
}
