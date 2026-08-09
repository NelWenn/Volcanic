package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchIndexTest {
    private static SearchIndex.Entry entry(String id, String title, String description, RouteId route) {
        return new SearchIndex.Entry(SettingId.parse(id), title, description, List.of(),
                route, SettingSource.VOLCANIC);
    }

    private static SearchIndex.Entry entry(String id, String title, String description, List<String> choices,
                                           RouteId route) {
        return new SearchIndex.Entry(SettingId.parse(id), title, description, choices,
                route, SettingSource.VOLCANIC);
    }

    private static SearchIndex index() {
        return SearchIndex.of(List.of(
                entry("vulkanmod:culling.occlusion", "Occlusion Culling",
                        "Skips geometry hidden from the camera", RouteId.parse("rendering.culling")),
                entry("vulkanmod:culling.entity", "Entity Culling", "", RouteId.parse("rendering.culling")),
                entry("minecraft:display.vsync", "VSync", "Waits for the display refresh",
                        RouteId.parse("display.general"))));
    }

    private static List<String> titles(List<SearchIndex.Entry> hits) {
        return hits.stream().map(SearchIndex.Entry::title).toList();
    }

    @Test
    void findsBySubstringOfTheTitle() {
        List<SearchIndex.Entry> hits = index().search("culling", 10);
        assertEquals(2, hits.size());
        assertTrue(hits.stream().allMatch(e -> e.title().contains("Culling")));
    }

    @Test
    void aTitleMatchOutranksADescriptionMatch() {
        List<SearchIndex.Entry> hits = index().search("camera", 10);
        assertEquals(1, hits.size());
        assertEquals("Occlusion Culling", hits.get(0).title());

        List<SearchIndex.Entry> both = index().search("vsync", 10);
        assertEquals("VSync", both.get(0).title());
    }

    @Test
    void aPrefixMatchOutranksAMatchInTheMiddle() {
        List<SearchIndex.Entry> hits = index().search("entity", 10);
        assertEquals("Entity Culling", hits.get(0).title());
    }

    @Test
    void matchingIgnoresCaseAndSurroundingSpace() {
        assertEquals(2, index().search("  CULLING ", 10).size());
    }

    @Test
    void anEmptyOrBlankQueryReturnsNothingRatherThanEverything() {
        assertTrue(index().search("", 10).isEmpty());
        assertTrue(index().search("   ", 10).isEmpty());
    }

    @Test
    void aQueryThatMatchesNothingReturnsAnEmptyListNotNull() {
        assertNotNull(index().search("zzzz", 10));
        assertTrue(index().search("zzzz", 10).isEmpty());
    }

    @Test
    void theLimitCapsTheResults() {
        assertEquals(1, index().search("culling", 1).size());
    }

    @Test
    void resultsAreStableForEqualScores() {
        assertEquals(index().search("culling", 10), index().search("culling", 10));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SearchIndex.of(null));
        assertThrows(IllegalArgumentException.class, () -> index().search(null, 10));
        assertThrows(IllegalArgumentException.class, () -> index().search("a", 0));
    }

    @Test
    void equalRanksKeepInsertionOrder() {
        assertEquals(List.of("Occlusion Culling", "Entity Culling"), titles(index().search("culling", 10)));
    }

    @Test
    void aPrefixMatchOutranksASubstringMatchIndexedBeforeIt() {
        SearchIndex index = SearchIndex.of(List.of(
                entry("vulkanmod:display.adaptive", "Adaptive Sync", "", RouteId.parse("display.general")),
                entry("vulkanmod:display.interval", "Sync Interval", "", RouteId.parse("display.general"))));

        assertEquals(List.of("Sync Interval", "Adaptive Sync"), titles(index.search("sync", 10)));
    }

    @Test
    void theLimitKeepsTheBestRankedNotTheFirstIndexed() {
        SearchIndex index = SearchIndex.of(List.of(
                entry("vulkanmod:display.adaptive", "Adaptive Sync", "", RouteId.parse("display.general")),
                entry("vulkanmod:display.interval", "Sync Interval", "", RouteId.parse("display.general"))));

        assertEquals(List.of("Sync Interval"), titles(index.search("sync", 1)));
    }

    @Test
    void aChoiceValueOutranksTheDescription() {
        SearchIndex index = SearchIndex.of(List.of(
                entry("vulkanmod:render.distance", "Render Distance", "Fancy trees cost the most",
                        RouteId.parse("rendering.general")),
                entry("vulkanmod:render.graphics", "Graphics", "", List.of("Fast", "Fancy"),
                        RouteId.parse("rendering.general"))));

        assertEquals(List.of("Graphics", "Render Distance"), titles(index.search("fancy", 10)));
    }

    @Test
    void aTitleSubstringOutranksAChoiceValue() {
        SearchIndex index = SearchIndex.of(List.of(
                entry("vulkanmod:render.graphics", "Graphics", "", List.of("Fast", "Fancy"),
                        RouteId.parse("rendering.general")),
                entry("vulkanmod:render.leaves", "Ultra Fancy Leaves", "", RouteId.parse("rendering.general"))));

        assertEquals(List.of("Ultra Fancy Leaves", "Graphics"), titles(index.search("fancy", 10)));
    }

    @Test
    void anEntryMatchingTwiceIsListedOnce() {
        SearchIndex index = SearchIndex.of(List.of(
                entry("vulkanmod:culling.entity", "Entity Culling", "Skips entities hidden from the camera",
                        List.of("Entity"), RouteId.parse("rendering.culling"))));

        assertEquals(List.of("Entity Culling"), titles(index.search("entity", 10)));
    }

    @Test
    void theIndexIgnoresLaterChangesToTheListItWasBuiltFrom() {
        List<SearchIndex.Entry> entries = new ArrayList<>();
        entries.add(entry("vulkanmod:display.vsync", "VSync", "", RouteId.parse("display.general")));
        SearchIndex index = SearchIndex.of(entries);

        entries.clear();

        assertEquals(1, index.search("vsync", 10).size());
    }

    @Test
    void rejectsAnEntryThatCannotBeSearched() {
        assertThrows(IllegalArgumentException.class,
                () -> SearchIndex.of(new ArrayList<>(java.util.Collections.singletonList(null))));
        assertThrows(IllegalArgumentException.class, () -> new SearchIndex.Entry(
                SettingId.parse("vulkanmod:a.b"), " ", "", List.of(), RouteId.parse("a"), SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SearchIndex.Entry(
                SettingId.parse("vulkanmod:a.b"), "A", null, List.of(), RouteId.parse("a"), SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SearchIndex.Entry(
                SettingId.parse("vulkanmod:a.b"), "A", "", null, RouteId.parse("a"), SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SearchIndex.Entry(
                SettingId.parse("vulkanmod:a.b"), "A", "", List.of(), null, SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SearchIndex.Entry(
                SettingId.parse("vulkanmod:a.b"), "A", "", List.of(), RouteId.parse("a"), null));
        assertThrows(IllegalArgumentException.class, () -> index().search("a", -1));
    }
}
