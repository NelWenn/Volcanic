package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;

// float block laid out as a std140 vec4 array, copied whole like mat4/vec4
public class FloatArray extends Uniform {

    public FloatArray(Info info) {
        super(info);
    }

    void setSupplier() {
        this.values = Uniforms.floatArr_uniformMap.get(this.info.name);
    }
}
