package com.lordmau5.ffs.client;

import net.minecraftforge.common.util.ForgeDirection;

/** Pure face-selection rule for the valve's accent texture. */
public final class ValveFace {

    private ValveFace() {}

    /** The accent is drawn on the face that points into the tank interior. */
    public static boolean isActiveFace(ForgeDirection inside, int side) {
        return inside != null && inside != ForgeDirection.UNKNOWN && side == inside.ordinal();
    }
}
