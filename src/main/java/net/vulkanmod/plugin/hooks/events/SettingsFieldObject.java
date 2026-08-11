package net.vulkanmod.plugin.hooks.events;

public class SettingsFieldObject<T> {
    public String   nameKey;
    public String   descriptionKey;
    public T        value;

    public SettingsFieldObject(String nameKey, String descriptionKey, T value) {
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.value = value;
    }
}
