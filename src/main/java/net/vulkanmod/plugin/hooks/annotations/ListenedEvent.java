package net.vulkanmod.plugin.hooks.annotations;

import net.vulkanmod.plugin.hooks.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ListenedEvent {

    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
