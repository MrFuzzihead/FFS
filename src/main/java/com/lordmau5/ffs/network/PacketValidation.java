package com.lordmau5.ffs.network;

/** Pure validation helpers for client-to-server packets. */
public final class PacketValidation {

    /** Maximum distance (blocks) between the player and the targeted valve. */
    public static final double MAX_REACH = 8.0D;

    /** Maximum stored valve-name length. The GUI allows 18; leave headroom. */
    public static final int MAX_NAME_LENGTH = 32;

    private PacketValidation() {}

    public static boolean isWithinReach(int x, int y, int z, double playerX, double playerY, double playerZ,
        double maxDistance) {
        double dx = (x + 0.5D) - playerX;
        double dy = (y + 0.5D) - playerY;
        double dz = (z + 0.5D) - playerZ;
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance;
    }

    public static boolean isValidValveName(String name) {
        return name != null && !name.isEmpty() && name.length() <= MAX_NAME_LENGTH;
    }
}
