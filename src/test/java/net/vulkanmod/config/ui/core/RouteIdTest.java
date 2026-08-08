package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RouteIdTest {
    @Test
    void splitsIntoSegments() {
        assertEquals(List.of("rendering", "culling"), RouteId.parse("rendering.culling").segments());
    }

    @Test
    void rootHasNoSegments() {
        assertEquals(0, RouteId.root().depth());
        assertEquals("", RouteId.root().toString());
    }

    @Test
    void childAppendsSegment() {
        assertEquals(RouteId.parse("rendering.culling"), RouteId.parse("rendering").child("culling"));
    }

    @Test
    void childOfRootHasOneSegment() {
        assertEquals(RouteId.parse("rendering"), RouteId.root().child("rendering"));
    }

    @Test
    void parentDropsLastSegment() {
        assertEquals(RouteId.parse("rendering"), RouteId.parse("rendering.culling").parent());
        assertEquals(RouteId.root(), RouteId.parse("rendering").parent());
        assertEquals(RouteId.root(), RouteId.root().parent());
    }

    @Test
    void ancestryIsStrict() {
        RouteId parent = RouteId.parse("rendering");
        RouteId child = RouteId.parse("rendering.culling");
        assertTrue(parent.isAncestorOf(child));
        assertFalse(child.isAncestorOf(parent));
        assertFalse(parent.isAncestorOf(parent));
        assertFalse(RouteId.parse("render").isAncestorOf(child));
    }

    @Test
    void rootIsAncestorOfEverything() {
        assertTrue(RouteId.root().isAncestorOf(RouteId.parse("mods.create.rendering")));
    }

    @Test
    void rejectsEmptySegment() {
        assertThrows(IllegalArgumentException.class, () -> RouteId.parse("rendering..culling"));
        assertThrows(IllegalArgumentException.class, () -> RouteId.parse("rendering").child(""));
    }
}
