package net.vulkanmod.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.vulkanmod.Initializer;

import java.util.concurrent.ThreadLocalRandom;

public final class UiSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Initializer.MOD_ID);

    private static final DeferredHolder<SoundEvent, SoundEvent> CLICK_ONE = register("ui.click1");
    private static final DeferredHolder<SoundEvent, SoundEvent> CLICK_TWO = register("ui.click2");
    private static final DeferredHolder<SoundEvent, SoundEvent> INTRO = register("ui.intro");

    private static final float CLICK_VOLUME = 0.5f;
    private static final float INTRO_VOLUME = 0.9f;
    private static final float PITCH_SPREAD = 0.06f;

    private static boolean introPlayed;

    private UiSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }

    public static void playClick() {
        DeferredHolder<SoundEvent, SoundEvent> pick =
                ThreadLocalRandom.current().nextBoolean() ? CLICK_ONE : CLICK_TWO;
        float pitch = 1.0f + ThreadLocalRandom.current().nextFloat(-PITCH_SPREAD, PITCH_SPREAD);
        play(pick, pitch, CLICK_VOLUME);
    }

    public static void playIntroOnce() {
        if (introPlayed) {
            return;
        }
        introPlayed = true;
        play(INTRO, 1.0f, INTRO_VOLUME);
    }

    private static void play(DeferredHolder<SoundEvent, SoundEvent> holder, float pitch, float volume) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getSoundManager() == null || !holder.isBound()) {
                return;
            }
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(holder.get(), pitch, volume));
        } catch (Throwable unavailable) {
            Initializer.LOGGER.debug("UI sound skipped: {}", unavailable.toString());
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
