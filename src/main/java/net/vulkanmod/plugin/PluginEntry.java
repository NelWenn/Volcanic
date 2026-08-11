package net.vulkanmod.plugin;

public class PluginEntry {

    private boolean                         enabled;
    private final boolean                   toggleable;
    private final RenderPipelinePlugin      plugin;

    public PluginEntry(RenderPipelinePlugin plugin, boolean toggleable) {
        this.plugin = plugin;
        this.toggleable = toggleable;
        this.enabled = true;
    }

    public RenderPipelinePlugin getPlugin() {
        return this.plugin;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isToggleable() {
        return this.toggleable;
    }
}
