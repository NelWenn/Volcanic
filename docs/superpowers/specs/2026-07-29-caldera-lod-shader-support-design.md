# Support des shaders sur le terrain LOD Caldera — Design

**Date :** 2026-07-29
**Branche :** caldera-api
**Statut :** design validé, prêt pour le plan d'implémentation

## 1. Objectif

Rendre le terrain lointain (LOD, fourni par le mod compagnon Caldera via `CalderaBridge`)
cohérent avec le terrain proche **quand un shader est actif** — pas de couture visible entre
les chunks détaillés proches et les cellules LOD lointaines. Le rendu LOD doit ressembler au
vanilla + shader (atmosphère, color-grading et éclairage continus).

Deux contraintes fortes :

- **Universel.** N'importe quel shader (le shader interne « Camille Radiance », un pack SCSS
  Sodium, un futur shader tiers) doit couvrir le LOD sans code spécifique par shader.
- **Performant.** Priorité affichée. Aucun effet coûteux (SSR, POM, cascade d'ombre
  supplémentaire) ne doit être ajouté sur le lointain.

## 2. Constat technique : pourquoi le LOD court-circuite le shader aujourd'hui

Le terrain proche est un rendu *forward+* : `terrain.fsh` écrit **deux** sorties MRT et les
passes différées enrichissent l'image ensuite.

- Terrain proche — `shaders/basic/terrain/terrain.fsh:27-28` :
  - `location 0` → `fragColor` = couleur ombrée (albedo × lightmap × relief/POM, + fog),
    atterrit dans l'attachment `scene`.
  - `location 1` → `outNormal` = normale monde, `w = 1.0`, atterrit dans `gnormal`
    (RGBA16F, `DefaultMainPass.java:159`, exposé au framegraph en `DefaultMainPass.java:729`).
- Terrain LOD — `shaders/basic/external_lod/lod.fsh:14` et `external_lod_tex/lod_tex.fsh:18` :
  - **une seule** sortie `location 0` = couleur plate + fog linéaire maison
    (`ExternalLodFogColor` / `ExternalLodFogParams`, `lod.fsh:52-55`).
  - **pas de `outNormal`, pas de material-ID.** Les fragments LOD laissent `gnormal` à sa
    valeur de clear (`w = 0`).

Conséquences quand Camille est actif :

- Le composite (`shaders/basic/radiance_composite/radiance_composite.fsh`) lit `scene`,
  `gnormal` (Sampler8, ligne 115), la lumière, les reflets, puis applique un tonemap/knee
  (`radiance_composite.fsh:140-141`). Ce knee s'applique à **tout** ce qui est dans `scene`,
  **y compris le lointain** → le color-grading est déjà cohérent pour le LOD *pour peu qu'il
  écrive dans `scene`* (ce qui est le cas).
- MAIS : `gnormal.w = 0` pour le LOD → le composite retombe sur une normale reconstruite par
  dérivées (`radiance_composite.fsh:92-93, 116`), et surtout le fog du LOD (formule et couleur
  propres) diffère du fog atmosphérique du proche → **couture visible**.
- Les ombres CSM et le SSR **s'éteignent d'eux-mêmes au loin** via `lightFade`
  (`radiance_composite.fsh:100-101`, `shadowFar = FogShadowSplits.z`) : au-delà de la portée
  d'ombre, `shadowTerm → 0`. Il n'y a donc **rien à désactiver** pour éviter les effets
  coûteux sur le lointain — ils disparaissent tout seuls. C'est exactement le palier voulu.

**Le trou à combler** se réduit donc à deux choses : (1) écrire la normale monde depuis le
LOD, (2) unifier l'atmosphère/lumière proche↔lointain.

## 3. Décisions de cadrage (validées)

- **Fidélité LOD = « cohérence atmo + lumière ».** Le LOD écrit sa normale, passe par le
  composite (même grading), partage le même modèle atmosphère/brouillard + un éclairage
  directionnel ciel/soleil. **Pas** de SSR / POM / ombres portées sur le lointain.
- **Mécanisme universel = « contrat implicite + includes partagés ».** Le LOD écrit le même
  contrat gbuffer que le proche et réutilise les mêmes fonctions GLSL ; tout shader qui lit
  déjà le gbuffer couvre le lointain sans code LOD. Pour SCSS, on auto-dérive une variante LOD
  du fragment du pack (comme `SodiumTerrainCompiler` le fait pour le proche).
- **On ne modifie pas le mod Caldera.** Le contrat vertex (`EXTERNAL_LOD` /
  `EXTERNAL_LOD_TEXTURED`, `CustomVertexFormat.java:23-33`) reste inchangé : la normale est
  reconstruite côté shader par dérivées, sans attribut de normale supplémentaire.

## 4. Architecture : le contrat gbuffer LOD (l'API universelle)

