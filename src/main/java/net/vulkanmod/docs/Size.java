package net.vulkanmod.docs;

public @interface Size {
    int value()     default 0;
    int min()       default 0;
    float step()    default 0.1f;
    int max();
}
