package com.lordmau5.ffs.tile;

import com.lordmau5.ffs.util.TankMath;

/** Per-valve auto-output flag plus the throttled client-sync bookkeeping. */
public final class AutoOutputState {

    private boolean enabled;
    private boolean pendingSync;
    private long lastSyncTick = -1L;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPendingSync() {
        return this.pendingSync;
    }

    public void requestSync() {
        this.pendingSync = true;
    }

    /** @return true when a sync should be sent now; consumes the pending flag */
    public boolean consumeSync(long now, int minIntervalTicks) {
        if (!this.pendingSync || !TankMath.shouldSyncFluid(now, this.lastSyncTick, minIntervalTicks)) {
            return false;
        }

        this.pendingSync = false;
        this.lastSyncTick = now;
        return true;
    }
}
