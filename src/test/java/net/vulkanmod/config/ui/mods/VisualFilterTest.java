package net.vulkanmod.config.ui.mods;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualFilterTest {

    @Test
    void aStrongKeywordInThePathIsVisual() {
        assertTrue(VisualFilter.isVisual("client.renderDistance", null, null));
        assertTrue(VisualFilter.isVisual("graphics.fogDensity", null, null));
        assertTrue(VisualFilter.isVisual("gui.hudPosition", null, null));
    }

    @Test
    void aStrongKeywordInTheTranslationKeyIsVisual() {
        assertTrue(VisualFilter.isVisual("misc.thing", null, "mymod.config.shaderQuality"));
    }

    @Test
    void aStrongKeywordInTheCommentAloneIsVisual() {
        assertTrue(VisualFilter.isVisual("beacon.beamAlpha", "Opacity of the beacon beam", null));
    }

    @Test
    void gameplayWordingIsNotVisual() {
        assertFalse(VisualFilter.isVisual("teleport.xpCost", "Whether teleporting costs experience", null));
        assertFalse(VisualFilter.isVisual("general.maxStackSize", null, null));
    }

    @Test
    void aWeakKeywordInACommentDoesNotMakeAGameplaySettingVisual() {
        assertFalse(VisualFilter.isVisual("combat.attackCooldown",
                "Ticks between attacks; also changes the swing animation", null));
        assertFalse(VisualFilter.isVisual("mobs.spawnRate", "Scale of the spawn rate per difficulty", null));
    }

    @Test
    void aWeakKeywordCountsOnlyAsTheHeadOfTheLeafName() {
        assertTrue(VisualFilter.isVisual("misc.blockAnimations", null, null));
        assertTrue(VisualFilter.isVisual("entities.entityDistance", null, null));
        assertFalse(VisualFilter.isVisual("waystones.distanceFromSpawn", null, null));
        assertFalse(VisualFilter.isVisual("misc.thing", null, "mymod.config.animationSpeed"));
    }

    @Test
    void matchingIsCaseInsensitiveAndPluralTolerant() {
        assertTrue(VisualFilter.isVisual("CLIENT.RENDER_DISTANCE", null, null));
        assertTrue(VisualFilter.isVisual("world.maxParticles", null, null));
        assertTrue(VisualFilter.isVisual("blocks.connectedTextures", null, null));
        assertTrue(VisualFilter.isVisual("effects.HUDScale", null, null));
    }

    @Test
    void wordsRunTogetherWithoutABoundaryDoNotMatch() {
        assertFalse(VisualFilter.isVisual("client.renderdistance", null, null));
        assertFalse(VisualFilter.isVisual("client.fpsdisplay", null, null));
    }

    @Test
    void missingCommentAndTranslationKeyAreTolerated() {
        assertFalse(VisualFilter.isVisual("a.b", null, null));
        assertFalse(VisualFilter.isVisual("a.b", "", ""));
    }

    @Test
    void aBlankPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VisualFilter.isVisual(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> VisualFilter.isVisual("  ", null, null));
    }

    @Test
    void tokensSplitOnCaseDigitsAndPunctuation() {
        assertEquals(List.of("render", "distance"), VisualFilter.tokens("renderDistance"));
        assertEquals(List.of("render", "distance"), VisualFilter.tokens("RENDER_DISTANCE"));
        assertEquals(List.of("hud", "scale"), VisualFilter.tokens("HUDScale"));
        assertEquals(List.of("fsr", "1", "sharpness"), VisualFilter.tokens("fsr1Sharpness"));
        assertEquals(List.of("client", "fog"), VisualFilter.tokens("client.fog"));
        assertEquals(List.of(), VisualFilter.tokens(null));
    }
}
