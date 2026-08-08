package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SidebarModelTest {

    private static final RouteId OVERVIEW = RouteId.parse("overview");
    private static final RouteId RENDERING = RouteId.parse("rendering");
    private static final RouteId MODS = RouteId.parse("mods");

    private static SidebarModel model() {
        NavTree tree = new NavTree.Builder()
                .add(new NavNode(OVERVIEW, "k.overview", "k.section.volcanic", true))
                .add(new NavNode(RENDERING, "k.rendering", null, true))
                .add(new NavNode(MODS, "k.mods", "k.section.content", true))
                .build();
        return new SidebarModel(tree);
    }

    @Test
    void insertsASectionHeaderBeforeEachLabelledRow() {
        SidebarModel model = model();
        assertEquals(5, model.entryCount());
        assertInstanceOf(SidebarModel.Section.class, model.entries().get(0));
        assertInstanceOf(SidebarModel.Row.class, model.entries().get(1));
        assertInstanceOf(SidebarModel.Row.class, model.entries().get(2));
        assertInstanceOf(SidebarModel.Section.class, model.entries().get(3));
        assertInstanceOf(SidebarModel.Row.class, model.entries().get(4));
    }

    @Test
    void heightsMatchTheDesignSystem() {
        SidebarModel model = model();
        assertEquals(16, model.heightOf(0));
        assertEquals(25, model.heightOf(1));
    }

    @Test
    void offsetsAccumulate() {
        SidebarModel model = model();
        assertEquals(0, model.offsetOf(0));
        assertEquals(16, model.offsetOf(1));
        assertEquals(41, model.offsetOf(2));
        assertEquals(66, model.offsetOf(3));
        assertEquals(82, model.offsetOf(4));
        assertEquals(107, model.totalHeight());
    }

    @Test
    void routeAtResolvesRowsOnly() {
        SidebarModel model = model();
        assertNull(model.routeAt(0));
        assertEquals(OVERVIEW, model.routeAt(16));
        assertEquals(RENDERING, model.routeAt(41));
        assertNull(model.routeAt(66));
        assertEquals(MODS, model.routeAt(82));
    }

    @Test
    void routeAtReturnsNullOutsideTheContent() {
        SidebarModel model = model();
        assertNull(model.routeAt(-1));
        assertNull(model.routeAt(107));
    }

    @Test
    void indexOfRouteFindsRowsAndRejectsUnknowns() {
        SidebarModel model = model();
        assertEquals(1, model.indexOfRoute(OVERVIEW));
        assertEquals(4, model.indexOfRoute(MODS));
        assertEquals(-1, model.indexOfRoute(RouteId.parse("nope")));
    }

    @Test
    void scrollClampsToTheOverflow() {
        SidebarModel model = model();
        assertEquals(0, model.maxScroll(200));
        assertEquals(7, model.maxScroll(100));
        assertEquals(0, model.clampScroll(-5, 100));
        assertEquals(7, model.clampScroll(999, 100));
    }

    @Test
    void anEmptyTreeYieldsAnEmptySidebar() {
        SidebarModel model = new SidebarModel(new NavTree.Builder().build());
        assertEquals(0, model.entryCount());
        assertEquals(0, model.totalHeight());
        assertNull(model.routeAt(0));
    }

    @Test
    void constructorRejectsANullTree() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel(null));
    }

    @Test
    void indexOfRouteRejectsANullRoute() {
        SidebarModel model = model();
        assertThrows(IllegalArgumentException.class, () -> model.indexOfRoute(null));
    }

    @Test
    void rowRejectsANullRoute() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Row(null, "k.title"));
    }

    @Test
    void rowRejectsABlankTitleKey() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Row(RouteId.parse("overview"), " "));
    }

    @Test
    void sectionRejectsABlankLabelKey() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Section(null));
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Section(""));
    }
}
