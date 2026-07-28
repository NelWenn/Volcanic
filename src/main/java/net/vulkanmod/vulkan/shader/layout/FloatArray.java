package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

public class FloatArray extends Uniform {

    public FloatArray(Info info) {
        super(info);
    }

    void setSupplier() {
        this.values = Uniforms.floatArr_uniformMap.get(this.info.name);
    }

    void update(long ptr) {
        MappedBuffer src = values.get();
        MemoryUtil.memCopy(src.ptr, ptr + this.offset, Math.min(this.size, src.buffer.limit()));
    }
}
