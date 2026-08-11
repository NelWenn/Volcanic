package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.ApplyBarModel;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.FocusRing;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.NavTree;
import net.vulkanmod.config.ui.core.PendingChanges;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.settings.SettingsDefinitions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NavPresenterTest {
    private static final Path LANG = Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json");
    private static final Pattern ENTRY = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"(.*)\"\\s*,?\\s*$");

    @Test
    void everyTopLevelRouteIsASidebarRow() {
        NavPresenter presenter = new NavPresenter();
        assertEquals(11, presenter.tree().sidebarRows().size());
        assertSidebarRowsAreExactlyTheTopLevelRoutes(presenter.tree());
    }

    @Test
    void aTopLevelRouteMissingFromTheSidebarIsCaught() {
        NavTree tree = new NavTree.Builder()
                .add(new NavNode(RouteId.parse("shown"), "key.shown", null, true))
                .add(new NavNode(RouteId.parse("hidden"), "key.hidden", null, false))
                .build();
        assertThrows(AssertionError.class, () -> assertSidebarRowsAreExactlyTheTopLevelRoutes(tree));
    }

    @Test
    void everyReadableModGetsAPageUnderMods() {
        NavTree tree = NavPresenter.buildTree(List.of("jade", "create"), List.of());

        List<RouteId> pages = new ArrayList<>();
        for (NavNode node : tree.children(RouteId.parse("mods"))) {
            pages.add(node.route());
            assertFalse(node.sidebarVisible(), "a mod page is not a sidebar row: " + node.route());
        }
        assertEquals(List.of(RouteId.parse("mods.jade"), RouteId.parse("mods.create")), pages);
        assertEquals(11, tree.sidebarRows().size());
        assertFalse(tree.contains(RouteId.parse("jade")), "a mod must not become a top-level category");
    }

    @Test
    void withoutModsTheModsPageKeepsNoChildren() {
        assertEquals(List.of(), NavPresenter.buildTree(List.of(), List.of()).children(RouteId.parse("mods")));
        assertThrows(IllegalArgumentException.class, () -> NavPresenter.buildTree(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> NavPresenter.buildTree(List.of(), null));
    }

    @Test
    void aModPageIsNamedAfterItsModWhenTheLoaderCannotBeAsked() {
        NavTree tree = NavPresenter.buildTree(List.of("jade"), List.of());
        assertEquals("jade", tree.find(RouteId.parse("mods.jade")).titleKey());
    }

    @Test
    void aModWeCannotReadStillGetsAPageAfterTheReadableOnes() {
        NavTree tree = NavPresenter.buildTree(List.of("jade"), List.of("waystones"));

        List<RouteId> pages = new ArrayList<>();
        for (NavNode node : tree.children(RouteId.parse("mods"))) {
            pages.add(node.route());
            assertFalse(node.sidebarVisible(), "a mod page is not a sidebar row: " + node.route());
        }
        assertEquals(List.of(RouteId.parse("mods.jade"), RouteId.parse("mods.waystones")), pages);
        assertEquals(11, tree.sidebarRows().size());
    }

    @Test
    void onlyThePageOfAModWeCannotReadOffersItsOwnScreen() {
        List<String> screenOnly = List.of("waystones");

        assertEquals("waystones",
                NavPresenter.modScreenOf(RouteId.parse("mods.waystones"), screenOnly).orElse(null));
        assertTrue(NavPresenter.modScreenOf(RouteId.parse("mods.jade"), screenOnly).isEmpty());
        assertTrue(NavPresenter.modScreenOf(RouteId.parse("mods"), screenOnly).isEmpty());
        assertTrue(NavPresenter.modScreenOf(RouteId.parse("waystones"), screenOnly).isEmpty());
        assertTrue(NavPresenter.modScreenOf(RouteId.parse("rendering.general"), screenOnly).isEmpty());
        assertTrue(NavPresenter.modScreenOf(RouteId.parse("mods.waystones"), List.of()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> NavPresenter.modScreenOf(null, screenOnly));
        assertThrows(IllegalArgumentException.class,
                () -> NavPresenter.modScreenOf(RouteId.parse("mods.waystones"), null));
    }

    @Test
    void startsOnTheFirstSidebarRow() {
        NavPresenter presenter = new NavPresenter();
        assertEquals(RouteId.parse("overview"), presenter.stack().current());
    }

    @Test
    void navigatingToAParentDescendsToItsFirstLeaf() {
        NavPresenter presenter = new NavPresenter();
        assertTrue(presenter.navigate(RouteId.parse("rendering")));
        assertEquals(RouteId.parse("rendering.general"), presenter.stack().current());

        assertTrue(presenter.navigate(RouteId.parse("display")));
        assertEquals(RouteId.parse("display.general"), presenter.stack().current());
    }

    @Test
    void navigatingToALeafStaysOnThatLeaf() {
        NavPresenter presenter = new NavPresenter();
        assertTrue(presenter.navigate(RouteId.parse("rendering.culling")));
        assertEquals(RouteId.parse("rendering.culling"), presenter.stack().current());
    }

    @Test
    void renderingHasItsSubTabs() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("rendering"));
        assertEquals(4, presenter.subTabs().size());
        assertEquals(RouteId.parse("rendering.general"), presenter.subTabs().get(0).route());
    }

    @Test
    void navigatingToASubTabKeepsTheSameSubTabs() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("rendering.culling"));
        assertEquals(4, presenter.subTabs().size());
    }

    @Test
    void aRouteWithNoChildrenHasNoSubTabs() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("experimental"));
        assertEquals(0, presenter.subTabs().size());
    }

    @Test
    void backReturnsToThePreviousLeafAndRebuildsTheContentRing() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("rendering"));
        assertEquals(4, presenter.subTabs().size());
        assertEquals(5, presenter.settings().size());
        assertEquals(10, presenter.focus().ring(NavPresenter.REGION_CONTENT).size(),
                "four tabs, five settings, plus the favourites button");

        assertTrue(presenter.back());

        assertEquals(RouteId.parse("overview"), presenter.stack().current());
        assertEquals(0, presenter.subTabs().size());
        assertEquals(1, presenter.focus().ring(NavPresenter.REGION_CONTENT).size(),
                "the favourites button is always reachable by keyboard");
    }

    @Test
    void backOnTheOpeningPageDoesNothing() {
        NavPresenter presenter = new NavPresenter();
        assertFalse(presenter.back());
        assertEquals(RouteId.parse("overview"), presenter.stack().current());
    }

    @Test
    void focusedRouteReadsTheActiveRegion() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("rendering"));

        assertNull(presenter.focusedRoute());

        presenter.focus().focusRegion(NavPresenter.REGION_SIDEBAR);
        assertTrue(presenter.focus().ring(NavPresenter.REGION_SIDEBAR).focus("rendering"));
        assertEquals(RouteId.parse("rendering"), presenter.focusedRoute());

        presenter.focus().focusRegion(NavPresenter.REGION_CONTENT);
        assertTrue(presenter.focus().ring(NavPresenter.REGION_CONTENT).focus("rendering.culling"));
        assertEquals(RouteId.parse("rendering.culling"), presenter.focusedRoute());
    }

    @Test
    void everyNavigationKeyResolvesInEnUs() throws IOException {
        Map<String, String> lang = readLang();
        List<String> keys = navigationKeys(new NavPresenter().tree());
        assertEquals(36, keys.size());

        List<String> unresolved = new ArrayList<>();
        for (String key : keys) {
            String value = lang.get(key);
            if (value == null || value.isBlank()) {
                unresolved.add(key);
            }
        }
        assertEquals(List.of(), unresolved);
    }

    @Test
    void everySettingTitleKeyWeOwnResolvesInEnUs() throws IOException {
        Map<String, String> lang = readLang();
        List<String> keys = SettingsDefinitions.displayGeneral().stream()
                .map(SettingMeta::titleKey)
                .filter(key -> key.startsWith("vulkanmod."))
                .toList();
        assertEquals(3, keys.size());

        List<String> unresolved = new ArrayList<>();
        for (String key : keys) {
            String value = lang.get(key);
            if (value == null || value.isBlank()) {
                unresolved.add(key);
            }
        }
        assertEquals(List.of(), unresolved);
    }

    @Test
    void everyApplyBarKeyAScopeCanProduceResolvesInEnUs() throws IOException {
        Map<String, String> lang = readLang();
        SettingId probe = SettingId.parse("vulkanmod:probe");

        List<String> unusable = new ArrayList<>();
        for (ApplyScope scope : ApplyScope.values()) {
            if (scope == ApplyScope.INSTANT) {
                continue;
            }
            PendingChanges pending = new PendingChanges();
            pending.mark(probe, scope);
            ApplyBarModel bar = ApplyBarModel.of(pending);
            assertTrue(bar.visible(), scope + " must be announced");

            String message = lang.get(bar.messageKey());
            if (message == null || message.isBlank() || !message.contains("%d")) {
                unusable.add(bar.messageKey());
            }
        }
        assertEquals(List.of(), unusable);
    }

    @Test
    void theBarOffersApplyAndDiscardAndWarnsThatLeavingDiscards() throws IOException {
        Map<String, String> lang = readLang();
        assertFalse(lang.getOrDefault("vulkanmod.applybar.apply", "").isBlank());
        assertFalse(lang.getOrDefault("vulkanmod.applybar.discard", "").isBlank());

        SettingId probe = SettingId.parse("vulkanmod:probe");
        List<String> silent = new ArrayList<>();
        for (ApplyScope scope : ApplyScope.values()) {
            if (scope == ApplyScope.INSTANT) {
                continue;
            }
            PendingChanges pending = new PendingChanges();
            pending.mark(probe, scope);
            String message = lang.getOrDefault(ApplyBarModel.of(pending).messageKey(), "");
            if (!message.contains("Esc")) {
                silent.add(scope.name());
            }
        }
        assertEquals(List.of(), silent, "every held change must warn that leaving discards it");
    }

    @Test
    void theContentRingHoldsTheSubTabsAndThenTheRowsOfTheCurrentRoute() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("display.general"));

        FocusRing ring = presenter.focus().ring(NavPresenter.REGION_CONTENT);
        assertEquals(3, presenter.subTabs().size());
        assertEquals(6, presenter.settings().size());
        assertEquals(10, ring.size(), "three tabs, six rows, plus the favourites button");
        for (SettingMeta meta : presenter.settings()) {
            assertTrue(ring.focus(meta.id().toString()), "row not focusable: " + meta.id());
        }

        presenter.navigate(RouteId.parse("display.volcanic"));
        assertEquals(8, ring.size(), "three tabs, four rows, plus the favourites button");
    }

    @Test
    void theFocusedSettingIsOnlyReadFromTheContentRegion() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("display.general"));
        presenter.focus().focusRegion(NavPresenter.REGION_CONTENT);
        assertTrue(presenter.focus().ring(NavPresenter.REGION_CONTENT)
                .focus(SettingsDefinitions.VSYNC.toString()));

        assertEquals(SettingsDefinitions.VSYNC, presenter.focusedSetting().id());
        assertNull(presenter.focusedRoute());

        presenter.focus().focusRegion(NavPresenter.REGION_SIDEBAR);
        assertNull(presenter.focusedSetting());
    }

    @Test
    void aFocusedSubTabIsARouteAndNotASetting() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("display.general"));
        presenter.focus().focusRegion(NavPresenter.REGION_CONTENT);
        assertTrue(presenter.focus().ring(NavPresenter.REGION_CONTENT).focus("display.interface"));

        assertEquals(RouteId.parse("display.interface"), presenter.focusedRoute());
        assertNull(presenter.focusedSetting());
    }

    @Test
    void revealingASettingLandsOnItsPageWithThatRowFocused() {
        NavPresenter presenter = new NavPresenter();

        assertTrue(presenter.reveal(SettingsDefinitions.VSYNC));

        assertEquals(RouteId.parse("display.general"), presenter.stack().current());
        assertEquals(NavPresenter.REGION_CONTENT, presenter.focus().activeRegion());
        assertEquals(SettingsDefinitions.VSYNC, presenter.focusedSetting().id());
        assertThrows(IllegalArgumentException.class, () -> presenter.reveal(null));
    }

    @Test
    void revealingASettingOnThePageAlreadyShownStillMovesTheFocusToIt() {
        NavPresenter presenter = new NavPresenter();
        presenter.reveal(SettingsDefinitions.VSYNC);

        assertTrue(presenter.reveal(SettingsDefinitions.FRAMERATE_LIMIT));

        assertEquals(RouteId.parse("display.general"), presenter.stack().current());
        assertEquals(SettingsDefinitions.FRAMERATE_LIMIT, presenter.focusedSetting().id());
    }

    @Test
    void eachDisplayRouteServesItsOwnRows() {
        NavPresenter presenter = new NavPresenter();
        assertEquals(List.of(), presenter.settings());

        presenter.navigate(RouteId.parse("display.general"));
        assertEquals(6, presenter.settings().size());

        presenter.navigate(RouteId.parse("display.interface"));
        assertEquals(6, presenter.settings().size());

        presenter.navigate(RouteId.parse("display.volcanic"));
        assertEquals(4, presenter.settings().size());
    }

    @Test
    void cyclingWrapsAndStartsAtTheFirstChoice() {
        List<String> choices = List.of("Windowed", "Windowed Fullscreen", "Exclusive Fullscreen");
        assertEquals("Windowed Fullscreen", NavPresenter.cycled(choices, "Windowed"));
        assertEquals("Windowed", NavPresenter.cycled(choices, "Exclusive Fullscreen"));
        assertEquals("Windowed", NavPresenter.cycled(choices, "unknown"));
        assertThrows(IllegalArgumentException.class, () -> NavPresenter.cycled(List.of(), "Windowed"));
    }

    @Test
    void theLangReaderReportsOnlyKeysThatArePresent() throws IOException {
        Map<String, String> lang = readLang();
        assertEquals("Overview", lang.get("vulkanmod.ui.page.overview"));
        assertEquals("Volcanic", lang.get("vulkanmod.ui.section.volcanic"));
        assertNull(lang.get("vulkanmod.ui.page.overview.absent"));
    }

    private static void assertSidebarRowsAreExactlyTheTopLevelRoutes(NavTree tree) {
        List<RouteId> topLevel = new ArrayList<>();
        for (NavNode node : tree.children(RouteId.root())) {
            if (!node.route().equals(NavPresenter.FAVORITES)) {
                topLevel.add(node.route());
            }
        }
        List<RouteId> rows = new ArrayList<>();
        for (NavNode node : tree.sidebarRows()) {
            rows.add(node.route());
        }
        assertFalse(topLevel.isEmpty());
        assertEquals(topLevel, rows);
    }

    private static List<String> navigationKeys(NavTree tree) {
        List<String> keys = new ArrayList<>();
        collectKeys(tree, tree.sidebarRows(), keys);
        return keys;
    }

    private static void collectKeys(NavTree tree, List<NavNode> nodes, List<String> keys) {
        for (NavNode node : nodes) {
            if (node.sectionKey() != null) {
                keys.add(node.sectionKey());
            }
            keys.add(node.titleKey());
            collectKeys(tree, tree.children(node.route()), keys);
        }
    }

    private static Map<String, String> readLang() throws IOException {
        assertTrue(Files.isRegularFile(LANG), "lang file missing at " + LANG.toAbsolutePath());
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : Files.readAllLines(LANG)) {
            Matcher matcher = ENTRY.matcher(line);
            if (matcher.matches()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
        }
        assertFalse(entries.isEmpty(), "no entries parsed from " + LANG.toAbsolutePath());
        return entries;
    }

    @Test
    void favouritesLeavesTheSidebarWithoutLeavingTheTree() {
        NavTree tree = new NavPresenter().tree();

        boolean listed = tree.sidebarRows().stream()
                .anyMatch(node -> node.route().equals(NavPresenter.FAVORITES));
        assertFalse(listed, "favourites moved to the header button");

        assertNotNull(tree.find(NavPresenter.FAVORITES),
                "it must stay in the tree or NavStack.navigate would refuse the route");
    }

    @Test
    void theFavouritesButtonIsReachableByKeyboardFromEveryPage() {
        NavPresenter presenter = new NavPresenter();
        for (RouteId route : List.of(RouteId.parse("rendering"), RouteId.parse("quality"),
                RouteId.parse("display"))) {
            presenter.navigate(route);
            assertTrue(presenter.focus().ring(NavPresenter.REGION_CONTENT)
                            .focus(NavPresenter.FAVORITES_FOCUS),
                    "missing from " + route);
        }
    }

    @Test
    void aPluginPageIsASidebarRowAndItsGroupsAreSubTabs() {
        NavTree tree = NavPresenter.buildTree(List.of(), List.of(),
                List.of(new NavPresenter.PluginPage("caldera", "Caldera",
                        List.of("terrain", "quality"), true, false)));

        assertNotNull(tree.find(RouteId.parse("plugins.caldera")));
        assertTrue(tree.find(RouteId.parse("plugins.caldera")).sidebarVisible(),
                "a plugin is a sidebar row, so it is reachable in one click");
        assertFalse(tree.find(RouteId.parse("plugins.caldera.terrain")).sidebarVisible(),
                "its groups are sub-tabs, not sidebar rows");

        boolean listed = tree.sidebarRows().stream()
                .anyMatch(node -> node.route().equals(RouteId.parse("plugins.caldera")));
        assertTrue(listed);
        boolean groupListed = tree.sidebarRows().stream()
                .anyMatch(node -> node.route().equals(RouteId.parse("plugins.caldera.terrain")));
        assertFalse(groupListed, "depth three never becomes a row");
    }

    @Test
    void clickingPluginsOpensTheManagementPageAndNotTheFirstPlugin() {
        NavTree tree = NavPresenter.buildTree(List.of(), List.of(),
                List.of(new NavPresenter.PluginPage("caldera", "Caldera", List.of("terrain"), true, false)));
        NavPresenter presenter = new NavPresenter();

        assertTrue(presenter.navigate(RouteId.parse("plugins")));
        assertEquals(RouteId.parse("plugins"), presenter.stack().current(),
                "descending would make the management page unreachable");
    }

    @Test
    void aPluginGroupClimbsToItsPluginAndAModPageStillClimbsToMods() {
        NavPresenter presenter = new NavPresenter();

        presenter.navigate(RouteId.parse("rendering.culling"));
        assertEquals(RouteId.parse("rendering"), presenter.activeSidebarRoute(),
                "an ordinary sub-page still climbs to its category");
    }

    @Test
    void aDisabledPluginStaysInTheTreeSoItsPageKeepsOpening() {
        NavTree tree = NavPresenter.buildTree(List.of(), List.of(),
                List.of(new NavPresenter.PluginPage("off", "Switched Off", List.of(), false, false)));

        assertNotNull(tree.find(RouteId.parse("plugins.off")),
                "disabled means greyed, never removed");
    }
}
