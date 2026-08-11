package net.vulkanmod.plugin;

import net.vulkanmod.plugin.hooks.Cancelable;
import net.vulkanmod.plugin.hooks.EventPriority;
import net.vulkanmod.plugin.hooks.annotations.ListenedEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class HookRegistry {

    @FunctionalInterface
    private interface Invoker {
        void invoke(Object event);
    }

    private record Subscriber(Object owner, Class<?> eventType, EventPriority priority, boolean receiveCanceled,
                              Invoker invoker) {}

    private static final List<Subscriber> SUBSCRIBERS = new ArrayList<>();
    private static final Comparator<Subscriber> BY_PRIORITY = Comparator.comparing(s -> s.priority);

    private HookRegistry() {}

    public static synchronized void register(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            ListenedEvent annotation = method.getAnnotation(ListenedEvent.class);

            if (annotation == null)
                continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1)
                throw new IllegalArgumentException(
                    method + " Invalid number of parameters. Expected 1, found " + params.length + " parameters.");

            method.setAccessible(true);
            Class<?> eventType = params[0];

            SUBSCRIBERS.add(new Subscriber(listener, eventType, annotation.priority(), annotation.receiveCanceled(), event -> {
                try {
                    method.invoke(listener, event);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(
                            "failed to register '" + method + "' on " + event.getClass().getName(), e);
                }
            }));
        }

        SUBSCRIBERS.sort(BY_PRIORITY);
    }

    public static <T> void addListener(Class<T> eventType, Consumer<T> listener) {
        addListener(eventType, EventPriority.NORMAL, false, listener);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> void addListener(Class<T> eventType, EventPriority priority, boolean receiveCanceled, Consumer<T> listener) {
        SUBSCRIBERS.add(new Subscriber(listener, eventType, priority, receiveCanceled, event -> listener.accept((T) event)));
        SUBSCRIBERS.sort(BY_PRIORITY);
    }

    public static synchronized void unregister(Object listenerOrOwner) {
        SUBSCRIBERS.removeIf(s -> s.owner == listenerOrOwner);
    }

    public static synchronized boolean post(Object event) {
        Class<?> runtimeType = event.getClass();
        boolean cancelable = event instanceof Cancelable;

        for (Subscriber subscriber : SUBSCRIBERS) {
            if (!subscriber.eventType.isAssignableFrom(runtimeType))
                continue;

            if (cancelable && ((Cancelable) event).isCanceled() && !subscriber.receiveCanceled)
                continue;

            subscriber.invoker.invoke(event);
        }

        return cancelable && ((Cancelable) event).isCanceled();
    }

    public static synchronized void clearAll() {
        SUBSCRIBERS.clear();
    }
}
