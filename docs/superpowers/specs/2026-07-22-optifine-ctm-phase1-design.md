# OptiFine CTM — Phase 1 (sprite swap + overlays) — Design

Date: 2026-07-22
Status: Draft for approval
Scope: Native OptiFine Connected-Textures (CTM) support in Volcanic's terrain renderer, Phase 1 only.

## 1. Goal & motivation

Volcanic (VulkanMod fork) replaces the vanilla/Sodium terrain renderer with its own `BlockRenderer`. Any mod that hooks vanilla/Sodium block rendering to swap textures is therefore bypassed. Nature X — like most modern packs — is largely an **OptiFine CTM pack** (1405 `.properties` files under `assets/minecraft/optifine/ctm/`). On the user's Sodium profile these render via the **Continuity** mod; under Volcanic nothing renders them, so Nature X looks flat: no flowers on trees, no ground "carpet" overlay, uniform grass instead of per-biome variation.

Phase 1 delivers the OptiFine CTM methods that produce those exact visible effects, natively, inside Volcanic's mesher — with no dependency on Continuity.

## 2. Scope

### In scope (Phase 1)
- Parse `optifine/ctm/**/*.properties` (and `mcpatcher/ctm/**`) from every loaded resource pack, in pack order.
- Methods: `fixed`, `random`, `repeat`, `overlay_fixed`, `overlay_random`.
- Matching: `matchTiles`, `matchBlocks`, `matchTiles`+`matchBlocks`.
- Filters: `biomes` (incl. negation `!x`), `faces`, `heights`/`minHeight`/`maxHeight`, `weights`.
- Tinting of overlays via `tintIndex` (+ `tintBlock`), falling back to no tint.
- Stitch referenced OptiFine tiles into the block atlas.
- Hook Volcanic's `BlockRenderer.renderModelFace`: UV-remap swapped sprites; emit overlay quads into the correct render layer.
- Per-section resolution cache for performance.

### Out of scope (Phase 2, separate spec)
- Connected methods: `ctm`, `ctm_compact`, `horizontal`, `vertical`, `horizontal+vertical`, `top` (need neighbour-connection + 47/tile mapping).
- `overlay` / `overlay_ctm` (connected overlays).
- Emissive textures.
- CIT (separate subsystem, separate cycle).

### Non-goals
- Byte-for-byte parity with OptiFine edge cases. Target: the common, visible behaviours packs actually use.

## 3. Architecture

Four isolated units. Each has one purpose, a narrow interface, and is testable in isolation.

```
resource reload ─► CtmPackLoader ─► List<CtmProperties>  (indexed)
                        │
                        └─► CtmAtlasRegistrar ─► sprites added to block atlas

chunk mesh (per quad) ─► CtmResolver.resolve(ctx) ─► CtmResult
                                                       │
BlockRenderer.renderModelFace ◄────────────────────────┘
   ├─ swap: remap quad UVs to result sprite
   └─ overlay: emit extra quad(s) into overlay layer buffer
```

### 3.1 `CtmPackLoader` (package `net.vulkanmod.render.ctm`)
- Runs at resource reload (hook the same reload path already used for Polytone/Camille; a `PreparableReloadListener` or the existing `GameRendererMixin` reload hook).
- Walks `ResourceManager.listResources("optifine/ctm", p -> p.endsWith(".properties"))` plus the `mcpatcher/ctm` alias.
- Parses each into an immutable `CtmProperties`. Invalid files are logged once and skipped (never crash — mirrors PolytoneCompat robustness).
- Builds two lookup indexes for the resolver:
  - `Map<ResourceLocation spriteId, List<CtmProperties>>` from `matchTiles`.
  - `Map<Block, List<CtmProperties>>` from `matchBlocks`.
  - Lists preserve pack order (highest pack priority first); resolver takes the first matching entry.

