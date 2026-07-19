package net.vulkanmod.render.pack;

import java.util.Map;

public class PackPass {
    public final String name;
    public final String program;
    public final Map<Integer, String> inputs;
    public final String output;

    public PackPass(String name, String program, Map<Integer, String> inputs, String output) {
        this.name = name;
        this.program = program;
        this.inputs = inputs;
        this.output = output;
    }
}
