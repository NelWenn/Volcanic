# OptiFine CTM Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render OptiFine CTM (sprite-swap + overlays) natively in Volcanic's terrain mesher so packs like Nature X show biome-varying block textures, leaf-flower overlays, and dirt "carpet" overlays without Continuity.

**Architecture:** A reload-time loader parses `optifine/ctm/**/*.properties` into indexed `CtmProperties`; referenced tiles are stitched into the block atlas; a pure `CtmStore.resolve()` maps each terrain quad to a swap or overlay; `BlockRenderer.renderModelFace` applies swaps by UV-remapping and emits overlay quads into the cutout layer buffer. Pure helpers (weighted pick, position RNG, UV math, path/biome parsing) are Minecraft-free and JUnit-tested; everything touching MC types is validated in-game.

**Tech Stack:** Java 21, NeoForge 1.21.1, VulkanMod terrain pipeline (`net.vulkanmod.render.chunk.build`), JUnit 5 (pure helpers only).

## Global Constraints

- ZERO comments in any code written or modified. No exceptions.
- No AI/Claude mention in any commit, file, or doc.
- Commit author/committer MUST be `NelWenn <NelWenn@users.noreply.github.com>`. Never `contact@bryanmourier.com`.
- Do NOT work on `main`. Create/execute on branch `optifine-ctm` first (the current uncommitted Polytone work rides along — the CTM hook shares the `renderModelFace` site).
- Build: `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home ./gradlew build -x test`.
- Deploy for in-game tests: copy `build/libs/Volcanic-1.21.1-*.jar` (non-sources) over the same-named jar in `~/Library/Application Support/ModrinthApp/profiles/NeoForge 1.21.1/mods/`. Test ONLY in that profile.
- Run JUnit helper tests with: `JAVA_HOME=... ./gradlew test --tests "net.vulkanmod.render.ctm.*"`.
- New package: `net.vulkanmod.render.ctm`. Follow existing `render/` subpackage naming.
- Robustness rule (hard): any exception in resolve/hook degrades to rendering the original quad. Never break chunk meshing (precedent: the null-quad empty-world regression).
- Shader-external principle: CTM must be globally toggleable off, restoring pure vanilla-Vulkan rendering.

---

## File Structure

New (`src/main/java/net/vulkanmod/render/ctm/`):
- `CtmMethod.java` — method enum + `fromString`.
- `WeightedPicker.java` — pure weighted index pick.
- `PositionRng.java` — pure deterministic position seed.
- `UvRemap.java` — pure UV rectangle remap.
- `TilePath.java` — pure OptiFine tile-token → texture `ResourceLocation`.
- `BiomeMatcher.java` — biome-name set with negation.
- `CtmContext.java` — resolve input (record).
- `CtmResult.java` — resolve output (NONE / SWAP / OVERLAY).
- `CtmProperties.java` — one parsed rule + `matches(...)`.
- `CtmPropertiesParser.java` — `.properties` text → `CtmProperties`.
- `CtmStore.java` — indexes + `resolve(ctx)`.
- `CtmPackLoader.java` — reload listener; scans resources; builds store + atlas set.
- `CtmAtlasRegistrar.java` — collects tile sprite ids; stitched into block atlas.
- `CtmUvQuad.java` — `QuadView` decorator remapping UVs to a target sprite.
- `Ctm.java` — facade: `isActive()`, `resolve(...)`.

Test (`src/test/java/net/vulkanmod/render/ctm/`):
- `WeightedPickerTest.java`, `PositionRngTest.java`, `UvRemapTest.java`, `TilePathTest.java`, `BiomeMatcherTest.java`, `CtmMethodTest.java`.

Modified:
- `src/main/java/net/vulkanmod/render/chunk/build/BlockRenderer.java` — per-quad CTM at the Polytone hook site.
- `src/main/java/net/vulkanmod/mixin/texture/MTextureAtlas.java` — stitch CTM tiles.
- reload hook (`src/main/java/net/vulkanmod/mixin/render/GameRendererMixin.java`, existing reload inject) — drive `CtmPackLoader`.
- `src/main/java/net/vulkanmod/config/Config.java` (+ `option/Options.java`) — `ctmEnabled` toggle.

---

