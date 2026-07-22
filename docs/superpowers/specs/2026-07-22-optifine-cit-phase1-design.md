# OptiFine CIT — Phase 1 (item + trim → model swap) — Design

Date: 2026-07-22
Status: Approved (user "go")
Scope: Native OptiFine Custom Item Textures/Models (CIT) in Volcanic, Phase 1 — the exact subset Visual Armor Trims uses.

## 1. Goal

Support OptiFine CIT resource packs natively (no CIT mod). Test pack: Visual Armor Trims v2.1 — 4500 `optifine/cit/**/*.properties`, all uniform:
```properties
type=item
matchItems=minecraft:leather_helmet
components.trim.pattern=minecraft:snout
components.trim.material=minecraft:lapis
model=minecraft:item/helmet/leather/snout/lapis
```
= "when this armor item carries this trim (pattern+material), render it with this custom item model." No CIT mod (CIT Resewn/Chime) is installed, so nothing applies today.

## 2. Why this is renderer-independent (unlike CTM)

CIT is item MODEL SELECTION, which is vanilla logic. VulkanMod's `ItemRendererM` only adds a GUI deferred-draw boundary in `render`; it does NOT touch `ItemRenderer.getModel` or model baking. So Phase 1 hooks vanilla item model resolution + NeoForge model registration — no terrain/atlas integration.

## 3. Scope

### In scope (Phase 1)
- Parse `optifine/cit/**/*.properties` (+ `mcpatcher/cit`), `type=item` only.
- Match keys: `matchItems` (item id list) + `components.trim.pattern` + `components.trim.material`.
- Action: `model=` → swap the item's baked model.
- Register the referenced custom models for baking (NeoForge `ModelEvent.RegisterAdditional`).
- Hook `ItemRenderer.getModel` → return the CIT model when a rule matches the stack's trim component.
- Fail-safe: any miss/parse error/absent model degrades to the vanilla model. Never crash.

### Out of scope (Phase 2)
- Other `type=` (armor/enchantment/elytra), `texture=` overrides (vs `model=`), `nbt.*`, `damage`, `stackSize`, `enchantments`, `hand`, weighted/multi-model.
- Non-trim `components.*` matching.

## 4. Architecture (`net.vulkanmod.render.cit`)

1. **`CitRule`** — immutable: `Set<Item> items`, `ResourceLocation trimPattern` (nullable), `ResourceLocation trimMaterial` (nullable), `ResourceLocation model`.
2. **`CitPackLoader`** — `reload(ResourceManager)`: scan + parse `type=item` rules; build `Map<Item, List<CitRule>>` index; collect `Set<ResourceLocation>` of model ids. Skip/log unsupported or malformed. `modelsToRegister()` accessor.
3. **`CitModelRegistrar`** — NeoForge `ModelEvent.RegisterAdditional` handler: ensure `CitPackLoader.reload` has run for the current RM, then register each model id as a standalone bakeable model.
4. **`Cit`** (facade) — `boolean isActive()`; `BakedModel resolve(ItemStack stack)` returns the CIT baked model or null. Reads `stack.get(DataComponents.TRIM)` → `ArmorTrim` (pattern+material Holders → ids), matches a `CitRule` for `stack.getItem()`, resolves the baked model via `ModelManager.getModel(...)`. Returns null on any miss / missing-model.
5. **Hook** — `MItemRenderer` mixin on `ItemRenderer.getModel(ItemStack, Level, LivingEntity, int)` at RETURN: if `Cit.isActive()` and `Cit.resolve(stack)` non-null, set the return value to the CIT model.

## 5. Data flow

```
reload ─► CitPackLoader.reload ─► rules index + model-id set
ModelEvent.RegisterAdditional ─► register each model id (baked by vanilla)
render item ─► ItemRenderer.getModel RETURN ─► Cit.resolve(stack)
                                                 ├─ read TRIM component (pattern,material)
                                                 ├─ match CitRule by item+pattern+material
                                                 └─ ModelManager.getModel(model id) or null
```

## 6. Key API verification points (pin during implementation)
- `ItemRenderer.getModel` exact 1.21.1 signature + that it is the central resolution for GUI/hand/dropped.
- `ModelEvent.RegisterAdditional` register method + the `ModelResourceLocation`/`ResourceLocation` type it wants; how a standalone item model id maps (`ModelResourceLocation.standalone(...)` vs `.inventory(...)`).
- `Minecraft.getInstance().getModelManager().getModel(...)` return type + missing-model sentinel.
- `DataComponents.TRIM` → `ArmorTrim.pattern()/material()` → `Holder<...>.unwrapKey().map(k -> k.location())`.
- Where Volcanic registers NeoForge mod-bus event handlers (Initializer/client init).

## 7. Failure handling
- Parser: malformed/unsupported (non-`type=item`, missing model) → skip, log once.
- Missing baked model → resolve returns null → vanilla model.
- Hook: exceptions caught → vanilla model. Never break item rendering.

## 8. Testing / validation
- Enable Visual Armor Trims in the profile. Apply a trim (e.g. snout + lapis) to a leather helmet at a smithing table; the inventory/hand item should show the custom "stylish" model. Log: `CIT: loaded N rules, M models`.
- Regression: no trim / no CIT pack → vanilla item rendering unchanged; no FPS/crash impact.

## 9. Files
New (`src/main/java/net/vulkanmod/render/cit/`): `CitRule.java`, `CitPackLoader.java`, `CitModelRegistrar.java`, `Cit.java`.
New mixin: `src/main/java/net/vulkanmod/mixin/render/entity/MItemRenderer.java` (+ register in mixins json).
Modified: `Initializer` (or client init) — register the `ModelEvent.RegisterAdditional` handler; optional `Config.citEnabled` toggle.
