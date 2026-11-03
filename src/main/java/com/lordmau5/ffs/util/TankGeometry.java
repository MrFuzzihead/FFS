package com.lordmau5.ffs.util;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Pure, world-independent tank geometry. Replaces the old in-place
 * {@code length[]} scan, which kept stale values when a direction found no wall
 * and only validated the downward axis.
 */
public final class TankGeometry {

    private TankGeometry() {}

    /** Minimal view of the world needed to measure a tank interior. */
    public interface BlockView {

        boolean isAir(int x, int y, int z);
    }

    /** Immutable result of a measurement. */
    public static final class Shape {

        public final boolean valid;
        /** Interior half-lengths, indexed by {@link ForgeDirection#ordinal()}. */
        public final int[] lengths;
        public final Position3D bottomDiag;
        public final Position3D topDiag;
        public final int tankHeight;

        private Shape(boolean valid, int[] lengths, Position3D bottomDiag, Position3D topDiag, int tankHeight) {
            this.valid = valid;
            this.lengths = lengths;
            this.bottomDiag = bottomDiag;
            this.topDiag = topDiag;
            this.tankHeight = tankHeight;
        }

        static Shape invalid() {
            return new Shape(false, new int[6], null, null, 0);
        }
    }

    /**
     * Measures the interior starting at {@code valve + inside}.
     *
     * <p>
     * The old implementation kept a mutable {@code length} array and only tested
     * {@code length[DOWN] != -1}, so a missing wall could leave stale values behind
     * and an open area could pass. This method allocates a fresh array, requires a
     * wall in all six directions, and enforces the documented size limit
     * <strong>including the frame</strong> (full axis size = halfA + halfB + 3).
     *
     * @param maxSize maximum full tank size per axis, including the frame
     */
    public static Shape measure(BlockView view, int x, int y, int z, ForgeDirection inside, int maxSize) {
        if (view == null || inside == null || inside == ForgeDirection.UNKNOWN || maxSize < 3) {
            return Shape.invalid();
        }

        int xIn = x + inside.offsetX;
        int yIn = y + inside.offsetY;
        int zIn = z + inside.offsetZ;

        // The block immediately inside the valve must be air; otherwise the
        // clicked face does not point into a tank interior.
        if (!view.isAir(xIn, yIn, zIn)) {
            return Shape.invalid();
        }

        int[] lengths = new int[6];
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            int foundAt = -1;
            for (int i = 0; i < maxSize; i++) {
                if (!view.isAir(xIn + dir.offsetX * i, yIn + dir.offsetY * i, zIn + dir.offsetZ * i)) {
                    foundAt = i;
                    break;
                }
            }

            if (foundAt < 0) {
                // No wall within the allowed distance: this is not a closed tank.
                return Shape.invalid();
            }

            lengths[dir.ordinal()] = foundAt - 1;
        }

        // Full axis size is halfA + halfB + 3 (the valve side, the opposite wall
        // and the two frame layers). MAX_SIZE documents the whole tank size.
        if (!fitsAxis(lengths, ForgeDirection.WEST, ForgeDirection.EAST, maxSize)
            || !fitsAxis(lengths, ForgeDirection.DOWN, ForgeDirection.UP, maxSize)
            || !fitsAxis(lengths, ForgeDirection.NORTH, ForgeDirection.SOUTH, maxSize)) {
            return Shape.invalid();
        }

        Position3D bottom = new Position3D(
            xIn + lengths[ForgeDirection.WEST.ordinal()] * ForgeDirection.WEST.offsetX + ForgeDirection.WEST.offsetX,
            yIn + lengths[ForgeDirection.DOWN.ordinal()] * ForgeDirection.DOWN.offsetY + ForgeDirection.DOWN.offsetY,
            zIn + lengths[ForgeDirection.NORTH.ordinal()] * ForgeDirection.NORTH.offsetZ
                + ForgeDirection.NORTH.offsetZ);
        Position3D top = new Position3D(
            xIn + lengths[ForgeDirection.EAST.ordinal()] * ForgeDirection.EAST.offsetX + ForgeDirection.EAST.offsetX,
            yIn + lengths[ForgeDirection.UP.ordinal()] * ForgeDirection.UP.offsetY + ForgeDirection.UP.offsetY,
            zIn + lengths[ForgeDirection.SOUTH.ordinal()] * ForgeDirection.SOUTH.offsetZ
                + ForgeDirection.SOUTH.offsetZ);

        int tankHeight = top.getY() - bottom.getY() - 1;
        if (tankHeight < 0) {
            return Shape.invalid();
        }

        return new Shape(true, lengths, bottom, top, tankHeight);
    }

    private static boolean fitsAxis(int[] lengths, ForgeDirection negative, ForgeDirection positive, int maxSize) {
        return lengths[negative.ordinal()] + lengths[positive.ordinal()] + 3 <= maxSize;
    }
}
