package net.vulkanmod.render.pack;

import java.util.List;

public class ShaderPack {
    public final String id;
    public final String name;
    public final List<PackTarget> targets;
    public final List<PackPass> passes;

    public ShaderPack(String id, String name, List<PackTarget> targets, List<PackPass> passes) {
        this.id = id;
        this.name = name;
        this.targets = targets;
        this.passes = passes;
    }

    public PackPass pass(String name) {
        for (PackPass p : this.passes) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }
}
