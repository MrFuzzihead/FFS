package com.lordmau5.ffs.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 1.7.10 FML invokes custom channel handlers on the Netty thread. World access
 * must be marshalled onto the server thread; this queue drains once per server
 * tick (dedicated and integrated servers both fire {@link TickEvent.ServerTickEvent}).
 */
public final class ServerTaskQueue {

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    public ServerTaskQueue() {}

    public static void schedule(Runnable task) {
        if (task != null) {
            TASKS.add(task);
        }
    }

    public static void drain() {
        Runnable task;
        while ((task = TASKS.poll()) != null) {
            try {
                task.run();
            } catch (Throwable t) {
                try {
                    FMLLog.log(Level.ERROR, t, "FFS: scheduled server task failed");
                } catch (Throwable loggingFailure) {
                    // FMLLog is not initialised in the headless unit-test JVM.
                    t.printStackTrace();
                }
            }
        }
    }

    public static int size() {
        return TASKS.size();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            drain();
        }
    }
}
