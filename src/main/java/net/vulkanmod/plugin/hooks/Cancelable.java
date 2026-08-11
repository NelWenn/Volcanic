package net.vulkanmod.plugin.hooks;

public interface Cancelable {
    boolean isCanceled();
    void setCanceled(boolean canceled);
}
