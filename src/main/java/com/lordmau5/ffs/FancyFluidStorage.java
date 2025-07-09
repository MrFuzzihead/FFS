package com.lordmau5.ffs;

import com.lordmau5.ffs.blockentity.tanktiles.BlockEntityTankComputer;
import com.lordmau5.ffs.blockentity.valves.BlockEntityFluidValve;
import com.lordmau5.ffs.compat.Compatibility;
import com.lordmau5.ffs.compat.computercraft.TankComputerPeripheral;
import com.lordmau5.ffs.config.ServerConfig;
import com.lordmau5.ffs.datagen.DataGenerators;
import com.lordmau5.ffs.holder.*;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.util.Config;
import com.lordmau5.ffs.util.GenericUtil;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(FancyFluidStorage.MOD_ID)
public class FancyFluidStorage {
    public static final String MOD_ID = "ffs";

    public static final TagKey<Block> TANK_BLACKLIST = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "invalid_for_tank"));

    public FancyFluidStorage(IEventBus bus, ModContainer container) {
        bus.addListener(this::setupClient);
        bus.addListener(this::registerCreativeTab);

        FFSBlocks.register(bus);
        FFSItems.register(bus);
        FFSBlockEntities.register(bus);
        FFSSounds.register(bus);
        FFSDataComponentType.register(bus);
        NetworkHandler.init(bus);

        bus.register(FFSBlockRendererManager.class);
        bus.register(FFSBlocks.class);
        bus.register(DataGenerators.class);

        GenericUtil.init();
        Compatibility.init(bus);

        bus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FFSBlockEntities.tileEntityFluidValve.get(), BlockEntityFluidValve::getFluidHandler);
        });

        container.registerConfig(ModConfig.Type.SERVER, Config.walkClass(ServerConfig.class, bus));
    }

    private void setupClient(final FMLClientSetupEvent event) {
        Compatibility.initClient();
    }

    private void registerCreativeTab(RegisterEvent event) {
        ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_tab"));
        event.register(Registries.CREATIVE_MODE_TAB, creativeModeTabRegisterHelper ->
        {
            creativeModeTabRegisterHelper.register(TAB, CreativeModeTab.builder().icon(() -> new ItemStack(FFSBlocks.fluidValve.get()))
                    .title(Component.translatable("itemGroup.ffs"))
                    .displayItems((params, output) -> {
                        FFSItems.ITEMS.getEntries().forEach(itemRegistryObject -> output.accept(new ItemStack(itemRegistryObject.get())));
                        FFSBlocks.BLOCKS.getEntries().forEach(itemRegistryObject -> output.accept(new ItemStack(itemRegistryObject.get())));
                    })
                    .build());
        });
    }
}