## Task 1: `CtmMethod` enum

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmMethod.java`
- Test: `src/test/java/net/vulkanmod/render/ctm/CtmMethodTest.java`

**Interfaces:**
- Produces: `enum CtmMethod { FIXED, RANDOM, REPEAT, OVERLAY_FIXED, OVERLAY_RANDOM, UNSUPPORTED }`; `static CtmMethod fromString(String)`; `boolean isOverlay()`.

- [ ] **Step 1: Write the failing test**

```java
package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CtmMethodTest {
    @Test
    void parsesKnownMethods() {
        assertEquals(CtmMethod.RANDOM, CtmMethod.fromString("random"));
        assertEquals(CtmMethod.OVERLAY_RANDOM, CtmMethod.fromString("overlay_random"));
        assertEquals(CtmMethod.FIXED, CtmMethod.fromString("fixed"));
    }

    @Test
    void connectedMethodsAreUnsupportedInPhase1() {
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString("ctm"));
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString("horizontal"));
    }

    @Test
    void overlayFlag() {
        assertTrue(CtmMethod.OVERLAY_FIXED.isOverlay());
        assertFalse(CtmMethod.RANDOM.isOverlay());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "net.vulkanmod.render.ctm.CtmMethodTest"`
Expected: FAIL (CtmMethod does not exist).

- [ ] **Step 3: Write minimal implementation**

```java
package net.vulkanmod.render.ctm;

public enum CtmMethod {
    FIXED,
    RANDOM,
    REPEAT,
    OVERLAY_FIXED,
    OVERLAY_RANDOM,
    UNSUPPORTED;

    public static CtmMethod fromString(String s) {
        if (s == null) return UNSUPPORTED;
        return switch (s.trim().toLowerCase()) {
            case "fixed" -> FIXED;
            case "random" -> RANDOM;
            case "repeat" -> REPEAT;
            case "overlay_fixed" -> OVERLAY_FIXED;
            case "overlay_random" -> OVERLAY_RANDOM;
            default -> UNSUPPORTED;
        };
    }

    public boolean isOverlay() {
        return this == OVERLAY_FIXED || this == OVERLAY_RANDOM;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/CtmMethod.java src/test/java/net/vulkanmod/render/ctm/CtmMethodTest.java
git commit -m "Add CtmMethod enum for OptiFine CTM phase 1"
```

---

## Task 2: Pure helpers — `WeightedPicker`, `PositionRng`, `UvRemap`

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/WeightedPicker.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/PositionRng.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/UvRemap.java`
- Test: `src/test/java/net/vulkanmod/render/ctm/WeightedPickerTest.java`, `PositionRngTest.java`, `UvRemapTest.java`

**Interfaces:**
- Produces: `WeightedPicker.pick(int[] weights, long seed) -> int` (index into weights; sum>0 assumed, empty→0).
- Produces: `PositionRng.seed(int x, int y, int z, int face) -> long` (deterministic).
- Produces: `UvRemap.remap(float u, float origU0, float origU1, float newU0, float newU1) -> float`.

- [ ] **Step 1: Write the failing tests**

```java
package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeightedPickerTest {
    @Test
    void allWeightOnLastIndex() {
        int[] w = {0, 0, 5};
        for (long s = 0; s < 50; s++) assertEquals(2, WeightedPicker.pick(w, s));
    }
    @Test
    void deterministicForSameSeed() {
        int[] w = {1, 1, 45};
        assertEquals(WeightedPicker.pick(w, 12345L), WeightedPicker.pick(w, 12345L));
    }
    @Test
    void indexInRange() {
        int[] w = {3, 2, 1};
        for (long s = 0; s < 100; s++) {
            int i = WeightedPicker.pick(w, s);
            assertTrue(i >= 0 && i < 3);
        }
    }
}
```

```java
package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionRngTest {
    @Test
    void sameInputSameSeed() {
        assertEquals(PositionRng.seed(10, 64, -30, 2), PositionRng.seed(10, 64, -30, 2));
    }
    @Test
    void differentPositionsDiffer() {
        assertNotEquals(PositionRng.seed(10, 64, -30, 2), PositionRng.seed(11, 64, -30, 2));
    }
}
```

```java
package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UvRemapTest {
    @Test
    void midpointMapsToMidpoint() {
        assertEquals(0.75f, UvRemap.remap(0.25f, 0.0f, 0.5f, 0.5f, 1.0f), 1e-6f);
    }
    @Test
    void edgesMapToEdges() {
        assertEquals(0.5f, UvRemap.remap(0.0f, 0.0f, 0.5f, 0.5f, 1.0f), 1e-6f);
        assertEquals(1.0f, UvRemap.remap(0.5f, 0.0f, 0.5f, 0.5f, 1.0f), 1e-6f);
    }
    @Test
    void degenerateOrigReturnsNewStart() {
        assertEquals(0.5f, UvRemap.remap(0.3f, 0.5f, 0.5f, 0.5f, 1.0f), 1e-6f);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=... ./gradlew test --tests "net.vulkanmod.render.ctm.WeightedPickerTest" --tests "net.vulkanmod.render.ctm.PositionRngTest" --tests "net.vulkanmod.render.ctm.UvRemapTest"`
Expected: FAIL (classes missing).

- [ ] **Step 3: Write minimal implementations**

```java
package net.vulkanmod.render.ctm;

public final class WeightedPicker {
    private WeightedPicker() {}

    public static int pick(int[] weights, long seed) {
        if (weights == null || weights.length == 0) return 0;
        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (total <= 0) return 0;
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 32);
        int r = (int) Math.floorMod(h, total);
        for (int i = 0; i < weights.length; i++) {
            r -= Math.max(0, weights[i]);
            if (r < 0) return i;
        }
        return weights.length - 1;
    }
}
```

```java
package net.vulkanmod.render.ctm;

public final class PositionRng {
    private PositionRng() {}

    public static long seed(int x, int y, int z, int face) {
        long h = 0x100000001B3L;
        h = (h ^ (x * 3129871L)) * 116129781L;
        h = (h ^ (y * 116129781L)) * 116129781L;
        h = (h ^ (z * 3129871L)) * 116129781L;
        h = (h ^ (face * 0x9E3779B1L)) * 116129781L;
        return h ^ (h >>> 29);
    }
}
```

```java
package net.vulkanmod.render.ctm;

public final class UvRemap {
    private UvRemap() {}

    public static float remap(float u, float origU0, float origU1, float newU0, float newU1) {
        float span = origU1 - origU0;
        float local = Math.abs(span) < 1e-7f ? 0.0f : (u - origU0) / span;
        return newU0 + local * (newU1 - newU0);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: same as Step 2. Expected: PASS (all).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/WeightedPicker.java src/main/java/net/vulkanmod/render/ctm/PositionRng.java src/main/java/net/vulkanmod/render/ctm/UvRemap.java src/test/java/net/vulkanmod/render/ctm/WeightedPickerTest.java src/test/java/net/vulkanmod/render/ctm/PositionRngTest.java src/test/java/net/vulkanmod/render/ctm/UvRemapTest.java
git commit -m "Add pure CTM helpers: weighted pick, position RNG, UV remap"
```

---

## Task 3: `TilePath` + `BiomeMatcher`

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/TilePath.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/BiomeMatcher.java`
- Test: `src/test/java/net/vulkanmod/render/ctm/TilePathTest.java`, `BiomeMatcherTest.java`

**Interfaces:**
- Produces: `TilePath.resolve(String token, String propertiesDirPath, String namespace) -> String` returning a texture resource path WITHOUT extension (e.g. `optifine/ctm/x/0`), namespace-qualified as `ns:path`. Numeric token → sibling file in `propertiesDirPath`; `~/x` → `optifine/x`; `path` → relative to `propertiesDirPath`; `ns:path` → absolute.
- Produces: `BiomeMatcher.parse(String spaceSeparated) -> BiomeMatcher`; `boolean matches(String biomeId)`; supports `!name` negation; a null/blank spec yields a matcher whose `matches` is always true (via `BiomeMatcher.any()`).

- [ ] **Step 1: Write the failing tests**

```java
package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TilePathTest {
    @Test
    void numericTokenResolvesSibling() {
        assertEquals("minecraft:optifine/ctm/leaves/0",
            TilePath.resolve("0", "optifine/ctm/leaves", "minecraft"));
    }
    @Test
    void tildeResolvesToOptifineRoot() {
        assertEquals("minecraft:optifine/x/y",
            TilePath.resolve("~/x/y", "optifine/ctm/leaves", "minecraft"));
    }
    @Test
    void namespacedTokenIsAbsolute() {
        assertEquals("nx:blocks/thing",
            TilePath.resolve("nx:blocks/thing", "optifine/ctm/leaves", "minecraft"));
    }
}
```

```java
package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BiomeMatcherTest {
    @Test
    void anyMatchesEverything() {
        assertTrue(BiomeMatcher.any().matches("minecraft:plains"));
    }
    @Test
    void positiveList() {
        BiomeMatcher m = BiomeMatcher.parse("taiga snowy_taiga");
        assertTrue(m.matches("minecraft:taiga"));
        assertFalse(m.matches("minecraft:plains"));
    }
    @Test
    void negation() {
        BiomeMatcher m = BiomeMatcher.parse("!plains");
        assertFalse(m.matches("minecraft:plains"));
        assertTrue(m.matches("minecraft:taiga"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=... ./gradlew test --tests "net.vulkanmod.render.ctm.TilePathTest" --tests "net.vulkanmod.render.ctm.BiomeMatcherTest"`
Expected: FAIL.

- [ ] **Step 3: Write minimal implementations**

```java
package net.vulkanmod.render.ctm;

public final class TilePath {
    private TilePath() {}

    public static String resolve(String token, String propertiesDirPath, String namespace) {
        String t = token.trim();
        if (t.endsWith(".png")) t = t.substring(0, t.length() - 4);
        int colon = t.indexOf(':');
        if (colon > 0) return t;
        if (t.startsWith("~/")) return namespace + ":optifine/" + t.substring(2);
        if (t.matches("\\d+")) return namespace + ":" + propertiesDirPath + "/" + t;
        if (t.startsWith("./")) t = t.substring(2);
        return namespace + ":" + propertiesDirPath + "/" + t;
    }
}
```

```java
package net.vulkanmod.render.ctm;

import java.util.HashSet;
import java.util.Set;

public final class BiomeMatcher {
    private final Set<String> names;
    private final boolean negated;
    private final boolean any;

    private BiomeMatcher(Set<String> names, boolean negated, boolean any) {
        this.names = names;
        this.negated = negated;
        this.any = any;
    }

    public static BiomeMatcher any() {
        return new BiomeMatcher(Set.of(), false, true);
    }

    public static BiomeMatcher parse(String spec) {
        if (spec == null || spec.isBlank()) return any();
        Set<String> set = new HashSet<>();
        boolean neg = false;
        for (String raw : spec.trim().split("\\s+")) {
            String s = raw;
            if (s.startsWith("!")) { neg = true; s = s.substring(1); }
            if (s.isEmpty()) continue;
            set.add(s.contains(":") ? s : "minecraft:" + s);
        }
        if (set.isEmpty()) return any();
        return new BiomeMatcher(set, neg, false);
    }

    public boolean matches(String biomeId) {
        if (any) return true;
        boolean present = names.contains(biomeId);
        return negated != present;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: same as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/TilePath.java src/main/java/net/vulkanmod/render/ctm/BiomeMatcher.java src/test/java/net/vulkanmod/render/ctm/TilePathTest.java src/test/java/net/vulkanmod/render/ctm/BiomeMatcherTest.java
git commit -m "Add CTM tile-path resolver and biome matcher"
```

---

## Task 4: `CtmContext`, `CtmResult`, `CtmProperties`, `CtmPropertiesParser`

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmContext.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmResult.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmProperties.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmPropertiesParser.java`

**Interfaces:**
- Produces: `record CtmContext(TextureAtlasSprite sprite, BlockState state, BlockPos pos, Direction face, ResourceLocation biome)`.
- Produces: `CtmResult` with static factories `none()`, `swap(TextureAtlasSprite)`, `overlay(TextureAtlasSprite, TerrainRenderType layer, int tintIndex)`; `Kind kind()` enum `{NONE, SWAP, OVERLAY}`; `TextureAtlasSprite sprite()`; `TerrainRenderType layer()`; `int tintIndex()`.
- Produces: `CtmProperties` fields `CtmMethod method`, `Set<ResourceLocation> matchTiles`, `Set<Block> matchBlocks`, `List<ResourceLocation> tileIds`, `int[] weights`, `EnumSet<Direction> faces`, `BiomeMatcher biomes`, `int minHeight`, `int maxHeight`, `int tintIndex`, `TerrainRenderType overlayLayer`; method `boolean matches(ResourceLocation baseSpriteId, Block block, Direction face, int y, String biomeId)`; `String basePath` (properties dir) retained for tile resolution.
- Produces: `CtmPropertiesParser.parse(Properties props, ResourceLocation file) -> CtmProperties` (null if method UNSUPPORTED or no match key). Consumes: `CtmMethod`, `TilePath`, `BiomeMatcher`.

**No JUnit here** (types are Minecraft-bound: `TextureAtlasSprite`, `Block`, `ResourceLocation`). Validated by compilation + later in-game load count.

- [ ] **Step 1: Create `CtmContext.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public record CtmContext(TextureAtlasSprite sprite, BlockState state, BlockPos pos,
                         Direction face, ResourceLocation biome) {}
```

- [ ] **Step 2: Create `CtmResult.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.vulkanmod.render.vertex.TerrainRenderType;

public final class CtmResult {
    public enum Kind { NONE, SWAP, OVERLAY }

    private static final CtmResult NONE = new CtmResult(Kind.NONE, null, null, -1);

    private final Kind kind;
    private final TextureAtlasSprite sprite;
    private final TerrainRenderType layer;
    private final int tintIndex;

    private CtmResult(Kind kind, TextureAtlasSprite sprite, TerrainRenderType layer, int tintIndex) {
        this.kind = kind;
        this.sprite = sprite;
        this.layer = layer;
        this.tintIndex = tintIndex;
    }

    public static CtmResult none() { return NONE; }
    public static CtmResult swap(TextureAtlasSprite sprite) { return new CtmResult(Kind.SWAP, sprite, null, -1); }
    public static CtmResult overlay(TextureAtlasSprite sprite, TerrainRenderType layer, int tintIndex) {
        return new CtmResult(Kind.OVERLAY, sprite, layer, tintIndex);
    }

    public Kind kind() { return kind; }
    public TextureAtlasSprite sprite() { return sprite; }
    public TerrainRenderType layer() { return layer; }
    public int tintIndex() { return tintIndex; }
}
```

- [ ] **Step 3: Create `CtmProperties.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class CtmProperties {
    public final CtmMethod method;
    public final Set<ResourceLocation> matchTiles;
    public final Set<Block> matchBlocks;
    public final List<ResourceLocation> tileIds;
    public final int[] weights;
    public final EnumSet<Direction> faces;
    public final BiomeMatcher biomes;
    public final int minHeight;
    public final int maxHeight;
    public final int tintIndex;
    public final TerrainRenderType overlayLayer;

    public CtmProperties(CtmMethod method, Set<ResourceLocation> matchTiles, Set<Block> matchBlocks,
                         List<ResourceLocation> tileIds, int[] weights, EnumSet<Direction> faces,
                         BiomeMatcher biomes, int minHeight, int maxHeight, int tintIndex,
                         TerrainRenderType overlayLayer) {
        this.method = method;
        this.matchTiles = matchTiles;
        this.matchBlocks = matchBlocks;
        this.tileIds = tileIds;
        this.weights = weights;
        this.faces = faces;
        this.biomes = biomes;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.tintIndex = tintIndex;
        this.overlayLayer = overlayLayer;
    }

    public boolean matches(ResourceLocation baseSpriteId, Block block, Direction face, int y, String biomeId) {
        if (face != null && !faces.contains(face)) return false;
        if (y < minHeight || y > maxHeight) return false;
        if (!biomes.matches(biomeId)) return false;
        if (!matchBlocks.isEmpty() && matchBlocks.contains(block)) return true;
        if (!matchTiles.isEmpty() && matchTiles.contains(baseSpriteId)) return true;
        return matchBlocks.isEmpty() && matchTiles.isEmpty();
    }
}
```

- [ ] **Step 4: Create `CtmPropertiesParser.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class CtmPropertiesParser {
    private CtmPropertiesParser() {}

    public static CtmProperties parse(Properties p, ResourceLocation file) {
        CtmMethod method = CtmMethod.fromString(p.getProperty("method", "ctm"));
        if (method == CtmMethod.UNSUPPORTED) return null;

        String ns = file.getNamespace();
        String path = file.getPath();
        int slash = path.lastIndexOf('/');
        String dir = slash >= 0 ? path.substring(0, slash) : path;

        List<ResourceLocation> tileIds = new ArrayList<>();
        for (String tok : expandTiles(p.getProperty("tiles", ""))) {
            tileIds.add(ResourceLocation.parse(TilePath.resolve(tok, dir, ns)));
        }
        if (tileIds.isEmpty()) return null;

        Set<ResourceLocation> matchTiles = new HashSet<>();
        for (String t : split(p.getProperty("matchTiles"))) {
            matchTiles.add(ResourceLocation.parse(TilePath.resolve(t, dir, ns)));
        }
        Set<Block> matchBlocks = new HashSet<>();
        for (String b : split(p.getProperty("matchBlocks"))) {
            ResourceLocation id = ResourceLocation.parse(b.contains(":") ? b : "minecraft:" + b);
            Block blk = BuiltInRegistries.BLOCK.get(id);
            if (blk != null) matchBlocks.add(blk);
        }

        int[] weights = parseWeights(p.getProperty("weights"), tileIds.size());
        EnumSet<Direction> faces = parseFaces(p.getProperty("faces"));
        BiomeMatcher biomes = BiomeMatcher.parse(p.getProperty("biomes"));
        int minH = parseInt(p.getProperty("minHeight"), Integer.MIN_VALUE);
        int maxH = parseInt(p.getProperty("maxHeight"), Integer.MAX_VALUE);
        int tint = parseInt(p.getProperty("tintIndex"), -1);
        TerrainRenderType layer = TerrainRenderType.CUTOUT_MIPPED;

        return new CtmProperties(method, matchTiles, matchBlocks, tileIds, weights, faces,
                biomes, minH, maxH, tint, layer);
    }

    private static List<String> expandTiles(String spec) {
        List<String> out = new ArrayList<>();
        for (String tok : split(spec)) {
            int dash = tok.indexOf('-');
            if (dash > 0 && tok.substring(0, dash).matches("\\d+") && tok.substring(dash + 1).matches("\\d+")) {
                int a = Integer.parseInt(tok.substring(0, dash));
                int b = Integer.parseInt(tok.substring(dash + 1));
                for (int i = a; i <= b; i++) out.add(Integer.toString(i));
            } else {
                out.add(tok);
            }
        }
        return out;
    }

    private static String[] split(String s) {
        if (s == null || s.isBlank()) return new String[0];
        return s.trim().split("\\s+");
    }

    private static int[] parseWeights(String s, int n) {
        String[] parts = split(s == null ? null : s.replace(":", " "));
        int[] w = new int[n];
        Arrays.fill(w, 1);
        for (int i = 0; i < Math.min(parts.length, n); i++) {
            w[i] = parseInt(parts[i], 1);
        }
        return w;
    }

    private static EnumSet<Direction> parseFaces(String s) {
        if (s == null || s.isBlank()) return EnumSet.allOf(Direction.class);
        EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        for (String f : split(s)) {
            switch (f.toLowerCase()) {
                case "all" -> set.addAll(EnumSet.allOf(Direction.class));
                case "sides" -> { set.add(Direction.NORTH); set.add(Direction.SOUTH); set.add(Direction.EAST); set.add(Direction.WEST); }
                case "top" -> set.add(Direction.UP);
                case "bottom" -> set.add(Direction.DOWN);
                case "north" -> set.add(Direction.NORTH);
                case "south" -> set.add(Direction.SOUTH);
                case "east" -> set.add(Direction.EAST);
                case "west" -> set.add(Direction.WEST);
                default -> {}
            }
        }
        return set.isEmpty() ? EnumSet.allOf(Direction.class) : set;
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `JAVA_HOME=... ./gradlew compileJava`
Expected: BUILD SUCCESSFUL. (Fix any `TerrainRenderType.CUTOUT_MIPPED` name mismatch by checking `net.vulkanmod.render.vertex.TerrainRenderType` constants; use the actual mipped-cutout constant name.)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/CtmContext.java src/main/java/net/vulkanmod/render/ctm/CtmResult.java src/main/java/net/vulkanmod/render/ctm/CtmProperties.java src/main/java/net/vulkanmod/render/ctm/CtmPropertiesParser.java
git commit -m "Add CTM properties model and parser"
```

---

## Task 5: `CtmStore` (indexing + resolve) + `Ctm` facade

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmStore.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/Ctm.java`

**Interfaces:**
- Consumes: `CtmProperties`, `CtmContext`, `CtmResult`, `WeightedPicker`, `PositionRng`, `CtmMethod`. A sprite lookup `java.util.function.Function<ResourceLocation, TextureAtlasSprite>` (provided by the atlas at build time, Task 6).
- Produces: `CtmStore` built from `List<CtmProperties>` + sprite lookup; `CtmResult resolve(CtmContext ctx)`.
- Produces: `Ctm` singleton facade: `static boolean isActive()`, `static CtmResult resolve(TextureAtlasSprite sprite, BlockState state, BlockPos pos, Direction face, BlockAndTintGetter region)`, `static void install(CtmStore store)`, `static void clear()`.

**No JUnit** (Minecraft-bound). Validated by later in-game behavior.

- [ ] **Step 1: Create `CtmStore.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CtmStore {
    private final Map<Block, List<CtmProperties>> byBlock = new HashMap<>();
    private final Map<ResourceLocation, List<CtmProperties>> byTile = new HashMap<>();
    private final Function<ResourceLocation, TextureAtlasSprite> spriteLookup;

    public CtmStore(List<CtmProperties> all, Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        this.spriteLookup = spriteLookup;
        for (CtmProperties p : all) {
            for (Block b : p.matchBlocks) byBlock.computeIfAbsent(b, k -> new ArrayList<>()).add(p);
            for (ResourceLocation t : p.matchTiles) byTile.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
        }
    }

    public boolean isEmpty() {
        return byBlock.isEmpty() && byTile.isEmpty();
    }

    public CtmResult resolve(CtmContext ctx) {
        ResourceLocation baseId = ctx.sprite().contents().name();
        Block block = ctx.state().getBlock();
        String biomeId = ctx.biome() == null ? "" : ctx.biome().toString();
        int y = ctx.pos().getY();

        CtmProperties match = firstMatch(byBlock.get(block), baseId, block, ctx.face(), y, biomeId);
        if (match == null) match = firstMatch(byTile.get(baseId), baseId, block, ctx.face(), y, biomeId);
        if (match == null) return CtmResult.none();

        int face = ctx.face() == null ? 6 : ctx.face().get3DDataValue();
        int idx = switch (match.method) {
            case FIXED, OVERLAY_FIXED -> 0;
            case RANDOM, OVERLAY_RANDOM -> WeightedPicker.pick(match.weights,
                    PositionRng.seed(ctx.pos().getX(), ctx.pos().getY(), ctx.pos().getZ(), face));
            case REPEAT -> Math.floorMod(ctx.pos().getX() * 31 + ctx.pos().getZ(), match.tileIds.size());
            default -> 0;
        };
        TextureAtlasSprite sprite = spriteLookup.apply(match.tileIds.get(idx));
        if (sprite == null) return CtmResult.none();

        if (match.method.isOverlay()) {
            return CtmResult.overlay(sprite, match.overlayLayer, match.tintIndex);
        }
        return CtmResult.swap(sprite);
    }

    private CtmProperties firstMatch(List<CtmProperties> list, ResourceLocation baseId, Block block,
                                     Direction face, int y, String biomeId) {
        if (list == null) return null;
        for (CtmProperties p : list) {
            if (p.matches(baseId, block, face, y, biomeId)) return p;
        }
        return null;
    }
}
```

- [ ] **Step 2: Create `Ctm.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.WorldRenderer;

public final class Ctm {
    private static volatile CtmStore store;

    private Ctm() {}

    public static void install(CtmStore s) {
        store = (s == null || s.isEmpty()) ? null : s;
    }

    public static void clear() {
        store = null;
    }

    public static boolean isActive() {
        return store != null && net.vulkanmod.Initializer.CONFIG.ctmEnabled;
    }

    public static CtmResult resolve(TextureAtlasSprite sprite, BlockState state, BlockPos pos,
                                    Direction face, BlockAndTintGetter region) {
        CtmStore s = store;
        if (s == null) return CtmResult.none();
        try {
            ResourceLocation biome = null;
            var level = WorldRenderer.getLevel();
            if (level != null) biome = level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
            return s.resolve(new CtmContext(sprite, state, pos, face, biome));
        } catch (Throwable t) {
            return CtmResult.none();
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `JAVA_HOME=... ./gradlew compileJava`
Expected: BUILD SUCCESSFUL. (If `ctmEnabled` does not yet exist on `Config`, add `public boolean ctmEnabled = true;` to `Config.java` now — it is formally owned by Task 9 but needed to compile; keep the field addition in this commit and skip re-adding in Task 9.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/CtmStore.java src/main/java/net/vulkanmod/render/ctm/Ctm.java src/main/java/net/vulkanmod/config/Config.java
git commit -m "Add CTM store, resolver, and Ctm facade"
```

---

## Task 6: `CtmAtlasRegistrar` + block-atlas stitch hook

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmAtlasRegistrar.java`
- Modify: `src/main/java/net/vulkanmod/mixin/texture/MTextureAtlas.java`

**Interfaces:**
- Consumes: the set of tile `ResourceLocation`s collected by `CtmPackLoader` (Task 7) via `CtmAtlasRegistrar.setPending(Set<ResourceLocation> textureIds)`.
- Produces: `CtmAtlasRegistrar.additionalSprites() -> Set<ResourceLocation>` (texture ids to stitch into the blocks atlas), and after stitch `CtmAtlasRegistrar.spriteLookup(TextureAtlas atlas) -> Function<ResourceLocation, TextureAtlasSprite>` mapping tile id → stitched sprite.

**Validation:** in-game — atlas stitch log shows the CTM tiles registered; no "Missing sprite" spam for the CTM tiles.

- [ ] **Step 1: Create `CtmAtlasRegistrar.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Function;

public final class CtmAtlasRegistrar {
    private static volatile Set<ResourceLocation> pending = Set.of();

    private CtmAtlasRegistrar() {}

    public static void setPending(Set<ResourceLocation> textureIds) {
        pending = textureIds == null ? Set.of() : Set.copyOf(textureIds);
    }

    public static Set<ResourceLocation> additionalSprites() {
        return pending;
    }

    public static Function<ResourceLocation, TextureAtlasSprite> spriteLookup(TextureAtlas atlas) {
        return id -> {
            TextureAtlasSprite s = atlas.getSprite(id);
            if (s == null) return null;
            ResourceLocation missing = net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
            return s.contents().name().equals(missing) ? null : s;
        };
    }
}
```

- [ ] **Step 2: Inspect the atlas mixin to find the stitch/sprite-source injection point**

Run: `sed -n '1,80p' src/main/java/net/vulkanmod/mixin/texture/MTextureAtlas.java`
Identify where sprite `ResourceLocation`s are gathered before stitching (the `SpriteLoader`/`getBasicSpriteInfos`/`runSpriteSources` path, or the block-atlas prep). Note the exact method + local holding the sprite id collection.

- [ ] **Step 3: Inject CTM tiles into the blocks atlas gather**

In `MTextureAtlas.java`, in the sprite-gathering method, when the atlas location is the blocks atlas (`TextureAtlas.LOCATION_BLOCKS`), add every id from `CtmAtlasRegistrar.additionalSprites()` to the collection of resource locations to load as single-file sprites. Use the same collection/type the surrounding code uses (mirror how the existing code adds a sprite id). Guard with `if (CtmAtlasRegistrar.additionalSprites().isEmpty()) return;` fast-path. Wrap in try/catch that logs once and continues.

Exact edit shape (adapt to the real local names discovered in Step 2):

```java
if (((TextureAtlas)(Object)this).location().equals(TextureAtlas.LOCATION_BLOCKS)) {
    for (net.minecraft.resources.ResourceLocation id : net.vulkanmod.render.ctm.CtmAtlasRegistrar.additionalSprites()) {
        spriteSourceList.add(new net.minecraft.client.renderer.texture.atlas.sources.SingleFile(id, java.util.Optional.empty()));
    }
}
```

- [ ] **Step 4: Build + deploy**

Run:
```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home ./gradlew build -x test
cp build/libs/Volcanic-1.21.1-*.jar "$HOME/Library/Application Support/ModrinthApp/profiles/NeoForge 1.21.1/mods/"Volcanic-1.21.1-0.1.1-alpha.jar
```
(Loader in Task 7 is not wired yet, so `additionalSprites()` is empty — this step only verifies the atlas hook compiles and does not regress atlas loading.)

- [ ] **Step 5: In-game check**

User launches the profile, loads a world. Expected: terrain renders exactly as before (no regression), no new atlas errors in `logs/latest.log`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/CtmAtlasRegistrar.java src/main/java/net/vulkanmod/mixin/texture/MTextureAtlas.java
git commit -m "Add CTM atlas registrar and blocks-atlas stitch hook"
```

---

## Task 7: `CtmPackLoader` + reload hook

**Files:**
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmPackLoader.java`
- Modify: `src/main/java/net/vulkanmod/mixin/render/GameRendererMixin.java` (existing resource-reload inject used for Camille/Polytone)

**Interfaces:**
- Consumes: `ResourceManager`, `CtmPropertiesParser`, `CtmAtlasRegistrar`, `TextureAtlas` (blocks), `CtmStore`, `Ctm`.
- Produces: `CtmPackLoader.reload(ResourceManager rm)` — scans, parses, collects tile ids → `CtmAtlasRegistrar.setPending(...)`; and `CtmPackLoader.buildStore(TextureAtlas blocksAtlas)` — builds the `CtmStore` with a sprite lookup and calls `Ctm.install(store)`. Order: `reload` must run before atlas stitch; `buildStore` after stitch.

**Validation:** in-game log line reporting the number of loaded `CtmProperties`.

- [ ] **Step 1: Create `CtmPackLoader.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.Initializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class CtmPackLoader {
    private static List<CtmProperties> loaded = List.of();

    private CtmPackLoader() {}

    public static void reload(ResourceManager rm) {
        List<CtmProperties> all = new ArrayList<>();
        Set<ResourceLocation> tiles = new HashSet<>();
        try {
            Map<ResourceLocation, Resource> found = rm.listResources("optifine/ctm",
                    p -> p.getPath().endsWith(".properties"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                try (InputStream in = e.getValue().open()) {
                    Properties props = new Properties();
                    props.load(in);
                    CtmProperties parsed = CtmPropertiesParser.parse(props, e.getKey());
                    if (parsed != null) {
                        all.add(parsed);
                        tiles.addAll(parsed.tileIds);
                    }
                } catch (Throwable t) {
                    Initializer.LOGGER.warn("CTM: failed to parse {}", e.getKey());
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("CTM: resource scan failed", t);
        }
        loaded = all;
        CtmAtlasRegistrar.setPending(tiles);
        Initializer.LOGGER.info("CTM: loaded {} properties, {} tiles", all.size(), tiles.size());
    }

    public static void buildStore(TextureAtlas blocksAtlas) {
        if (loaded.isEmpty()) { Ctm.clear(); return; }
        CtmStore store = new CtmStore(loaded, CtmAtlasRegistrar.spriteLookup(blocksAtlas));
        Ctm.install(store);
    }
}
```

- [ ] **Step 2: Inspect the existing reload inject**

Run: `grep -n "reload\|Reload\|onReload\|ResourceManager\|registerReloadListener\|apply" src/main/java/net/vulkanmod/mixin/render/GameRendererMixin.java`
Find where Camille/Polytone reload is driven and where the blocks atlas becomes available post-stitch. `CtmPackLoader.reload(rm)` must be called during prepare (before atlas stitch); `CtmPackLoader.buildStore(atlas)` after the blocks atlas is stitched.

- [ ] **Step 3: Wire `reload` before stitch**

Add `net.vulkanmod.render.ctm.CtmPackLoader.reload(resourceManager);` at the existing reload-prepare inject (same site that triggers Polytone/Camille reload). If the atlas gather (Task 6) runs during this same reload after this call, `additionalSprites()` will be populated in time.

- [ ] **Step 4: Wire `buildStore` after stitch**

After the blocks atlas is available (obtain via `net.minecraft.client.Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)`), call `net.vulkanmod.render.ctm.CtmPackLoader.buildStore(atlas);`. Place this at the tail of the reload inject (after models/atlas are reloaded).

- [ ] **Step 5: Build + deploy + in-game check**

Run build + deploy (as Task 6 Step 4). User launches with Nature X active.
Expected in `logs/latest.log`: `CTM: loaded <N> properties, <M> tiles` with N in the hundreds (Nature X has ~1405 files; Phase-1-supported methods are a subset, so N < 1405 is expected). No crash.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/CtmPackLoader.java src/main/java/net/vulkanmod/mixin/render/GameRendererMixin.java
git commit -m "Load CTM properties on resource reload and build store post-stitch"
```

---

## Task 8: `BlockRenderer` hook — sprite swap (fixed/random/repeat)

**Files:**
- Modify: `src/main/java/net/vulkanmod/render/chunk/build/BlockRenderer.java`
- Create: `src/main/java/net/vulkanmod/render/ctm/CtmUvQuad.java`

**Interfaces:**
- Consumes: `Ctm.resolve(...)`, `CtmResult`, `UvRemap`, `QuadView`.
- Produces: `CtmUvQuad` implementing `QuadView`, wrapping a base `QuadView` + base `TextureAtlasSprite` + target `TextureAtlasSprite`, overriding `getU`/`getV` via `UvRemap`; all other methods delegate.

**Validation:** in-game — grass_block and matched blocks show swapped/biome-varying textures.

- [ ] **Step 1: Create `CtmUvQuad.java`**

```java
package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.vulkanmod.render.model.quad.QuadView;

public final class CtmUvQuad implements QuadView {
    private final QuadView base;
    private final float o0u, o1u, o0v, o1v;
    private final float n0u, n1u, n0v, n1v;

    public CtmUvQuad(QuadView base, TextureAtlasSprite from, TextureAtlasSprite to) {
        this.base = base;
        this.o0u = from.getU0(); this.o1u = from.getU1();
        this.o0v = from.getV0(); this.o1v = from.getV1();
        this.n0u = to.getU0(); this.n1u = to.getU1();
        this.n0v = to.getV0(); this.n1v = to.getV1();
    }

    @Override public int getFlags() { return base.getFlags(); }
    @Override public float getX(int idx) { return base.getX(idx); }
    @Override public float getY(int idx) { return base.getY(idx); }
    @Override public float getZ(int idx) { return base.getZ(idx); }
    @Override public int getColor(int idx) { return base.getColor(idx); }
    @Override public int getColorIndex() { return base.getColorIndex(); }
    @Override public Direction getFacingDirection() { return base.getFacingDirection(); }
    @Override public float getU(int idx) { return UvRemap.remap(base.getU(idx), o0u, o1u, n0u, n1u); }
    @Override public float getV(int idx) { return UvRemap.remap(base.getV(idx), o0v, o1v, n0v, n1v); }
}
```

- [ ] **Step 2: Add the swap branch in `renderModelFace`**

In `BlockRenderer.renderModelFace`, immediately after the existing Polytone line (`bakedQuad = ...PolytoneCompat.maybeModifyQuad(...)`) and before `QuadView quadView = (QuadView) bakedQuad;`, replace the `quadView` acquisition with a CTM-aware version:

```java
QuadView quadView = (QuadView) bakedQuad;
if (net.vulkanmod.render.ctm.Ctm.isActive()) {
    net.vulkanmod.render.ctm.CtmResult ctm = net.vulkanmod.render.ctm.Ctm.resolve(
            bakedQuad.getSprite(), blockState, blockPos, cullFace, resources.region);
    if (ctm.kind() == net.vulkanmod.render.ctm.CtmResult.Kind.SWAP) {
        quadView = new net.vulkanmod.render.ctm.CtmUvQuad(quadView, bakedQuad.getSprite(), ctm.sprite());
    }
}
```

Keep the subsequent `lightPipeline.calculate(quadView, ...)` and `putQuadData(...)` calls exactly as they are, using this `quadView`.

Note: pass `cullFace` (the `Direction` parameter of `renderModelFace`) as the face; when null, the resolver treats it as the omni face.

- [ ] **Step 3: Build + deploy**

Run build + deploy (as Task 6 Step 4).

- [ ] **Step 4: In-game check**

User flies across biomes with Nature X active. Expected: `grass_block` (and other matched blocks with `method=fixed/random` + `biomes=`) now show per-biome / random texture variation instead of uniform grass. Compare against the Sodium+Continuity profile at the same coordinates. No crash, no flicker (swaps are position-deterministic).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/vulkanmod/render/ctm/CtmUvQuad.java src/main/java/net/vulkanmod/render/chunk/build/BlockRenderer.java
git commit -m "Apply CTM sprite swaps in the terrain block renderer"
```

---

## Task 9: Overlay emission (overlay_fixed / overlay_random)

**Files:**
- Modify: `src/main/java/net/vulkanmod/render/chunk/build/BlockRenderer.java`

**Interfaces:**
- Consumes: `Ctm.resolve`, `CtmResult` (OVERLAY kind), `CtmUvQuad`, `TerrainRenderType`, `resources.builderPack`.
- Produces: overlay quads written to the overlay layer buffer, coplanar with the base face, tinted when `tintIndex >= 0`.

**Validation:** in-game — leaf flowers + dirt carpet overlays appear (the headline Nature X features).

- [ ] **Step 1: Confirm builderPack access from BlockRenderer**

Run: `grep -n "builderPack\|ThreadBuilderPack\|builder(" src/main/java/net/vulkanmod/render/chunk/build/thread/BuilderResources.java`
Confirm `resources.builderPack.builder(TerrainRenderType)` returns the `TerrainBufferBuilder` for a layer. Note the exact accessor names.

- [ ] **Step 2: Add the overlay branch in `renderModelFace`**

Extend the CTM block from Task 8. After computing `ctm`, handle OVERLAY (base quad still renders normally through the existing path; the overlay is an ADDITIONAL emission):

```java
if (ctm.kind() == net.vulkanmod.render.ctm.CtmResult.Kind.OVERLAY) {
    net.vulkanmod.render.vertex.TerrainBufferBuilder overlayBuffer = resources.builderPack.builder(ctm.layer());
    net.vulkanmod.render.ctm.CtmUvQuad overlayQuad = new net.vulkanmod.render.ctm.CtmUvQuad(quadView, bakedQuad.getSprite(), ctm.sprite());
    net.vulkanmod.render.ctm.CtmOverlayEmitter.emit(overlayBuffer, overlayQuad, quadLightData, this.pos,
            this.waveCode, this.blockBaseY, this.upperHalf, ctm.tintIndex(), blockState, resources.region, blockPos);
}
```

Then continue rendering the base `quadView` unchanged (do NOT skip the base quad for overlays).

- [ ] **Step 3: Create the overlay emitter**

Create `src/main/java/net/vulkanmod/render/ctm/CtmOverlayEmitter.java`. It mirrors `BlockRenderer.putQuadData` but (a) writes to the passed overlay buffer, (b) applies a small outward positional bias along the quad normal to avoid z-fighting, and (c) resolves tint from the block color provider when `tintIndex >= 0`, else white. Reuse the existing static `BlockRenderer.putQuadData(bufferBuilder, pos, quad, quadLightData, r, g, b, waveCode, blockBaseY, upperHalf)` by first computing `r,g,b` from tint, and pass a bias-adjusted `pos` copy:

```java
package net.vulkanmod.render.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Vector3f;

public final class CtmOverlayEmitter {
    private CtmOverlayEmitter() {}

    public static void emit(TerrainBufferBuilder buffer, QuadView quad, QuadLightData light, Vector3f pos,
                            int waveCode, float blockBaseY, boolean upperHalf, int tintIndex,
                            BlockState state, BlockAndTintGetter region, BlockPos blockPos) {
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (tintIndex >= 0) {
            int color = BlockRenderer.tint(state, region, blockPos, tintIndex);
            r = ColorUtil.ARGB.unpackR(color);
            g = ColorUtil.ARGB.unpackG(color);
            b = ColorUtil.ARGB.unpackB(color);
        }
        Vec3i n = quad.getFacingDirection().getNormal();
        Vector3f biased = new Vector3f(pos.x() + n.getX() * 0.002F, pos.y() + n.getY() * 0.002F, pos.z() + n.getZ() * 0.002F);
        BlockRenderer.putQuadData(buffer, biased, quad, light, r, g, b, waveCode, blockBaseY, upperHalf);
    }
}
```

Add a small static helper on `BlockRenderer` to expose tint (the class already holds `blockColors`):

```java
public static int tint(BlockState state, BlockAndTintGetter region, BlockPos pos, int tintIndex) {
    return blockColors.getColor(state, region, pos, tintIndex);
}
```

- [ ] **Step 4: Build + deploy**

Run build + deploy (as Task 6 Step 4).

- [ ] **Step 5: In-game check**

User with Nature X active in taiga/forest/plains. Expected: flower/detail overlays on leaves (overlay_random with biomes), rock/carpet overlays on coarse dirt, matching the Sodium+Continuity reference. Overlays must not z-fight (flicker) and must be transparent where the overlay tile is empty. No crash.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/vulkanmod/render/chunk/build/BlockRenderer.java src/main/java/net/vulkanmod/render/ctm/CtmOverlayEmitter.java
git commit -m "Emit CTM overlay quads for leaf and ground detail"
```

---

## Task 10: Config toggle + final Nature X validation

**Files:**
- Modify: `src/main/java/net/vulkanmod/config/option/Options.java` (add a CTM cycling/switch option)
- (Config field `ctmEnabled` already added in Task 5.)

**Interfaces:**
- Consumes: `Config.ctmEnabled` (read by `Ctm.isActive`).
- Produces: a user-facing toggle in the video/quality options that flips `ctmEnabled` and triggers a chunk rebuild.

- [ ] **Step 1: Inspect an existing boolean option**

Run: `grep -n "leavesCulling\|SwitchOption\|new CyclingOption\|triggerRebuild\|WorldRenderer.get" src/main/java/net/vulkanmod/config/option/Options.java | head -30`
Copy the pattern of an existing boolean like `leavesCulling` (which also needs a terrain rebuild on change).

- [ ] **Step 2: Add the CTM option**

Add a boolean/switch option "Connected Textures (CTM)" bound to `config.ctmEnabled`, and on change trigger a full terrain rebuild the same way `leavesCulling` does (e.g. `WorldRenderer.getInstance().allChanged()` — use whatever the neighboring option uses). Place it near the other quality toggles. Provide the translation key in `assets/vulkanmod/lang/en_us.json` (mirror the `leavesCulling` key entry).

- [ ] **Step 3: Build + deploy**

Run build + deploy (as Task 6 Step 4).

- [ ] **Step 4: In-game validation (full acceptance)**

User:
1. With CTM ON + Nature X: leaves show biome overlays, dirt shows overlay layer, grass varies per biome — matching Sodium+Continuity.
2. Toggle CTM OFF: terrain returns to plain vanilla-Vulkan (shader-external principle) with no leftover overlays after the rebuild.
3. No FPS regression with CTM OFF vs today; acceptable cost with CTM ON.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/vulkanmod/config/option/Options.java src/main/resources/assets/vulkanmod/lang/en_us.json
git commit -m "Add Connected Textures toggle to video options"
```

---

## Self-Review Notes

- **Spec coverage:** §2 methods → Tasks 1,8,9 (fixed/random/repeat swap; overlay_fixed/random). §2 matching/filters → Tasks 3,4 (matchTiles/Blocks, biomes, faces, heights, weights). §3.3 atlas → Task 6. §3.1 loader → Task 7. §3.4 resolver → Task 5. §3.5 hook → Tasks 8,9. §5 perf fast-path → `Ctm.isActive` (Task 5) + empty store. §6 failure handling → try/catch in `Ctm.resolve` (Task 5) and parser (Tasks 4,7). §2 toggle / shader-external → Tasks 5,10.
- **Placeholder scan:** the two atlas/reload wiring tasks (6,7) intentionally require reading the real mixin locals first (Step 2 of each) because the exact injection locals cannot be known without the file; the edit shape is given. This is a discovery step, not a placeholder.
- **Type consistency:** `Ctm.resolve(sprite,state,pos,face,region)` signature is identical in Tasks 5, 8, 9. `CtmResult.Kind {NONE,SWAP,OVERLAY}` used consistently. `BlockRenderer.putQuadData(...)` reused with the exact existing signature. `ctmEnabled` added once (Task 5), consumed in Tasks 5/10.
- **Known verification points during execution:** real constant name for mipped cutout in `TerrainRenderType`; exact `MTextureAtlas` sprite-gather locals; exact `BuilderResources.builderPack` accessor; exact rebuild call used by `leavesCulling`.
