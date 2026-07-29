# Support des shaders sur le LOD Caldera — Plan d'implémentation (Phase 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rendre le terrain lointain (LOD Caldera) cohérent avec le terrain proche quand le shader interne Camille est actif — pas de couture (même normale gbuffer, même atmosphère/color-grading), à coût GPU négligeable.

**Architecture:** Les draws LOD opaques ont déjà lieu dans le framebuffer MRT `scene` de la passe monde. Il suffit que leurs fragments écrivent la 2ᵉ sortie `outNormal` (`location 1`) pour peupler `gnormal` — le nombre d'attachments du pipeline est dérivé automatiquement du render-pass lié (`GraphicsPipeline.java:153-155`). On factorise la reconstruction de normale et l'atmosphère dans un include GLSL partagé, appelé par les shaders LOD (et alignable sur le terrain proche). Les passes différées (light/reflection/glass/composite/aa) couvrent alors le lointain sans code spécifique ; ombres/SSR s'éteignent seuls au loin via `lightFade`.

**Tech Stack:** Java 21, Vulkan (LWJGL), GLSL 460 → SPIR-V, VulkanMod fork, MoltenVK, Minecraft 1.21.1 / NeoForge, Gradle.

## Global Constraints

- **ZÉRO commentaire dans le code** (règle absolue, s'applique à tout Java et GLSL écrit/réécrit).
- **Aucune mention d'IA/Claude** dans les commits, fichiers ou docs.
- **Commits en tant que `NelWenn <NelWenn@users.noreply.github.com>`** (déjà configuré dans le repo).
- **Ne PAS modifier le mod Caldera** ni le contrat vertex `EXTERNAL_LOD` / `EXTERNAL_LOD_TEXTURED` : la normale est reconstruite côté shader, aucun attribut ajouté.
- **Performance prioritaire** : pas de POM/SSR/ombres portées/cascade supplémentaire sur le lointain.
- **Cible** : Minecraft 1.21.1 / NeoForge, MoltenVK / M1 Pro.
- **Build & déploiement** : `./gradlew build -x test` puis copier `build/libs/Volcanic-1.21.1-*.jar` dans le profil Modrinth « NeoForge 1.21.1 ». Tester **uniquement** dans ce profil.

---

## Boucle de vérification (pas de tests unitaires GLSL/rendu)

Ce code est du shader + orchestration Vulkan : il n'y a pas de harnais de test unitaire. La vérification de chaque tâche = **build qui compile** + **observation en jeu** (éventuellement via une sortie de debug temporaire). Commande de build + déploiement à réutiliser à chaque étape « Build & deploy » :

```bash
cd /Users/theoschneider/IdeaProjects/MoltenVulkanMod
./gradlew build -x test
cp build/libs/Volcanic-1.21.1-*.jar "$HOME/Library/Application Support/ModrinthApp/profiles/NeoForge 1.21.1/mods/"
```

Puis lancer le profil « NeoForge 1.21.1 » (Caldera + shader Camille actifs), se placer face à un horizon LOD, observer, et — si demandé — comparer les FPS via l'overlay de debug (F3) ou le FrameTimer du mod.

## File Structure (Phase 1)

- **Create** `src/main/resources/assets/vulkanmod/shaders/include/lod_common.glsl`
  — helpers partagés LOD : reconstruction de normale géométrique + atmosphère/brouillard (+ directionnel en Task 3). Une seule responsabilité : le shading gbuffer commun du LOD.
- **Modify** les 4 vertex shaders LOD **opaques** (ajout du varying `vWorldPos`) :
  `external_lod/lod.vsh`, `external_lod_tex/lod_tex.vsh`, `external_lod_solid/lod_solid.vsh`, `external_lod_tex_solid/lod_tex_solid.vsh`.
- **Modify** les 4 fragment shaders LOD **opaques** (sortie `outNormal` + atmosphère partagée) :
  `external_lod/lod.fsh`, `external_lod_tex/lod_tex.fsh`, `external_lod_solid/lod_solid.fsh`, `external_lod_tex_solid/lod_tex_solid.fsh`.
- **Modify** les 2 fragment shaders LOD **eau** (atmosphère partagée seule, pas de gnormal) :
  `external_lod_water/lod_water.fsh`, `external_lod_water_tex/lod_water_tex.fsh`.
- **Modify (Task 3, conditionnel)** `src/main/java/net/vulkanmod/api/CalderaBridge.java` (`writeFogUniforms`) + les 6 `ExternalLod*Pipeline.java` (champ UBO sun dir) — uniquement si le directionnel s'avère nécessaire.

> Rappel varyings : famille **flat** (`lod`, `lod_solid`) utilise les locations 0 (`vertexColor`), 1 (`vViewDist`) → prochaine libre = **2**. Famille **textured** (`lod_tex`, `lod_tex_solid`) utilise 0..3 → prochaine libre = **4**. Le varying `vWorldPos` doit avoir la **même** location dans le `.vsh` et le `.fsh` d'une même famille.

---

### Task 1 : Contrat gbuffer — écrire la normale monde depuis le LOD opaque

**Files:**
- Create: `src/main/resources/assets/vulkanmod/shaders/include/lod_common.glsl`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod/lod.vsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod/lod.fsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod_solid/lod_solid.vsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod_solid/lod_solid.fsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod_tex/lod_tex.vsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod_tex/lod_tex.fsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod_tex_solid/lod_tex_solid.vsh`
- Modify: `src/main/resources/assets/vulkanmod/shaders/basic/external_lod_tex_solid/lod_tex_solid.fsh`

**Interfaces:**
- Produces (GLSL, appelé en Task 2/3) :
  - `vec3 lod_geo_normal(vec3 camRelPos)` — normale géométrique par dérivées, orientée vers la caméra.
- Produces (contrat gbuffer) : chaque `.fsh` opaque écrit `layout(location = 1) out vec4 outNormal = vec4(N, 1.0)` dans l'attachment `gnormal`.

- [ ] **Step 1 : Créer l'include partagé `lod_common.glsl`**

Contenu **intégral** du nouveau fichier `src/main/resources/assets/vulkanmod/shaders/include/lod_common.glsl` :

```glsl
vec3 lod_geo_normal(vec3 camRelPos) {
    vec3 n = normalize(cross(dFdx(camRelPos), dFdy(camRelPos)));
    return dot(n, camRelPos) > 0.0 ? -n : n;
}
```

- [ ] **Step 2 : Ajouter le varying `vWorldPos` dans les vertex shaders flat (`lod.vsh`, `lod_solid.vsh`)**

Dans `external_lod/lod.vsh` **et** `external_lod_solid/lod_solid.vsh` (fichiers identiques), après la ligne `layout (location = 1) out float vViewDist;` ajouter :

```glsl
layout (location = 2) out vec3 vWorldPos;
```

Puis, juste avant `vViewDist = length(worldPos);`, ajouter :

```glsl
    vWorldPos = worldPos;
```

- [ ] **Step 3 : Ajouter le varying `vWorldPos` dans les vertex shaders textured (`lod_tex.vsh`, `lod_tex_solid.vsh`)**

Dans `external_lod_tex/lod_tex.vsh` **et** `external_lod_tex_solid/lod_tex_solid.vsh` (identiques), après `layout (location = 3) smooth out vec2 vTileUV;` ajouter :

```glsl
layout (location = 4) out vec3 vWorldPos;
```

Puis, juste avant `vViewDist = length(worldPos);`, ajouter :

```glsl
    vWorldPos = worldPos;
```

- [ ] **Step 4 : Vérification visuelle temporaire de la normale (flat opaque)**

Dans `external_lod/lod.fsh`, après `#version 460` insérer `#include "lod_common.glsl"`, ajouter l'entrée varying et une sortie debug **temporaire**. Remplacer l'en-tête entrées/sorties :

```glsl
layout (location = 0) in vec4 vertexColor;
layout (location = 1) in float vViewDist;
layout (location = 0) out vec4 fragColor;
```

par :

```glsl
layout (location = 0) in vec4 vertexColor;
layout (location = 1) in float vViewDist;
layout (location = 2) in vec3 vWorldPos;
layout (location = 0) out vec4 fragColor;
```

Puis remplacer **temporairement** la dernière ligne `fragColor.rgb = mix(fragColor.rgb, ExternalLodFogColor.rgb, fogFactor);` par :

```glsl
    fragColor = vec4(lod_geo_normal(vWorldPos) * 0.5 + 0.5, 1.0);
```

- [ ] **Step 5 : Build & deploy, observer les normales**

Lancer la boucle « Build & deploy ». En jeu, face à l'horizon LOD : les cellules lointaines doivent s'afficher en **couleurs de normale** (faces horizontales ~ vert `(0.5,1,0.5)`, faces verticales teintées selon l'orientation), cohérentes par face. Pas de crash, pas d'écran noir.
Attendu : PASS si les couleurs de normale sont plausibles et stables (pas de scintillement aberrant).

- [ ] **Step 6 : Basculer la sortie debug en vraie écriture `outNormal` (les 4 opaques)**

Dans **chacun** des 4 fragment shaders opaques, appliquer le patron final :

1. Après `#version 460` : `#include "lod_common.glsl"`.
2. Ajouter l'entrée varying `vWorldPos` (flat : `layout (location = 2) in vec3 vWorldPos;` ; textured : `layout (location = 4) in vec3 vWorldPos;`).
3. Ajouter la sortie normale après `layout (location = 0) out vec4 fragColor;` :
   ```glsl
   layout (location = 1) out vec4 outNormal;
   ```
4. Retirer la sortie debug de Step 4 et, **à la fin de `main()`** (après le calcul de `fragColor`), écrire :
   ```glsl
   outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);
   ```

Pour `external_lod/lod.fsh` le `main()` final (fog inchangé pour l'instant, unifié en Task 2) :

```glsl
void main() {
    float clipDistance = ExternalLodRenderParams.y;
    bool dither = ExternalLodRenderParams.w != 0.0;
    float viewDistance = vViewDist;

    if (clipDistance > 0.0) {
        if (dither) {
            float noise = bayer8x8(gl_FragCoord.xy) + 0.001;
            float bandStart = ExternalLodFogParams.z > 0.0 ? ExternalLodFogParams.z : clipDistance * 0.85;
            float bandEnd = ExternalLodFogParams.w > bandStart ? ExternalLodFogParams.w : clipDistance * 1.7;
            float fadeStep = smoothstep(bandStart, bandEnd, viewDistance);
            if (fadeStep <= noise) {
                discard;
            }
        } else if (viewDistance < clipDistance) {
            discard;
        }
    }

    fragColor = vertexColor;

    float fogFactor = smoothstep(ExternalLodFogParams.x, ExternalLodFogParams.y, viewDistance);
    fragColor.rgb = mix(fragColor.rgb, ExternalLodFogColor.rgb, fogFactor);

    outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);
}
```

Pour `external_lod_solid/lod_solid.fsh` (pas de bloc clip/dither) :

```glsl
void main() {
    fragColor = vertexColor;

    float viewDistance = vViewDist;
    float fogFactor = smoothstep(ExternalLodFogParams.x, ExternalLodFogParams.y, viewDistance);
    fragColor.rgb = mix(fragColor.rgb, ExternalLodFogColor.rgb, fogFactor);

    outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);
}
```

Pour `external_lod_tex/lod_tex.fsh` (garder le bloc clip/dither existant et le sampling `textureGrad`), ajouter en fin de `main()` :

```glsl
    outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);
```

Pour `external_lod_tex_solid/lod_tex_solid.fsh` (pas de bloc clip/dither), ajouter en fin de `main()` :

```glsl
    outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);
```

- [ ] **Step 7 : Build & deploy, vérifier l'absence de régression**

Lancer « Build & deploy ». En jeu : le terrain LOD s'affiche **normalement** (couleurs/atlas corrects, pas de normales visibles), aucun crash, aucune régression visuelle vs avant. La normale est désormais dans `gnormal` (invisible directement, consommée par le composite).
Attendu : PASS si le rendu LOD est identique à l'avant-tâche (au grading près) et stable.

- [ ] **Step 8 : Commit**

```bash
git add src/main/resources/assets/vulkanmod/shaders/include/lod_common.glsl \
        src/main/resources/assets/vulkanmod/shaders/basic/external_lod/ \
        src/main/resources/assets/vulkanmod/shaders/basic/external_lod_solid/ \
        src/main/resources/assets/vulkanmod/shaders/basic/external_lod_tex/ \
        src/main/resources/assets/vulkanmod/shaders/basic/external_lod_tex_solid/
git commit -m "LOD opaque terrain writes world normal into the gbuffer"
```

---

### Task 2 : Unifier l'atmosphère/brouillard proche↔lointain

**Files:**
- Modify: `src/main/resources/assets/vulkanmod/shaders/include/lod_common.glsl`
- Modify: les 4 `.fsh` opaques + les 2 `.fsh` eau (`external_lod_water/lod_water.fsh`, `external_lod_water_tex/lod_water_tex.fsh`)
- Modify (optionnel, si mismatch de couleur observé) : `src/main/java/net/vulkanmod/api/CalderaBridge.java` (`writeFogUniforms`)

**Interfaces:**
- Consumes : `ExternalLodFogColor`, `ExternalLodFogParams` (UBO LOD existant).
- Produces (GLSL) :
  - `vec3 lod_atmosphere(vec3 color, float dist, vec3 fogColor, float fogStart, float fogEnd)` — mélange atmosphérique partagé, identique à la forme du fog du terrain proche (`fog.glsl` `linear_fog` avec alpha = 1).

- [ ] **Step 1 : Ajouter `lod_atmosphere` à l'include partagé**

Ajouter à `lod_common.glsl` :

```glsl
vec3 lod_atmosphere(vec3 color, float dist, vec3 fogColor, float fogStart, float fogEnd) {
    return mix(color, fogColor, smoothstep(fogStart, fogEnd, dist));
}
```

- [ ] **Step 2 : Remplacer le fog inline par `lod_atmosphere` dans les 6 fragment shaders LOD**

Dans chaque `.fsh` (4 opaques + 2 eau), remplacer les deux lignes de fog inline :

```glsl
    float fogFactor = smoothstep(ExternalLodFogParams.x, ExternalLodFogParams.y, viewDistance);
    fragColor.rgb = mix(fragColor.rgb, ExternalLodFogColor.rgb, fogFactor);
```

par :

```glsl
    fragColor.rgb = lod_atmosphere(fragColor.rgb, viewDistance, ExternalLodFogColor.rgb, ExternalLodFogParams.x, ExternalLodFogParams.y);
```

Pour les 2 fichiers eau, ajouter aussi `#include "lod_common.glsl"` après `#version 460` (les opaques l'ont déjà via Task 1).

- [ ] **Step 3 : Build & deploy, vérifier la continuité du brouillard**

Lancer « Build & deploy ». En jeu, à la frontière render-distance↔LOD : la teinte et la progression du brouillard doivent être **continues** (pas de saut de couleur ni de marche de densité au raccord). Comparer proche vs lointain sous plusieurs météos/heures.
Attendu : PASS si aucune couture de brouillard n'est visible.

- [ ] **Step 4 (conditionnel) : Forcer la couleur de fog LOD = couleur de fog du shader**

Uniquement **si** Step 3 révèle un décalage de teinte entre le fog proche et le fog LOD. Repérer la source de la couleur de fog du terrain proche :

```bash
grep -n "FogColor" src/main/java/net/vulkanmod/vulkan/shader/Uniforms.java
```

Suivre le supplier référencé pour obtenir le getter de couleur de fog courant (celui qui alimente l'uniform `FogColor` du terrain), et l'utiliser dans `CalderaBridge.writeFogUniforms` pour écraser `fogRed/fogGreen/fogBlue` par cette couleur avant de remplir `FOG_COLOR` (lignes 1036-1039). Cela garantit une couleur identique quel que soit ce que Caldera fournit. Re-build & deploy et re-vérifier la continuité.
Attendu : PASS si la teinte du fog est identique proche↔lointain.

- [ ] **Step 5 : Commit**

```bash
git add src/main/resources/assets/vulkanmod/shaders/
# si Step 4 appliqué :
# git add src/main/java/net/vulkanmod/api/CalderaBridge.java
git commit -m "Unify LOD atmosphere with near terrain fog"
```

---

### Task 3 : (Polish, conditionnel) Cohérence de la lumière directionnelle au loin

> **Ne réaliser cette tâche QUE si**, après Task 2, le terrain lointain paraît visiblement plus **plat** que le proche (versants ensoleillés qui n'accrochent pas le soleil). Sinon, sauter — le lightmap (via `uLightMap`) suffit déjà à la cohérence de base. Cette tâche ajoute un champ à l'UBO LOD.

**Files:**
- Modify: `src/main/resources/assets/vulkanmod/shaders/include/lod_common.glsl`
- Modify: les 4 `.fsh` opaques + leur bloc UBO `ExternalLodUniforms`
- Modify: `src/main/java/net/vulkanmod/api/CalderaBridge.java` (`writeFogUniforms`)
- Modify: les 6 `src/main/java/net/vulkanmod/vulkan/shader/pipeline/definitions/ExternalLod*Pipeline.java` (champ `LodUbo`)

**Interfaces:**
- Consumes : direction soleil (même source que l'uniform `FogSunDir` du terrain).
- Produces (GLSL) :
  - `vec3 lod_directional(vec3 color, vec3 N, vec3 sunDir)` — éclaircissement directionnel léger, borné, constant en distance.

- [ ] **Step 1 : Ajouter le champ `ExternalLodSunDir` à l'UBO des 6 pipelines (Java)**

Dans **chacun** des 6 fichiers `ExternalLod*Pipeline.java`, dans la classe `LodUbo`, ajouter le champ **après** `Vector4f ExternalLodFogParams;` et **avant** le tableau `ExternalLodCellOrigins` (l'ordre des champs doit refléter exactement le std140 du bloc GLSL) :

```java
        Vector4f ExternalLodSunDir;
```

- [ ] **Step 2 : Ajouter le même champ dans le bloc UBO GLSL des 6 shaders**

Dans chaque `.vsh`/`.fsh` qui déclare `uniform ExternalLodUniforms { ... }`, ajouter la ligne **après** `vec4 ExternalLodFogParams;` et **avant** `vec4 ExternalLodCellOrigins[1024];` :

```glsl
    vec4 ExternalLodSunDir;
```

(Modifier tous les fichiers déclarant ce bloc pour garder une disposition std140 identique : les 6 `.vsh` et les 6 `.fsh`.)

- [ ] **Step 3 : Peupler `ExternalLodSunDir` côté Java**

Repérer la source de la direction soleil du terrain proche :

```bash
grep -n "FogSunDir" src/main/java/net/vulkanmod/vulkan/shader/Uniforms.java
```

Dans `CalderaBridge`, ajouter un `MappedBuffer SUN_DIR` (aligné comme `FOG_PARAMS`) au bon offset du bloc UBO, et le remplir dans `writeFogUniforms` avec la direction soleil courante (xyz), `w = 0`. Écrire les 3 composantes via `putFloat(0/4/8, ...)` et `putFloat(12, 0.0f)`.

- [ ] **Step 4 : Ajouter `lod_directional` à l'include et l'appliquer**

Ajouter à `lod_common.glsl` :

```glsl
vec3 lod_directional(vec3 color, vec3 N, vec3 sunDir) {
    vec3 s = normalize((sunDir.y >= 0.0 ? sunDir : -sunDir) + vec3(1e-4));
    float ndl = max(dot(N, s), 0.0);
    return color * (1.0 + 0.12 * ndl);
}
```

Dans les 4 `.fsh` opaques, juste avant l'écriture de `outNormal`, insérer :

```glsl
    vec3 lodN = lod_geo_normal(vWorldPos);
    fragColor.rgb = lod_directional(fragColor.rgb, lodN, ExternalLodSunDir.xyz);
    outNormal = vec4(lodN, 1.0);
```

(et supprimer l'ancienne ligne `outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);` pour ne pas recalculer la normale deux fois.)

- [ ] **Step 5 : Build & deploy, régler l'intensité**

Lancer « Build & deploy ». En jeu : les versants lointains orientés vers le soleil doivent s'éclaircir légèrement, de façon **cohérente** avec le proche, sans sur-éclat. Ajuster le facteur `0.12` si nécessaire (plus bas = plus subtil) et re-build.
Attendu : PASS si le lointain n'est plus perçu comme plat et qu'aucun banding/sur-éclat n'apparaît.

- [ ] **Step 6 : Commit**

```bash
git add src/main/resources/assets/vulkanmod/shaders/ \
        src/main/java/net/vulkanmod/api/CalderaBridge.java \
        src/main/java/net/vulkanmod/vulkan/shader/pipeline/definitions/
git commit -m "Coherent directional sun shading on far LOD terrain"
```

---

### Task 4 : Validation finale — couture, perf et non-régression

**Files:** aucun (validation).

- [ ] **Step 1 : Non-régression vanilla (sans shader)**

Désactiver le shader Camille (rester en vanilla), Caldera actif. Vérifier que le rendu LOD est **identique** à l'état antérieur au plan (les shaders LOD écrivent `outNormal` mais `gnormal` n'est consommé par personne → aucun effet visible).
Attendu : PASS si le LOD vanilla est inchangé.

- [ ] **Step 2 : Couture proche↔lointain (shader actif)**

Shader Camille actif, Caldera actif. Parcourir plusieurs biomes/heures : vérifier l'absence de couture d'albédo, de normale (éclairage) et de grading à la frontière render-distance↔LOD.
Attendu : PASS si aucune couture nette n'est visible.

- [ ] **Step 3 : Performance**

Comparer les FPS (overlay F3 / FrameTimer) horizon LOD visible, avant/après le plan, dans la même scène. Le surcoût doit être négligeable (écriture `gnormal` on-tile + quelques ALU).
Attendu : PASS si l'écart FPS reste dans le bruit de mesure (pas de chute franche).

- [ ] **Step 4 : Cas limites**

Vérifier : bord extrême du LOD (depth proche de 1.0) sans artefact de « ciel » marqué ; transition dithered d'apparition des cellules toujours fonctionnelle ; eau lointaine correctement brumée et non opaque.
Attendu : PASS si aucun artefact bloquant. Noter tout écart pour ajustement.

---

## Self-review (couverture de la spec Phase 1)

- Contrat gbuffer LOD (spec §4) → **Task 1** (écriture `outNormal`).
- C1 includes partagés (spec §5) → **Task 1** (`lod_common.glsl`) + **Task 2** (`lod_atmosphere`).
- C2 normale LOD (spec §5) → **Task 1**.
- C3 atmosphère/fog unifiée (spec §5) → **Task 2**.
- C4 directionnel (spec §5) → **Task 3** (conditionnel).
- Perf (spec §7) → **Task 4 Step 3** + choix de conception (pas d'effet coûteux ajouté).
- Risque render-pass MRT (spec §8.1) → auto-résolu (`GraphicsPipeline.java:153-155`), confirmé par **Task 1 Step 5/7**.
- Continuité fog (spec §8.2) → **Task 2 Step 3-4**.
- Non-régression vanilla / cas limites (spec §8.4, §11) → **Task 4**.

Hors Phase 1 : C5 (hook d'override + auto-dérivation SCSS) — voir Phase 2 ci-dessous.

---

## Phase 2 (plan séparé — à détailler après validation in-game de la Phase 1)

L'universalité « par construction » est déjà livrée en Phase 1 : tout shader qui lit le gbuffer
(`scene` + `gnormal`) couvre le lointain sans code LOD. La Phase 2 branche le cas concret des
**packs SCSS tiers** via auto-dérivation, et nécessite une étude fine de
`render/sodium/SodiumTerrainCompiler.java` + `SodiumShaderBridge.java` avant d'écrire du code
sans placeholder. Esquisse (à convertir en plan bite-sized dédié) :

- **P2-T1** — Hook d'override au bind LOD : introduire une consultation d'override dans
  `CalderaBridge.beginLodDrawInternal` (switch lignes 445-453), calquée sur
  `PipelineManager.getTerrainShader` (`PipelineManager.java:87-90`) qui consulte déjà
  `SodiumShaderBridge`. Défaut → pipelines LOD internes (Phase 1).
- **P2-T2** — `LodShaderDerivation` : compiler une variante LOD du fragment d'un pack SCSS
  (réutiliser la logique de `SodiumTerrainCompiler.macrosFor` / wrapping), alimentée depuis les
  attributs `EXTERNAL_LOD` / `EXTERNAL_LOD_TEXTURED` et sortie dans le contrat gbuffer §4.
- **P2-T3** — Registre par pack, rafraîchi au reload de resource pack (comme
  `SodiumShaderBridge.refresh`, hook `GameRendererMixin:107`), + validation in-game avec un pack
  SCSS réel.
