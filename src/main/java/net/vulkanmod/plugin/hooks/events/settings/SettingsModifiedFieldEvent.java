package net.vulkanmod.plugin.hooks.events.settings;

import net.vulkanmod.plugin.hooks.events.SettingsFieldObject;

public interface SettingsModifiedFieldEvent<T> {
    long                   when();
    T                      type();
    SettingsFieldObject<T> field();
}
