package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.NavTree;
import net.vulkanmod.config.ui.core.RouteId;
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
        assertEquals(6, presenter.subTabs().size());
        assertEquals(RouteId.parse("rendering.general"), presenter.subTabs().get(0).route());
    }

    @Test
    void navigatingToASubTabKeepsTheSameSubTabs() {
        NavPresenter presenter = new NavPresenter();
        presenter.navigate(RouteId.parse("rendering.culling"));
        assertEquals(6, presenter.subTabs().size());
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
        assertEquals(6, presenter.focus().ring(NavPresenter.REGION_CONTENT).size());

        assertTrue(presenter.back());

        assertEquals(RouteId.parse("overview"), presenter.stack().current());
        assertEquals(0, presenter.subTabs().size());
        assertEquals(0, presenter.focus().ring(NavPresenter.REGION_CONTENT).size());
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
        assertEquals(40, keys.size());

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
        assertEquals(2, keys.size());

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
    void displayGeneralIsTheOnlyRouteWithRowsToday() {
        NavPresenter presenter = new NavPresenter();
        assertEquals(List.of(), presenter.settings());

        presenter.navigate(RouteId.parse("display.general"));
        assertEquals(5, presenter.settings().size());

        presenter.navigate(RouteId.parse("display.advanced"));
        assertEquals(List.of(), presenter.settings());
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
            topLevel.add(node.route());
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
}
