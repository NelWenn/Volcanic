# OptiFine CIT Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans / subagent-driven. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Natively render OptiFine CIT `type=item` rules that swap an armor item's model based on its trim (pattern+material), so packs like Visual Armor Trims work under Volcanic with no CIT mod.

**Architecture:** Reload-time `CitPackLoader` parses `optifine/cit/**` into `CitRule`s indexed by `Item`; a NeoForge `ModelEvent.RegisterAdditional` handler registers the referenced custom models for baking; a mixin on `ItemRenderer.getModel` returns the CIT baked model when the stack's `minecraft:trim` component matches a rule. Renderer-independent (VulkanMod does not touch item model selection).

**Tech Stack:** Java 21, NeoForge 1.21.1, SpongePowered Mixin, NeoForge client mod-bus events.

## Global Constraints
- ZERO comments in any code. No AI/Claude mention anywhere. Commit as `NelWenn <NelWenn@users.noreply.github.com>`.
- Branch `optifine-ctm` (continue on it; CIT rides after the CTM commits) — do NOT switch to main.
- Build: `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home ./gradlew build -x test`.
- Deploy: `cp build/libs/Volcanic-1.21.1-*.jar "$HOME/Library/Application Support/ModrinthApp/profiles/NeoForge 1.21.1/mods/"Volcanic-1.21.1-0.1.1-alpha.jar`.
- New package `net.vulkanmod.render.cit`. Fail-safe everywhere: any error degrades to the vanilla model; never crash item rendering.

## File Structure
New: `render/cit/CitRule.java`, `render/cit/CitPackLoader.java`, `render/cit/CitModelRegistrar.java`, `render/cit/Cit.java`, `mixin/render/entity/MItemRenderer.java`.
Modified: mixins json (register `MItemRenderer`); `Initializer` or client init (register the model event handler); optional `Config.citEnabled`.

---

## Task 1: `CitRule` + `CitPackLoader`

**Files:** Create `src/main/java/net/vulkanmod/render/cit/CitRule.java`, `src/main/java/net/vulkanmod/render/cit/CitPackLoader.java`.

**Interfaces:**
- Produces: `record CitRule(java.util.Set<net.minecraft.world.item.Item> items, ResourceLocation trimPattern, ResourceLocation trimMaterial, ResourceLocation model)` with `boolean matches(Item item, ResourceLocation pattern, ResourceLocation material)`.
- Produces: `CitPackLoader.reload(ResourceManager)`; `List<CitRule> rulesFor(Item)`; `Set<ResourceLocation> modelsToRegister()`; `int ruleCount()`.

- [ ] **Step 1: Create `CitRule.java`**
```java
package net.vulkanmod.render.cit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Set;

public record CitRule(Set<Item> items, ResourceLocation trimPattern, ResourceLocation trimMaterial, ResourceLocation model) {
    public boolean matches(Item item, ResourceLocation pattern, ResourceLocation material) {
        if (!items.isEmpty() && !items.contains(item)) return false;
        if (trimPattern != null && !trimPattern.equals(pattern)) return false;
        if (trimMaterial != null && !trimMaterial.equals(material)) return false;
        return true;
    }
}
```

