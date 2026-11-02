package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;
import com.lordmau5.ffs.util.Position3D;

class TankOwnershipTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void masterFlagAndLinkLifecycle() {
        TankOwnership ownership = new TankOwnership();
        TileEntityValve valve = new TileEntityValve();

        assertFalse(ownership.isMaster());
        assertNull(ownership.getMaster(valve));
        assertNull(ownership.getMasterValvePos());

        ownership.setMasterPos(new Position3D(1, 2, 3));
        assertEquals(new Position3D(1, 2, 3), ownership.getMasterValvePos());

        ownership.setMaster(true);
        assertTrue(ownership.isMaster());
        assertNull(ownership.getMasterValvePos());
        assertSame(valve, ownership.getMaster(valve));

        ownership.setMaster(false);
        assertFalse(ownership.isMaster());
        assertNull(ownership.getMaster(valve));
    }

    @Test
    void valveListsIncludeTheMasterAndSlavesOnce() {
        TankOwnership ownership = new TankOwnership();
        ownership.setMaster(true);

        TileEntityValve master = new TileEntityValve();
        TileEntityValve first = new TileEntityValve();
        TileEntityValve second = new TileEntityValve();
        master.setValveName("master");
        first.setValveName("first");
        second.setValveName("second");

        List<TileEntityValve> slaves = new ArrayList<>();
        slaves.add(first);
        slaves.add(second);
        slaves.add(master);

        List<TileEntityValve> all = ownership.getAllValves(master, slaves);
        assertEquals(3, all.size());
        assertSame(master, all.get(0));
        assertSame(first, all.get(1));
        assertSame(second, all.get(2));
    }

    @Test
    void nameFilteringIsCaseInsensitive() {
        TankOwnership ownership = new TankOwnership();
        ownership.setMaster(true);

        TileEntityValve master = new TileEntityValve();
        TileEntityValve slave = new TileEntityValve();
        master.setValveName("Main");
        slave.setValveName("Side");

        List<TileEntityValve> slaves = new ArrayList<>();
        slaves.add(slave);

        assertEquals(
            1,
            ownership.getValvesByName(master, slaves, "side")
                .size());
        assertEquals(
            1,
            ownership.getValvesByName(master, slaves, "MAIN")
                .size());
        assertTrue(
            ownership.getValvesByName(master, slaves, "missing")
                .isEmpty());
    }
}
