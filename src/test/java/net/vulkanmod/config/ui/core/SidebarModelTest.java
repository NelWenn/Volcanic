package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
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

    private static final RouteId PLUGINS = RouteId.parse("plugins");

    private static SidebarModel withPlugins() {
        return new SidebarModel(new NavTree.Builder()
                .add(new NavNode(OVERVIEW, "k.overview", "k.section.volcanic", true))
                .add(new NavNode(PLUGINS, "k.plugins", null, true))
                .add(new NavNode(RouteId.parse("plugins.caldera"), "Caldera", null, true))
                .add(new NavNode(RouteId.parse("plugins.other"), "Other", null, true))
                .add(new NavNode(MODS, "k.mods", SidebarModel.SECTION_SYSTEM, true))
                .build());
    }

    @Test
    @DisplayName("the rail runs from the category down to its last child and ticks each one")
    void railTiesEachPluginToItsCategory() {
        SidebarModel model = withPlugins();
        Rect sidebar = new Rect(0, 0, 132, 400);
        SidebarModel.Rail rail = model.rail(sidebar, 0, PLUGINS);

        assertEquals(2, rail.ticks().size(), "one tick per plugin row");
        assertFalse(rail.stem().isEmpty());
        assertEquals(1, rail.stem().width(), "the stem is a hairline, not a bar");
        for (Rect tick : rail.ticks()) {
            assertTrue(tick.y() >= rail.stem().y() && tick.y() <= rail.stem().bottom(),
                    "a tick hangs off the stem");
            assertTrue(tick.x() >= rail.stem().x(), "a tick starts left of its stem");
        }
        assertEquals(rail.ticks().get(rail.ticks().size() - 1).y(), rail.stem().bottom(),
                "the stem must stop at the last child, never dangle past it");
    }

    @Test
    @DisplayName("the rail sits in the gutter, clear of the child card and its label")
    void railSitsInTheGutter() {
        Rect sidebar = new Rect(40, 0, 132, 400);
        SidebarModel model = withPlugins();
        SidebarModel.Rail rail = model.rail(sidebar, 0, PLUGINS);
        Rect childCard = SidebarModel.rowBox(sidebar, model.offsetOf(2), model.heightOf(2), 2);

        assertTrue(rail.stem().right() <= childCard.x(),
                "the stem would be drawn inside the child card");
        for (Rect tick : rail.ticks()) {
            assertTrue(tick.right() < childCard.x(), "a tick would touch the child card");
            assertTrue(tick.x() >= sidebar.x(), "a tick escapes the sidebar on the left");
        }
    }

    @Test
    @DisplayName("a child card is indented so the gutter exists at all")
    void childCardsAreIndented() {
        Rect sidebar = new Rect(0, 0, 132, 400);
        Rect parent = SidebarModel.rowBox(sidebar, 0, 25, 1);
        Rect child = SidebarModel.rowBox(sidebar, 25, 25, 2);

        assertEquals(SidebarModel.ROW_INDENT, child.x() - parent.x(),
                "a child card must step in by exactly one indent");
        assertEquals(parent.right(), child.right(), "both cards must end on the same right edge");
        assertTrue(child.x() > SidebarModel.STEM_X, "the rail would have no gutter to live in");
    }

    @Test
    @DisplayName("the rail is clipped to the sidebar rather than bleeding out when scrolled")
    void railIsClippedToTheSidebar() {
        SidebarModel model = withPlugins();
        Rect sidebar = new Rect(0, 100, 132, 60);
        SidebarModel.Rail rail = model.rail(sidebar, 0, PLUGINS);

        assertTrue(rail.stem().isEmpty() || rail.stem().y() >= sidebar.y(), "stem starts above the sidebar");
        assertTrue(rail.stem().isEmpty() || rail.stem().bottom() <= sidebar.bottom(),
                "stem runs past the sidebar");
        for (Rect tick : rail.ticks()) {
            assertTrue(tick.y() >= sidebar.y() && tick.y() < sidebar.bottom(), "a tick escaped the sidebar");
        }
    }

    @Test
    @DisplayName("a category with no children, or no category at all, draws nothing")
    void railIsEmptyWithoutChildren() {
        SidebarModel.Rail none = model().rail(new Rect(0, 0, 132, 400), 0, PLUGINS);
        assertTrue(none.stem().isEmpty());
        assertTrue(none.ticks().isEmpty());

        SidebarModel.Rail childless = withPlugins().rail(new Rect(0, 0, 132, 400), 0, OVERVIEW);
        assertTrue(childless.stem().isEmpty());
        assertTrue(childless.ticks().isEmpty());
    }

    @Test
    @DisplayName("the rail rejects a null sidebar or category rather than painting nowhere")
    void railRejectsNulls() {
        SidebarModel model = withPlugins();
        assertThrows(IllegalArgumentException.class, () -> model.rail(null, 0, PLUGINS));
        assertThrows(IllegalArgumentException.class, () -> model.rail(new Rect(0, 0, 10, 10), 0, null));
    }
}
