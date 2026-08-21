package net.vulkanmod.plugin.hooks.events;

import net.vulkanmod.plugin.hooks.Cancelable;

public class CancelableEvent implements Cancelable {

    private boolean canceled;

    @Override
    public final boolean isCanceled() {
        return canceled;
    }

    @Override
    public final void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public final void cancel() {
        this.canceled = true;
    }
}
