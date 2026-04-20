package com.lordmau5.ffs.compat;

import net.minecraft.launchwrapper.Launch;

import com.lordmau5.ffs.FancyFluidStorage;

import cpw.mods.fml.common.Loader;
import de.npe.gameanalytics.minecraft.MCSimpleAnalytics;

public class FFSAnalytics extends MCSimpleAnalytics {

    static String GAME_KEY = "2b36a1907820e76a137e5922205123f5";
    static String SECRET_KEY = "2562f324cc5257e84df00399f2132cdf017379c1";

    public FFSAnalytics() {
        super(
            ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) ? "1.7.10-1.3.3"
                : Loader.instance()
                    .activeModContainer()
                    .getVersion(),
            GAME_KEY,
            SECRET_KEY);
    }

    @Override
    public boolean isActive() {
        return FancyFluidStorage.instance.ANONYMOUS_STATISTICS && super.isActive();
    }

    public void event(FFSAnalytics.Category cat, FFSAnalytics.Event event) {
        if (this.isActive()) {
            this.eventDesign(cat.name() + ":" + event.name());
        }
    }

    public void event(FFSAnalytics.Category cat, FFSAnalytics.Event event, Number number) {
        if (this.isActive()) {
            this.eventDesign(cat.name() + ":" + event.name(), number);
        }
    }

    public static enum Category {
        TANK;
    }

    public static enum Event {
        TANK_BUILD,
        TANK_BREAK,
        FLUID_INTAKE,
        FLUID_OUTTAKE,
        RAIN_INTAKE;
    }
}
