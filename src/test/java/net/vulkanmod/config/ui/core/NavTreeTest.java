package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NavTreeTest {

    private static NavTree sample() {
        return new NavTree.Builder()
                .add(new NavNode(RouteId.parse("overview"), "k.overview", "k.section.volcanic", true))
                .add(new NavNode(RouteId.parse("rendering"), "k.rendering", null, true))
                .add(new NavNode(RouteId.parse("rendering.general"), "k.general", null, false))
                .add(new NavNode(RouteId.parse("rendering.culling"), "k.culling", null, false))
                .add(new NavNode(RouteId.parse("mods"), "k.mods", "k.section.content", true))
                .build();
    }

    @Test
    void findsNodesByRoute() {
        assertEquals("k.culling", sample().find(RouteId.parse("rendering.culling")).titleKey());
        assertNull(sample().find(RouteId.parse("nope")));
    }

    @Test
    void childrenAreDirectOnlyAndInInsertionOrder() {
        List<NavNode> children = sample().children(RouteId.parse("rendering"));
        assertEquals(2, children.size());
        assertEquals(RouteId.parse("rendering.general"), children.get(0).route());
        assertEquals(RouteId.parse("rendering.culling"), children.get(1).route());
    }

    @Test
    void aLeafHasNoChildren() {
        assertEquals(List.of(), sample().children(RouteId.parse("rendering.culling")));
    }

    @Test
    void rootChildrenAreTheTopLevelNodes() {
        assertEquals(3, sample().children(RouteId.root()).size());
    }

    @Test
    void sidebarRowsExcludeHiddenNodes() {
        List<NavNode> rows = sample().sidebarRows();
        assertEquals(3, rows.size());
        assertEquals(RouteId.parse("overview"), rows.get(0).route());
        assertEquals(RouteId.parse("mods"), rows.get(2).route());
    }

    @Test
    void defaultRouteIsTheFirstSidebarRow() {
        assertEquals(RouteId.parse("overview"), sample().defaultRoute());
    }

    @Test
    void containsAndSizeReflectEveryNode() {
        assertTrue(sample().contains(RouteId.parse("rendering.general")));
        assertFalse(sample().contains(RouteId.parse("rendering.nope")));
        assertEquals(5, sample().size());
    }

    @Test
    void duplicateRouteIsRejected() {
        NavTree.Builder builder = new NavTree.Builder()
                .add(new NavNode(RouteId.parse("a"), "k.a", null, true));
        assertThrows(IllegalArgumentException.class,
                () -> builder.add(new NavNode(RouteId.parse("a"), "k.a2", null, true)));
    }

    @Test
    void aNodeWhoseParentIsMissingIsRejected() {
        NavTree.Builder builder = new NavTree.Builder();
        assertThrows(IllegalArgumentException.class,
                () -> builder.add(new NavNode(RouteId.parse("a.b"), "k.b", null, false)));
    }

    @Test
    void rootRouteIsRejectedAsANode() {
        assertThrows(IllegalArgumentException.class,
                () -> new NavNode(RouteId.root(), "k", null, true));
    }

    @Test
    void anEmptyTreeRejectsDefaultRoute() {
        NavTree tree = new NavTree.Builder().build();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, tree::defaultRoute);
        assertTrue(thrown.getMessage().contains("sidebar rows"));
    }

    @Test
    void aTreeWithoutSidebarVisibleRowsRejectsDefaultRoute() {
        NavTree tree = new NavTree.Builder()
                .add(new NavNode(RouteId.parse("hidden"), "k.hidden", null, false))
                .build();
        assertEquals(1, tree.size());
        assertThrows(IllegalStateException.class, tree::defaultRoute);
    }
}