> **Tout pipeline LOD opaque, quel que soit le shader actif, écrit le même gbuffer que le
> terrain proche :**
> - `location 0` → `scene` : couleur ombrée en espace *pré-composite*
>   (albedo × lightmap × directionnel, + atmosphère).
> - `location 1` → `gnormal` : normale monde géométrique, `w = 1.0`.

Les draws LOD opaques ont lieu pendant la phase solide, **dans le même framebuffer MRT scaled**
que le terrain proche (2 attachments couleur + depth, `DefaultMainPass.java:157-161`). Écrire
`outNormal` en `location 1` le range donc automatiquement dans `gnormal`, et **toutes** les
passes différées (`RadianceGraph` : light → reflection → glass → composite → aa) couvrent le
lointain **exactement comme le proche, sans branche spécifique**. C'est l'universalité « par
construction ».

**Chemin vanilla (aucun shader actif) : inchangé.** Pas de consommateur gbuffer, `gnormal`
ignoré, le LOD garde son rendu actuel. Le contrat n'est « activé » que lorsqu'un shader est en
place.

## 5. Composants

### C1 — Includes de shading partagés
Nouvel include `shaders/include/atmosphere.glsl` (+ un helper directionnel), à côté de
`fog.glsl` / `light.glsl`. Il factorise la fonction atmosphère/brouillard et le terme
directionnel soleil, **définis une seule fois**, appelés par `terrain.fsh` **et** les shaders
LOD. C'est le socle « défini une fois → proche & lointain identiques ». La forme actuelle du
fog proche (`fog.glsl:1-3`, `linear_fog`) et du fog LOD (`lod.fsh:54-55`) est déjà la même
famille `mix(color, fogColor, smoothstep(start, end, dist))` → la factorisation est directe.

### C2 — Normale LOD (gbuffer)
Dans les fragments LOD opaques (`lod.fsh`, `lod_tex.fsh`, et variantes `_solid`), reconstruire :
```
geoN = normalize(cross(dFdx(worldPos), dFdy(worldPos)));  // orientée vers la caméra
outNormal = vec4(geoN, 1.0);
```
identique à `terrain.fsh:86-91`. Nécessite que les VS LOD sortent `worldPos`
(`lod.vsh` calcule déjà `worldPos`, ligne 29 — il suffit de le passer en varying). Coût :
1 cross + 1 normalize par fragment. Les pipelines LOD opaques doivent déclarer la 2ᵉ sortie et
cibler le render-pass MRT 2-attachments (voir §8, risque principal).

### C3 — Atmosphère / brouillard unifiée
Le fog LOD maison est remplacé par la fonction atmosphère partagée (C1), évaluée à la **vraie
distance monde**, **continue** à la frontière render-distance↔LOD. Deux leviers :
- **Côté Java (principal, faible risque) :** peupler les uniformes de fog du LOD
  (`ExternalLodFogColor` / `ExternalLodFogParams`, écrits dans `CalderaBridge.writeSharedUniforms`
  ~ligne 1010) pour **continuer** la courbe de fog du proche dans la plage LOD, et **étendre**
  la portée du fog proche (`FogEnd`) jusqu'à la distance LOD quand le LOD est actif — sinon le
  proche devient opaque avant que le lointain apparaisse. Quand le LOD est inactif, le fog
  proche garde sa portée render-distance (comportement vanilla préservé).
- **Côté GLSL (exactitude) :** faire appeler la **même** fonction atmosphère par les deux
  côtés pour garantir une forme identique.
- La **bande de fade + dither Bayer** du LOD (transition d'apparition des cellules,
  `lod.fsh:38-50`) est **conservée telle quelle** — c'est un mécanisme Caldera orthogonal au
  brouillard atmosphérique.

### C4 — Cohérence lumière directionnelle sur le lointain
Le proche tire son « versant ensoleillé » du highlight du composite
(`radiance_composite.fsh:109-113`), qui **s'éteint au loin** (`lightFade`). Pour éviter que le
lointain paraisse plus plat, on ajoute dans le shading LOD un terme directionnel léger
`N·soleil` (via `geoN` de C2 et `FogSunDir`), **constant en distance**, calé sur la même
`lightCol`/force que le highlight du composite. Coût : 1 dot. Tunable/désactivable ; réglage
final validé en jeu.

### C5 — Couture d'universalité (Phase 2)
Un hook d'override dans `CalderaBridge.beginLodDrawInternal` (le `switch` pass→pipeline,
~lignes 445-453), **calqué sur `PipelineManager.getTerrainShader` (`PipelineManager.java:87-90`)**
qui consulte déjà l'override SCSS :
- défaut → jeu de pipelines LOD internes enrichis (C1-C4) ;
- SCSS actif → **variantes LOD auto-dérivées** du fragment du pack, produites par un
  `LodShaderDerivation` analogue à `SodiumTerrainCompiler` (réutilise la logique de
  block-shading du pack, l'enveloppe pour la sortir dans le contrat gbuffer §4 en l'alimentant
  depuis les attributs `EXTERNAL_LOD` au lieu de `COMPRESSED_TERRAIN`).
