package com.lordmau5.ffs.compat.computercraft;

import com.lordmau5.ffs.holder.FFSBlockEntities;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CompatibilityComputerCraft {

    public static void initialize(IEventBus bus) {
        bus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(
                    PeripheralCapability.get(),
                    FFSBlockEntities.tankComputer.get(),
                    (blockEntity, _class) -> new TankComputerPeripheral(blockEntity)
            );
        });
    }
}