### 3.2 `CtmProperties` (immutable record/class)
Fields (Phase 1 subset):
- `Method method` (enum: FIXED, RANDOM, REPEAT, OVERLAY_FIXED, OVERLAY_RANDOM)
- `Set<ResourceLocation> matchTiles`, `Set<Block> matchBlocks`
- `List<ResourceLocation> tiles` (resolved sprite ids, in declared order)
- `int[] weights` (parallel to tiles; default all-1)
- `EnumSet<Direction> faces` (default all)
- `BiomeMatcher biomes` (nullable = any)
- `int minHeight, maxHeight` (default MIN/MAX)
- `int tintIndex` (default -1), `Block tintBlock` (nullable)
- `TerrainRenderType overlayLayer` (overlays only; default CUTOUT_MIPPED)
- resolution helpers: `boolean matches(sprite, state, dir, pos, biome)`.

### 3.3 `CtmAtlasRegistrar`
- Problem: OptiFine tiles live outside `textures/` and are not stitched by vanilla.
- Approach: at block-atlas stitch time, add each referenced tile as a sprite. Reuse Volcanic's existing atlas mixin surface (`MTextureAtlas`) — the same insertion point the PBR work uses to add its parallel atlas entries (`PbrAtlas`).
- Tile → sprite id: resolve OptiFine tile syntax (`N` → sibling `N.png`; `path`/`~/path` relative; `ns:path` absolute) to a `ResourceLocation`, sprite id = that location minus extension (e.g. `minecraft:optifine/ctm/leaves_overlay/spruce_leaves/0`).
- Register the union of all tiles referenced by loaded `CtmProperties`. De-dup. Missing tiles log once and are dropped from the property.
- After stitch, `CtmProperties.tiles` hold ids resolvable to live `TextureAtlasSprite` via the atlas.

### 3.4 `CtmResolver`
Pure function, no side effects, given a `CtmContext`:
- input: original `TextureAtlasSprite` (from `BakedQuad.getSprite()`), `BlockState`, `BlockPos`, `Direction` (quad facing / cull face), biome id, block-atlas accessor.
- lookup: candidate `CtmProperties` from block index then tile index; first whose `matches(...)` is true wins.
- output `CtmResult`:
  - `SWAP(TextureAtlasSprite newSprite)` — for fixed/random/repeat.
  - `OVERLAY(TextureAtlasSprite overlaySprite, TerrainRenderType layer, int tintIndex)` — for overlay_*.
  - `NONE` — no CTM applies.
- selection:
  - fixed → tiles[0].
  - random / overlay_random → weighted pick indexed by a deterministic hash of `(x,y,z)` (and face when `symmetry`/`faces` demands), matching OptiFine's `Random` seeding closely enough for stable, non-flickering results.
  - repeat → tile chosen by `(x,y,z)` modulo the tile grid (`width`/`height`).
- A tile that resolves to a fully-empty/transparent sprite (common in `overlay_random` weight tables) yields `NONE` for that quad (no overlay emitted).

### 3.5 `BlockRenderer` hook
At the existing per-quad site in `renderModelFace` (where `PolytoneCompat.maybeModifyQuad` already runs):
1. Build `CtmContext` from `bakedQuad.getSprite()`, `blockState`, `blockPos`, facing, `resources.region` biome.
2. `CtmResolver.resolve(ctx)`:
   - `SWAP`: wrap the quad in a lightweight `QuadView` decorator that remaps each vertex UV from the original sprite's UV rectangle into the new sprite's rectangle: `u' = newSprite.getU((origU - s0.getU0())/s0.getWidthUV())`. Feed the decorated quad to the existing `putQuadData` path. No extra geometry.
   - `OVERLAY`: keep the base quad unchanged, then emit ONE overlay quad — same positions, UVs remapped to the overlay sprite, written to `resources.builderPack.builder(overlayLayer)`, with a tiny outward normal-bias to avoid z-fighting, and tint applied via the existing block-color path when `tintIndex >= 0`.
   - `NONE`: unchanged.
- Biome access mirrors `TintCache` (`WorldRenderer.getLevel().getBiome(pos)` / region), read once per block and passed down.

## 4. Data flow (per quad)