- [ ] **Step 2: Create `CitPackLoader.java`**
```java
package net.vulkanmod.render.cit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.vulkanmod.Initializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class CitPackLoader {
    private static Map<Item, List<CitRule>> byItem = Map.of();
    private static Set<ResourceLocation> models = Set.of();

    private CitPackLoader() {}

    public static void reload(ResourceManager rm) {
        Map<Item, List<CitRule>> index = new HashMap<>();
        Set<ResourceLocation> modelSet = new HashSet<>();
        int count = 0;
        try {
            Map<ResourceLocation, Resource> found = rm.listResources("optifine/cit",
                    p -> p.getPath().endsWith(".properties"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                try (InputStream in = e.getValue().open()) {
                    Properties p = new Properties();
                    p.load(in);
                    CitRule rule = parse(p);
                    if (rule == null) continue;
                    for (Item it : rule.items()) index.computeIfAbsent(it, k -> new ArrayList<>()).add(rule);
                    modelSet.add(rule.model());
                    count++;
                } catch (Throwable t) {
                    Initializer.LOGGER.warn("CIT: failed to parse {}", e.getKey());
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("CIT: resource scan failed", t);
        }
        byItem = index;
        models = modelSet;
        Initializer.LOGGER.info("CIT: loaded {} rules, {} models", count, modelSet.size());
    }

    private static CitRule parse(Properties p) {
        if (!"item".equalsIgnoreCase(p.getProperty("type", "item"))) return null;
        String modelStr = p.getProperty("model");
        if (modelStr == null) return null;
        ResourceLocation model = ResourceLocation.parse(modelStr.trim());

        Set<Item> items = new HashSet<>();
        String matchItems = p.getProperty("matchItems");
        if (matchItems != null) {
            for (String s : matchItems.trim().split("\\s+")) {
                ResourceLocation id = ResourceLocation.parse(s.contains(":") ? s : "minecraft:" + s);
                Item it = BuiltInRegistries.ITEM.get(id);
                if (it != Items.AIR) items.add(it);
            }
        }
        ResourceLocation pattern = idOrNull(p.getProperty("components.trim.pattern"));
        ResourceLocation material = idOrNull(p.getProperty("components.trim.material"));
        if (items.isEmpty() && pattern == null && material == null) return null;
        return new CitRule(items, pattern, material, model);
    }

    private static ResourceLocation idOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return ResourceLocation.parse(t.contains(":") ? t : "minecraft:" + t);
    }

    public static List<CitRule> rulesFor(Item item) { return byItem.getOrDefault(item, List.of()); }
    public static Set<ResourceLocation> modelsToRegister() { return models; }
    public static int ruleCount() { int n = 0; for (List<CitRule> l : byItem.values()) n += l.size(); return n; }
}
```

- [ ] **Step 3: Compile** — `./gradlew compileJava` → SUCCESS (fix any 1.21 API mismatch minimally: `BuiltInRegistries.ITEM.get` returns `Items.AIR` for unknown, hence the guard).
- [ ] **Step 4: Commit** — `git commit -m "Add CIT rule model and pack loader"`.

---

## Task 2: `Cit` facade (trim-component resolve)

**Files:** Create `src/main/java/net/vulkanmod/render/cit/Cit.java`.

**Interfaces:**
- Consumes: `CitPackLoader.rulesFor(Item)`.
- Produces: `Cit.isActive()`, `Cit.resolve(ItemStack) -> BakedModel` (null if none).

- [ ] **Step 1: Create `Cit.java`**
```java
package net.vulkanmod.render.cit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

import java.util.List;

public final class Cit {
    private Cit() {}

    public static boolean isActive() {
        return CitPackLoader.ruleCount() > 0 && net.vulkanmod.Initializer.CONFIG.citEnabled;
    }

    public static BakedModel resolve(ItemStack stack) {
        try {
            List<CitRule> rules = CitPackLoader.rulesFor(stack.getItem());
            if (rules.isEmpty()) return null;
            ArmorTrim trim = stack.get(DataComponents.TRIM);
            ResourceLocation pattern = null, material = null;
            if (trim != null) {
                pattern = trim.pattern().unwrapKey().map(ResourceKey::location).orElse(null);
                material = trim.material().unwrapKey().map(ResourceKey::location).orElse(null);
            }
            for (CitRule rule : rules) {
                if (rule.matches(stack.getItem(), pattern, material)) {
                    BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(rule.model()));
                    BakedModel missing = Minecraft.getInstance().getModelManager().getMissingModel();
                    return (model == null || model == missing) ? null : model;
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }
}
```

- [ ] **Step 2: Compile** — `./gradlew compileJava`. VERIFY the risky 1.21.1 APIs and fix minimally if wrong: `stack.get(DataComponents.TRIM)` returns `ArmorTrim`; `ArmorTrim.pattern()`/`material()` return `Holder<TrimPattern>`/`Holder<TrimMaterial>` with `.unwrapKey()`; `ModelManager.getModel(ModelResourceLocation)` + `ModelManager.getMissingModel()`; `ModelResourceLocation.standalone(ResourceLocation)`. If `citEnabled` is absent on `Config`, add `public boolean citEnabled = true;` in this task's commit.
- [ ] **Step 3: Commit** — `git commit -m "Add CIT resolver reading the trim component"`.

---

## Task 3: Model registration (`ModelEvent.RegisterAdditional`)

**Files:** Create `src/main/java/net/vulkanmod/render/cit/CitModelRegistrar.java`. Modify the mod init to register the handler.

