package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SidebarModelTest {

    private static final RouteId OVERVIEW = RouteId.parse("overview");
    private static final RouteId RENDERING = RouteId.parse("rendering");
    private static final RouteId MODS = RouteId.parse("mods");

    private static NavTree tree() {
        return new NavTree.Builder()
                .add(new NavNode(OVERVIEW, "k.overview", "k.section.volcanic", true))
                .add(new NavNode(RENDERING, "k.rendering", null, true))
                .add(new NavNode(MODS, "k.mods", SidebarModel.SECTION_SYSTEM, true))
                .build();
    }

    private static SidebarModel model() {
        return new SidebarModel(tree());
    }

    @Test
    void theVisibleRangeBracketsExactlyTheEntriesTouchingTheViewport() {
        SidebarModel model = model();
        int viewport = 40;
        for (int scroll = 0; scroll <= model.maxScroll(viewport); scroll++) {
            int first = model.firstVisible(scroll);
            int last = model.lastVisible(scroll, viewport);
            for (int index = 0; index < model.entryCount(); index++) {
                int top = model.offsetOf(index);
                boolean touches = top + model.heightOf(index) > scroll && top < scroll + viewport;
                assertEquals(touches, index >= first && index <= last,
                        "entry " + index + " at scroll " + scroll);
            }
        }
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
        assertNull(model.entryAt(0));
    }

    @Test
    void constructorRejectsANullTree() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel(null));
    }

    @Test
    void anEntryRejectsWhatItCannotDraw() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Row(null, "k.title", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SidebarModel.Row(RouteId.parse("overview"), " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Section(null, false));
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel.Section("", false));
    }

    @Test
    void aCollapsedSectionKeepsItsHeaderAndDropsItsRows() {
        NavTree tree = tree();
        SidebarModel open = new SidebarModel(tree, java.util.Set.of());
        SidebarModel shut = new SidebarModel(tree, java.util.Set.of(SidebarModel.SECTION_SYSTEM));

        assertTrue(shut.entryCount() < open.entryCount(), "collapsing must remove rows");
        long headers = shut.entries().stream().filter(e -> e instanceof SidebarModel.Section).count();
        long openHeaders = open.entries().stream().filter(e -> e instanceof SidebarModel.Section).count();
        assertEquals(openHeaders, headers, "headers never disappear, only their rows");
    }

    @Test
    void theCollapsedFlagReachesTheHeaderItBelongsTo() {
        SidebarModel shut = new SidebarModel(tree(), java.util.Set.of(SidebarModel.SECTION_SYSTEM));
        for (SidebarModel.Entry entry : shut.entries()) {
            if (entry instanceof SidebarModel.Section section) {
                assertEquals(SidebarModel.SECTION_SYSTEM.equals(section.labelKey()), section.collapsed());
            }
        }
    }

    @Test
    void paintingAndHitTestingCannotDiverge() {
        for (java.util.Set<String> collapsed
                : List.of(java.util.Set.<String>of(), java.util.Set.of(SidebarModel.SECTION_SYSTEM))) {
            SidebarModel model = new SidebarModel(tree(), collapsed);
            for (int i = 0; i < model.entryCount(); i++) {
                assertEquals(i, model.entryIndexAt(model.offsetOf(i)),
                        "top edge of entry " + i + " with collapsed=" + collapsed);
                assertEquals(i, model.entryIndexAt(model.offsetOf(i) + model.heightOf(i) - 1),
                        "bottom edge of entry " + i + " with collapsed=" + collapsed);
            }
        }
    }

    @Test
    void scrollStaysInsideTheShorterListAfterCollapsing() {
        SidebarModel shut = new SidebarModel(tree(), java.util.Set.of(SidebarModel.SECTION_SYSTEM));
        int clamped = shut.clampScroll(9000, 60);

        assertTrue(clamped <= shut.maxScroll(60));
        assertTrue(shut.firstVisible(clamped) <= shut.lastVisible(clamped, 60),
                "a stale scroll must not blank the sidebar");
    }

    @Test
    void aFreshInstallCollapsesSystemAndAnEmptyListCollapsesNothing() {
        assertEquals(java.util.Set.of(SidebarModel.SECTION_SYSTEM),
                SidebarModel.collapsedOrDefault(null));
        assertEquals(java.util.Set.of(), SidebarModel.collapsedOrDefault(List.of()),
                "an empty stored list means the user expanded everything");
        assertEquals(java.util.Set.of("a", "b"), SidebarModel.collapsedOrDefault(List.of("a", "b")));
    }

    @Test
    void anIndexOutsideTheListYieldsNoEntry() {
        SidebarModel model = new SidebarModel(tree());
        assertNull(model.entryAt(-1));
        assertNull(model.entryAt(model.entryCount()));
        assertThrows(IllegalArgumentException.class, () -> new SidebarModel(tree(), null));
    }
}