```
BakedQuad ─► CtmContext(sprite,state,pos,face,biome)
          ─► CtmResolver.resolve
             ├─ NONE     ─► putQuadData(originalQuad)
             ├─ SWAP     ─► putQuadData(uvRemapped(originalQuad, newSprite))
             └─ OVERLAY  ─► putQuadData(originalQuad)                        [current layer]
                         + putQuadData(uvRemapped(originalQuad, overlaySprite), tint)  [overlay layer buffer]
```

## 5. Performance

- Per-quad resolution must be cheap. Mitigations:
  - Empty-fast-path: if no CTM properties loaded, the hook is a single boolean check (like PolytoneCompat when inactive).
  - Index lookups are `Map.get` by block/sprite; most blocks have zero candidates.
  - Per-`CtmProperties` `matches` short-circuits on face/biome/height before touching tiles.
  - A small per-section memo `(stateHash, localPos, face) -> CtmResult` is optional; start without it, add only if profiling shows cost. Volcanic already has `FrameTimer` for GPU timing; CPU mesh cost is measured via existing build-time stats in `BuildTask`.

## 6. Failure handling

- Parser: malformed property → log once (`Initializer.LOGGER`), skip that file.
- Missing tile/sprite → drop from the property, log once.
- Resolver/hook: any exception is caught and degrades to `NONE` (render the original quad) — never break chunk meshing (the empty-world null-quad regression is the cautionary precedent).

## 7. Testing / validation

- Primary: in-game with Nature X in the NeoForge 1.21.1 profile (user tests). Expected visible results:
  - Flowers/detail overlays appear on leaves in the right biomes (overlay_random).
  - Ground "carpet"/rock overlays on dirt/coarse dirt (overlay_fixed/random).
  - grass_block texture varies per biome (fixed/random + biomes filter).
- Diagnostic switch: a debug flag to force-log the count of loaded CtmProperties and per-frame swap/overlay counts, to confirm the engine is active before judging visuals.
- Regression: with no CTM packs, terrain identical to today; no measurable FPS drop (empty fast-path).
- Side-by-side against the Sodium+Continuity profile for the same coordinates.

## 8. Risks & open questions

- **Atlas registration is the riskiest unit.** Getting OptiFine tiles stitched into the block atlas via the existing `MTextureAtlas` surface needs validation early; if the mixin insertion point proves unsuitable, fall back to a dedicated `SpriteSource` registered on the blocks atlas.
- **Overlay z-fighting**: outward bias vs. polygon offset; pick bias first, tune in-game.
- **Random seeding parity**: exact OptiFine RNG is not fully documented; aim for stable deterministic results, accept minor tile-choice differences vs Continuity.
- **Overlay layer routing**: confirm `resources.builderPack` is reachable from `BlockRenderer` (it is via `resources`); confirm CUTOUT_MIPPED buffer is being finalized in `BuildTask` (it iterates `TerrainRenderType.VALUES`).

## 9. Files

New (`src/main/java/net/vulkanmod/render/ctm/`):
- `CtmPackLoader.java`
- `CtmProperties.java`
- `CtmProperties$Method` (enum, nested or own file)
- `BiomeMatcher.java`
- `CtmAtlasRegistrar.java`
- `CtmResolver.java`
- `CtmContext.java` / `CtmResult.java`
- `CtmUvRemapper.java` (UV-remap `QuadView` decorator)

Modified:
- `BlockRenderer.java` — per-quad resolve + overlay emission (alongside the Polytone hook).
- atlas mixin (`MTextureAtlas` / new sprite source) — tile registration.
- reload hook (existing GameRenderer/reload path) — drive `CtmPackLoader`.
- `Config` / options (optional) — a master CTM on/off toggle, defaulting on, so vanilla-Vulkan can be fully restored (consistent with the "shaders must be external" principle).

## 10. Milestone acceptance

Phase 1 is done when, with Nature X active in the target profile, leaves show biome overlays, dirt shows its overlay layer, and grass varies per biome — visibly matching the Sodium+Continuity reference — with no crash and no meaningful FPS regression when CTM is inactive.
