package com.lordmau5.ffs.holder;

import com.lordmau5.ffs.FancyFluidStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class FFSSounds {

    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, FancyFluidStorage.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> birdSounds = register("bird", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "bird")));

    private static <T extends SoundEvent> DeferredHolder<SoundEvent, SoundEvent> register(final String name, final Supplier<T> sound) {
        return SOUND_EVENTS.register(name, sound);
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
