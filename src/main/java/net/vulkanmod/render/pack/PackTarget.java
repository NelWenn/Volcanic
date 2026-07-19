package net.vulkanmod.render.pack;

public class PackTarget {
    public final String name;
    public final String format;
    public final float scale;
    public final boolean pingpong;
    public final float clear;

    public PackTarget(String name, String format, float scale, boolean pingpong, float clear) {
        this.name = name;
        this.format = format;
        this.scale = scale;
        this.pingpong = pingpong;
        this.clear = clear;
    }
}
