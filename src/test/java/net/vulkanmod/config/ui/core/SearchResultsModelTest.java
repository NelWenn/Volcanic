package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchResultsModelTest {
    private static final Rect CONTAINER = new Rect(150, 32, 400, 300);

    private static SearchIndex.Entry entry(String id, String title, SettingSource source) {
        return new SearchIndex.Entry(SettingId.parse(id), title, "", List.of(),
                RouteId.parse("rendering.culling"), source);
    }

    private static List<SettingSource> headers(SearchResultsModel model) {
        List<SettingSource> sources = new ArrayList<>();
        for (SearchResultsModel.Row row : model.rows()) {
            if (row instanceof SearchResultsModel.Header header) {
                sources.add(header.source());
            }
        }
        return sources;
    }

    private static List<String> titles(SearchResultsModel model) {
        List<String> titles = new ArrayList<>();
        for (SearchResultsModel.Row row : model.rows()) {
            if (row instanceof SearchResultsModel.Hit hit) {
                titles.add(hit.entry().title());
            }
        }
        return titles;
    }

    @Test
    void groupsFollowTheFixedSourceOrderNotTheRelevanceOrder() {
        SearchResultsModel model = SearchResultsModel.of(List.of(
                entry("mods:a", "A", SettingSource.MODS),
                entry("minecraft:b", "B", SettingSource.MINECRAFT),
                entry("vulkanmod:c", "C", SettingSource.VOLCANIC),
                entry("shaders:d", "D", SettingSource.SHADERS)), CONTAINER);

        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.MINECRAFT,
                SettingSource.SHADERS, SettingSource.MODS), headers(model));
        assertEquals(List.of("C", "B", "D", "A"), titles(model));
    }

    @Test
    void aSourceWithNoHitsProducesNoHeader() {
        SearchResultsModel model = SearchResultsModel.of(List.of(
                entry("vulkanmod:a", "A", SettingSource.VOLCANIC),
                entry("mods:b", "B", SettingSource.MODS)), CONTAINER);

        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.MODS), headers(model));
        assertEquals(4, model.rows().size());
    }

    @Test
    void theRankedOrderIsKeptInsideAGroup() {
        SearchResultsModel model = SearchResultsModel.of(List.of(
                entry("vulkanmod:a", "A", SettingSource.VOLCANIC),
                entry("mods:z", "Z", SettingSource.MODS),
                entry("vulkanmod:b", "B", SettingSource.VOLCANIC)), CONTAINER);

        assertEquals(List.of("A", "B", "Z"), titles(model));
    }

    @Test
    void theRowIndexAClickMapsToIsTheEntryTheUserSees() {
        SearchResultsModel model = SearchResultsModel.of(List.of(
                entry("vulkanmod:a", "A", SettingSource.VOLCANIC),
                entry("vulkanmod:b", "B", SettingSource.VOLCANIC),
                entry("mods:z", "Z", SettingSource.MODS)), CONTAINER);

        List<Rect> boxes = model.boxes();
        assertEquals(model.rows().size(), boxes.size());
        for (int index = 0; index < boxes.size(); index++) {
            Rect box = boxes.get(index);
            assertEquals(index, model.indexAt(box.x() + box.width() / 2, box.y() + box.height() / 2));
        }

        int hitIndex = model.indexAt(boxes.get(1).x(), boxes.get(1).y());
        assertEquals("A", model.hitAt(hitIndex).orElseThrow().title());
        assertEquals("A", ((SearchResultsModel.Hit) model.rows().get(hitIndex)).entry().title());
    }

    @Test
    void aClickOutsideEveryRowMapsToNoRow() {
        SearchResultsModel model = SearchResultsModel.of(
                List.of(entry("vulkanmod:a", "A", SettingSource.VOLCANIC)), CONTAINER);

        assertEquals(-1, model.indexAt(CONTAINER.x() - 1, CONTAINER.y() - 1));
        assertTrue(model.hitAt(-1).isEmpty());
    }

    @Test
    void boxesStackInsideThePanelWithoutOverlapping() {
        SearchResultsModel model = SearchResultsModel.of(List.of(
                entry("vulkanmod:a", "A", SettingSource.VOLCANIC),
                entry("mods:z", "Z", SettingSource.MODS)), CONTAINER);

        Rect panel = model.panel();
        assertFalse(panel.isEmpty());
        assertTrue(panel.x() >= CONTAINER.x() && panel.right() <= CONTAINER.right());
        assertTrue(panel.y() >= CONTAINER.y() && panel.bottom() <= CONTAINER.bottom());

        int previousBottom = panel.y();
        for (Rect box : model.boxes()) {
            assertTrue(box.x() >= panel.x() && box.right() <= panel.right(), "box escapes the panel: " + box);
            assertTrue(box.y() >= previousBottom, "boxes overlap at " + box);
            assertTrue(box.bottom() <= panel.bottom(), "box escapes the panel: " + box);
            previousBottom = box.bottom();
        }
    }

    @Test
    void aRowThatDoesNotFitIsNeitherPaintedNorClickable() {
        List<SearchIndex.Entry> entries = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            entries.add(entry("vulkanmod:" + index, "S" + index, SettingSource.VOLCANIC));
        }

        SearchResultsModel full = SearchResultsModel.of(entries, CONTAINER);
        assertEquals(13, full.rows().size());

        SearchResultsModel cropped = SearchResultsModel.of(entries, CONTAINER.withHeight(80));
        assertTrue(cropped.rows().size() < full.rows().size());
        assertEquals(cropped.rows().size(), cropped.boxes().size());
        for (Rect box : cropped.boxes()) {
            assertTrue(box.bottom() <= cropped.panel().bottom(), "box escapes the panel: " + box);
        }
    }

    @Test
    void aHeaderIsNeverLeftWithoutTheHitsItAnnounces() {
        List<SearchIndex.Entry> entries = List.of(
                entry("vulkanmod:a", "A", SettingSource.VOLCANIC),
                entry("vulkanmod:b", "B", SettingSource.VOLCANIC),
                entry("mods:y", "Y", SettingSource.MODS),
                entry("mods:z", "Z", SettingSource.MODS));

        for (int height = 0; height <= 200; height++) {
            SearchResultsModel model = SearchResultsModel.of(entries, CONTAINER.withHeight(height));
            List<SearchResultsModel.Row> rows = model.rows();
            assertFalse(!rows.isEmpty() && rows.get(rows.size() - 1) instanceof SearchResultsModel.Header,
                    "a trailing header survived at height " + height);
        }
    }

    @Test
    void arrowsMoveBetweenHitsSkippingHeadersAndStoppingAtTheEnds() {
        SearchResultsModel model = SearchResultsModel.of(List.of(
                entry("vulkanmod:a", "A", SettingSource.VOLCANIC),
                entry("vulkanmod:b", "B", SettingSource.VOLCANIC),
                entry("mods:y", "Y", SettingSource.MODS),
                entry("mods:z", "Z", SettingSource.MODS)), CONTAINER);

        assertEquals(6, model.rows().size());
        assertEquals(1, model.firstHit());

        assertEquals(1, model.nextHit(-1, 1));
        assertEquals(1, model.nextHit(0, 1));
        assertEquals(2, model.nextHit(1, 1));
        assertEquals(4, model.nextHit(2, 1));
        assertEquals(5, model.nextHit(4, 1));
        assertEquals(5, model.nextHit(5, 1));

        assertEquals(4, model.nextHit(5, -1));
        assertEquals(2, model.nextHit(4, -1));
        assertEquals(1, model.nextHit(2, -1));
        assertEquals(1, model.nextHit(1, -1));

        assertThrows(IllegalArgumentException.class, () -> model.nextHit(1, 0));
    }

    @Test
    void hitAtIgnoresHeadersAndIndexesThatAreNotThere() {
        SearchResultsModel model = SearchResultsModel.of(
                List.of(entry("vulkanmod:a", "A", SettingSource.VOLCANIC)), CONTAINER);

        assertTrue(model.hitAt(0).isEmpty());
        assertEquals("A", model.hitAt(1).orElseThrow().title());
        assertTrue(model.hitAt(2).isEmpty());
        assertTrue(model.hitAt(-1).isEmpty());
    }

    @Test
    void noResultsStillLeavesAPanelToPutTheMessageIn() {
        SearchResultsModel model = SearchResultsModel.of(List.of(), CONTAINER);

        assertFalse(model.panel().isEmpty());
        assertEquals(List.of(), model.rows());
        assertEquals(List.of(), model.boxes());
        assertEquals(-1, model.firstHit());
        assertEquals(-1, model.nextHit(-1, 1));
    }

    @Test
    void aContainerWithNoRoomOffersNoPanelAtAll() {
        SearchResultsModel model = SearchResultsModel.of(
                List.of(entry("vulkanmod:a", "A", SettingSource.VOLCANIC)), Rect.EMPTY);

        assertTrue(model.panel().isEmpty());
        assertEquals(List.of(), model.rows());
        assertEquals(List.of(), model.boxes());
        assertEquals(-1, model.indexAt(0, 0));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SearchResultsModel.of(null, CONTAINER));
        assertThrows(IllegalArgumentException.class,
                () -> SearchResultsModel.of(Arrays.asList((SearchIndex.Entry) null), CONTAINER));
        assertThrows(IllegalArgumentException.class,
                () -> SearchResultsModel.of(List.of(entry("vulkanmod:a", "A", SettingSource.VOLCANIC)), null));
    }
}