- Registre rafraîchi au reload de resource pack, comme `SodiumShaderBridge.refresh`.

## 6. Flux d'une frame

```
FRAME_START    : shadow map (inchangé, portée proche)
Phase solide   : terrain proche  → scene + gnormal        (MRT scaled)
                 Caldera LOD      → scene + gnormal        (MÊME MRT, contrat §4)
Phase transluc : eau proche + eau LOD → scene + gnormal    (fog cohérent, pas de SSR lointain)
POST_PROCESS   : light / reflection / glass / composite / aa lisent le gbuffer
                 → couvrent proche ET lointain à l'identique
                 → ombres/SSR s'éteignent seuls au loin (lightFade) = coût lointain nul
```
Le matrix LOD est capturé par `VRenderSystem.captureExternalLodViewMatrix`
(`LevelRendererMixin.java:107`) ; la phase `MID_RENDER` du framegraph tourne à la bascule
opaque→translucide (`LevelRendererMixin.java:111-119`).

## 7. Performance

- **TBDR-friendly (M1 Pro).** Le 2ᵉ attachment MRT est on-tile → l'écriture `gnormal` pour les
  fragments LOD est quasi gratuite.
- **Aucun effet coûteux ajouté au loin.** POM/SSR/verre/cascade d'ombre supplémentaire ne sont
  pas ajoutés ; les effets différés existants s'éteignent seuls via `lightFade`.
- **Surcoût par fragment lointain** : 1 cross + 1 normalize (C2) + 1 dot (C4) + fog partagé
  (C3) ≈ négligeable.
- **Culling intact.** HiZ (`HiZPyramid`), snapshot depth prev-frame (`DepthSnapshot` /
  `DepthOcclusion`) et draws indirects + GPU cull (`LodCulling`) ne sont pas touchés.

## 8. Risques & points à valider en jeu

1. **Compat render-pass (risque principal).** Les pipelines LOD opaques doivent être créés
   contre le render-pass MRT 2-attachments (comme `terrain`), sinon la 2ᵉ sortie n'est pas
   écrite. Vérifier comment `ExternalLod*Pipeline` obtient son render-pass / état de blend et
   l'aligner sur celui du terrain proche.
2. **Continuité du fog** à la frontière render-distance↔LOD : réglage de l'extension du fog
   proche + raccord de `ExternalLodFogParams` (C3). À valider visuellement.
3. **Cohérence de projection.** La matrice de draw LOD (`ExternalLodCombinedMatrix`) doit être
   cohérente avec `FogInvMVPMat` (reconstruction depth du composite, `radiance_composite.fsh:36`),
   sinon les effets depth-based sont incohérents au loin. Impact faible à ce palier (ombres/SSR
   éteints, normale écrite explicitement), mais à vérifier.
4. **Depth ≥ 0.9999 classé « ciel »** par le composite (`radiance_composite.fsh:73`) au loin
   extrême → léger écart de grading (le pixel court-circuite le knee). Cas limite à surveiller ;
   corrigeable en abaissant le seuil ou en s'appuyant sur `gnormal.w`.

## 9. Séquencement

- **Phase 1 — le shader interne sur LOD, sans couture.** C1 (includes partagés) + C2 (normale
  gbuffer) + C3 (atmosphère/fog unifiés) + C4 (directionnel). Livrable testable directement en
  jeu : ton shader Camille sur le terrain lointain, rendu vanilla-seamless.
- **Phase 2 — universalité réelle.** C5 (hook d'override + auto-dérivation SCSS). L'universalité
  « par construction » est déjà présente en Phase 1 (tout shader lisant le gbuffer couvre le
  lointain) ; la Phase 2 branche le cas concret des packs tiers SCSS.

## 10. Hors-scope (non-goals)

- SSR / réflexions sur l'eau ou le verre lointains.
- POM / parallax sur le lointain.
- Material-ID lointain (verre/métal) — non requis au palier « cohérence atmo + lumière ».
- Cascade d'ombre supplémentaire couvrant la plage LOD.
- Toute modification du mod Caldera (contrat vertex, format de mesh).

## 11. Validation

Tests en jeu sur le profil Modrinth « NeoForge 1.21.1 » (M1 Pro), avec Caldera + shader Camille
actif :

- Absence de couture proche↔lointain (albedo, normale, grading).
- Continuité du brouillard/atmosphère à la frontière render-distance↔LOD.
- Versants lointains qui accrochent le soleil de façon cohérente avec le proche.
- FPS avant/après (objectif : surcoût négligeable, cf. §7).
- Non-régression : **vanilla sans shader** → LOD identique à l'actuel.
- Phase 2 : un pack SCSS → terrain lointain shadé par le pack via auto-dérivation.
