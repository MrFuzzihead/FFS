package com.lordmau5.ffs.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * Direct, single-pass scan of a tank shell. Replaces the old
 * {@code Map<Position3D, ExtendedBlock>[]} cuboid (which allocated an entry for
 * every interior block and retained all three maps) with two shell lists and a
 * packed interior set.
 */
public final class TankStructure {

    public enum CellType {
        INTERIOR,
        FACE,
        EDGE
    }

    /** Minimal world view so the scan can be unit-tested without a Minecraft world. */
    public interface CellView {

        Block getBlock(int x, int y, int z);

        int getMetadata(int x, int y, int z);

        boolean isAir(int x, int y, int z);
    }

    public static final class ShellCell {

        public final Position3D pos;
        public final ExtendedBlock block;

        public ShellCell(Position3D pos, ExtendedBlock block) {
            this.pos = pos;
            this.block = block;
        }
    }

    public final List<ShellCell> edges;
    public final List<ShellCell> faces;
    /** Interior positions, packed relative to the bottom corner. */
    public final LongSet interior;
    public final int interiorCount;
    public final boolean interiorAirValid;
    public final boolean validBounds;

    private TankStructure(List<ShellCell> edges, List<ShellCell> faces, LongSet interior, int interiorCount,
        boolean interiorAirValid, boolean validBounds) {
        this.edges = edges;
        this.faces = faces;
        this.interior = interior;
        this.interiorCount = interiorCount;
        this.interiorAirValid = interiorAirValid;
        this.validBounds = validBounds;
    }

    public static TankStructure scan(World world, Position3D bottom, Position3D top) {
        if (world == null) {
            return empty();
        }

        return scan(new CellView() {

            @Override
            public Block getBlock(int x, int y, int z) {
                return world.getBlock(x, y, z);
            }

            @Override
            public int getMetadata(int x, int y, int z) {
                return world.getBlockMetadata(x, y, z);
            }

            @Override
            public boolean isAir(int x, int y, int z) {
                return world.isAirBlock(x, y, z);
            }
        }, bottom, top);
    }

    public static TankStructure scan(CellView view, Position3D bottom, Position3D top) {
        if (view == null || bottom == null || top == null) {
            return empty();
        }

        int x1 = bottom.getX();
        int y1 = bottom.getY();
        int z1 = bottom.getZ();
        int x2 = top.getX();
        int y2 = top.getY();
        int z2 = top.getZ();

        if (x1 > x2 || y1 > y2 || z1 > z2) {
            return empty();
        }

        long volume = (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
        List<ShellCell> edges = new ArrayList<>();
        List<ShellCell> faces = new ArrayList<>();
        LongSet interior = new LongSet((int) Math.min(Integer.MAX_VALUE, Math.max(4L, volume / 2L)));
        boolean interiorAirValid = true;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    CellType type = classify(x, y, z, x1, y1, z1, x2, y2, z2);

                    if (type == CellType.INTERIOR) {
                        interior.add(LongSet.pack(x - x1, y - y1, z - z1));
                        if (!view.isAir(x, y, z)) {
                            Block block = view.getBlock(x, y, z);
                            if (block == null || !block.getUnlocalizedName()
                                .equals("railcraft.residual.heat")) {
                                interiorAirValid = false;
                            }
                        }
                    } else {
                        ExtendedBlock block = new ExtendedBlock(view.getBlock(x, y, z), view.getMetadata(x, y, z));
                        ShellCell cell = new ShellCell(new Position3D(x, y, z), block);
                        if (type == CellType.EDGE) {
                            edges.add(cell);
                        } else {
                            faces.add(cell);
                        }
                    }
                }
            }
        }

        return new TankStructure(edges, faces, interior, interior.size(), interiorAirValid, true);
    }

    public static CellType classify(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2) {
        int boundaries = (x == x1 || x == x2 ? 1 : 0) + (y == y1 || y == y2 ? 1 : 0) + (z == z1 || z == z2 ? 1 : 0);

        if (boundaries == 0) {
            return CellType.INTERIOR;
        }
        if (boundaries >= 2) {
            return CellType.EDGE;
        }
        return CellType.FACE;
    }

    private static TankStructure empty() {
        return new TankStructure(Collections.emptyList(), Collections.emptyList(), new LongSet(4), 0, false, false);
    }
}
