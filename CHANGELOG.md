# Changelog

All notable changes to Volcanic. Versions are alpha pre-releases.

## 0.1.2-alpha

The **compatibility & reflections** release. Volcanic gains a full deferred shader system with
screen-space reflections, and — most importantly — starts rendering the resource packs and mods that
used to fall back to OpenGL or simply not work: **Sodium core shaders, Polytone, and OptiFine CTM/CIT**,
all natively in the Vulkan terrain and item paths.

### ✨ New — resource-pack & mod compatibility

- **OptiFine CTM (Connected Textures) — native.** Packs like Nature X now render their biome-varying
  block textures, leaf-flower overlays and ground "carpet" detail directly in Volcanic's terrain
  mesher — no Continuity, no OptiFine. Supports the `fixed`, `random`, `repeat`, `overlay_fixed` and
  `overlay_random` methods with `matchTiles` / `matchBlocks` matching and `biomes` / `faces` /
  `heights` / `weights` filters. OptiFine tiles are stitched into the block atlas via a custom sprite
  source. Toggleable; degrades safely to vanilla when off or on error. *(Connected `ctm` / `vertical`
  methods are Phase 2.)*
- **OptiFine CIT (Custom Item Textures/Models) — native.** Armor with a trim now shows the pack's
  custom item model (matched by item + trim pattern + material) — e.g. Visual Armor Trims works with no
  CIT mod. Custom models are registered for baking and swapped at item-render time.
- **Polytone compatibility.** Custom colormaps (fixes crash-spam and missing tints with packs like
  Nature X), biome-dependent **variant textures**, and block **visual offsets** now apply under
  Volcanic's terrain renderer. Unsupported custom render types are skipped gracefully instead of
  crashing the game.
- **Sodium Core Shader Support (SCSS) — native.** Fragment resource packs written for
  [SodiumCoreShaderSupport](https://github.com/lni-dev/SodiumCoreShaderSupport) render natively —
  identical-to-Sodium terrain and clouds — without Sodium installed, including `u_GameTime` /
  `u_SunAngle` uniforms.

### ✨ New — rendering

- **Screen-space reflections (SSR).** Natural, deterministic **water reflections** rendered in a
  dedicated half-resolution `water_reflection` pass.
- **Glass reflections** via a material-ID G-buffer (Iris-style, extensible toward LabPBR later).
- **Horizon fog** blending the sky into distant terrain.
- **Camille "Radiance" deferred pipeline** — the post-process/lighting stack (shadows, volumetric fog,
  god-rays, custom lightmap, point lights, bloom, TAA, render-scale) reorganised into a proper deferred
  shader system.

### 🧱 Internals

- New annotation-driven pipeline engine (`@GfxPipeline` / `@Ubo` / `@Sampler`, `PipelineFactory`) —
  shaders are defined in Java instead of loose JSON.
- The whole shader is expressed as a **Java frame graph** (`@Pass` annotations → phases + executor
  passes, pass order derived automatically) under `render/framegraph/`.
- Half-resolution evaluation with bilateral upsample for heavy effects; shadow-map perf pass.

### 🐛 Fixes

- Fixed a startup **stack overflow** and corrected **GLFW platform detection** (was crashing on
  Wayland).
- CTM/CIT hooks are fully fail-safe: any resolve/render error degrades to vanilla — chunk meshing and
  item rendering never crash.

---

## 0.1.1-alpha — 2026-07-18

- Native Vulkan **post-process pipeline** (color grading, volumetric height fog, god-rays).
- **Sun/moon shadow mapping** with Vogel-disk PCF, slope bias, day/night, quality slider.
- Custom **lightmap** + per-pixel **point lights** from emissive blocks; handheld dynamic light.
- **TAA** temporal accumulation and **render-scale** upscaling.
- **macOS/NeoForge startup crash fixed** (LWJGL Vulkan module-layer issue); `CAMetalLayer` surface path
  via `VK_EXT_metal_surface` (MoltenVK); one cross-platform jar.
- **GPU frame timing** via Vulkan timestamp queries.