**Interfaces:** Consumes `CitPackLoader.reload`, `CitPackLoader.modelsToRegister`.

- [ ] **Step 1: Pin the NeoForge API** — via javap/source confirm the 1.21.1 `net.neoforged.neoforge.client.event.ModelEvent$RegisterAdditional` register method and argument type (`ModelResourceLocation` vs `ResourceLocation`), and how the registered id must match what `ModelManager.getModel` uses in Task 2 (they MUST be the same key form — `ModelResourceLocation.standalone(rl)`). Also confirm how Volcanic registers client mod-bus event handlers (inspect `net.vulkanmod.Initializer` and any `@EventBusSubscriber`/`modEventBus.addListener` usage).

- [ ] **Step 2: Create `CitModelRegistrar.java`** — a handler that, on `ModelEvent.RegisterAdditional`: calls `CitPackLoader.reload(Minecraft.getInstance().getResourceManager())` (so `modelsToRegister()` is populated for the current packs), then registers each id:
```java
package net.vulkanmod.render.cit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class CitModelRegistrar {
    private CitModelRegistrar() {}

    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        try {
            CitPackLoader.reload(Minecraft.getInstance().getResourceManager());
            for (ResourceLocation id : CitPackLoader.modelsToRegister()) {
                event.register(ModelResourceLocation.standalone(id));
            }
        } catch (Throwable t) {
            net.vulkanmod.Initializer.LOGGER.warn("CIT: model registration failed", t);
        }
    }
}
```
(Adapt `event.register(...)` to the exact signature found in Step 1.)

- [ ] **Step 3: Register the handler on the client mod event bus** — in Volcanic's mod init (matching the existing pattern found in Step 1), add `modEventBus.addListener(CitModelRegistrar::onRegisterAdditional);` (client side only).

- [ ] **Step 4: Build** — `./gradlew build -x test` → SUCCESS (catches the event signature).
- [ ] **Step 5: Commit** — `git commit -m "Register CIT custom models for baking via ModelEvent"`.

---

## Task 4: Hook `ItemRenderer.getModel` + config toggle

**Files:** Create `src/main/java/net/vulkanmod/mixin/render/entity/MItemRenderer.java`. Modify mixins json. (Config `citEnabled` added in Task 2.)

- [ ] **Step 1: Confirm `getModel` signature** — javap/source `net.minecraft.client.renderer.entity.ItemRenderer#getModel` in 1.21.1 (expected `public BakedModel getModel(ItemStack, @Nullable Level, @Nullable LivingEntity, int)`).

- [ ] **Step 2: Create the mixin**
```java
package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.vulkanmod.render.cit.Cit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class MItemRenderer {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void volcanic$citModel(ItemStack stack, Level level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        if (!Cit.isActive()) return;
        BakedModel cit = Cit.resolve(stack);
        if (cit != null) cir.setReturnValue(cit);
    }
}
```
(Match the exact param list found in Step 1.)

- [ ] **Step 3: Register the mixin** in the mixins json (mirror how `ItemRendererM` is listed).

- [ ] **Step 4: Build + deploy** — build, then `cp` the jar into the profile.

- [ ] **Step 5: In-game validation** — user enables Visual Armor Trims, applies a snout+lapis trim to a leather helmet (smithing table), checks the inventory/hand item shows the custom model. Log: `CIT: loaded N rules, M models`. No trim / disabled → vanilla; no crash.

- [ ] **Step 6: Commit** — `git commit -m "Swap item model for CIT trim rules in ItemRenderer"`.

---

## Self-Review Notes
- Coverage: §4.2 loader → Task 1; §4.4 resolve → Task 2; §4.3 registration → Task 3; §4.5 hook → Task 4; §7 fail-safe → try/catch in Cit.resolve + mixin guard; toggle → Task 2/4.
- Verification points (risky APIs): `ModelEvent.RegisterAdditional.register` type + key form (Task 3.1) — MUST match `ModelResourceLocation.standalone(...)` used in `getModel` retrieval (Task 2); `ItemRenderer.getModel` signature (Task 4.1); `ArmorTrim` accessors + `ModelManager.getMissingModel` (Task 2.2). These are isolated to their tasks with explicit "confirm via javap" steps.
- Type consistency: model id registered (Task 3) and retrieved (Task 2) both via `ModelResourceLocation.standalone(rule.model())`. `Cit.isActive/resolve` identical across Tasks 2 and 4.
