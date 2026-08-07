# Xaero's Minimap & World Map — compatibility notes

Volcanic runs Xaero's Minimap and Xaero's World Map on its native Vulkan/MoltenVK
renderer. Both mods render through OpenGL paths (offscreen framebuffers, texture
read-back, pixel-unpack buffers, radar-icon atlases) that the compatibility layer
now emulates.

## What works

- **Minimap**: terrain, correct north–south scrolling, and radar entity icons.
- **World Map**: real terrain at every zoom level, including the zoomed-out LOD
  ("branch") tiles, with no crash and no garbage/placeholder tiles.

## Existing worlds: clear the map cache once

Xaero caches rendered map tiles to disk. A world opened with an **older** Volcanic
build may contain tiles that were baked incorrectly by that build; they persist and
are re-loaded as-is. After updating, clear the world-map cache once so tiles
regenerate cleanly:

```
<game directory>/xaero/world-map/<world name>/
```

Delete that folder (or the whole `xaero/world-map/` directory) while the game is
closed. It is a regenerable cache — the map rebuilds as you explore. The minimap
cache (`xaero/minimap/`) does not need clearing.

## Known limitation

At **extreme zoom-out (below ~0.5x)** the World Map switches to Xaero's downscaled
LOD tiles. Those tiles carry no mipmaps (Xaero allocates them with
`GL_TEXTURE_MAX_LEVEL = 0`), so under heavy minification their edges can show a
slight seam / thin-feature aliasing at tile boundaries. The map remains fully
readable and correct; this is a minor cosmetic artifact confined to the deepest
zoom levels. Zoom levels at and above ~0.5x are unaffected.
