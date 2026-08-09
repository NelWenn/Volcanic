package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DetailsContentTest {
    private static final SettingId ID = SettingId.parse("vulkanmod:culling.occlusion");
    private static final RouteId ROUTE = RouteId.parse("rendering.culling");
    private static final String DESCRIPTION = "vulkanmod.options.occlusionCulling.tooltip";

    private static DetailsContent.Text text(int descriptionLines) {
        return (key, uppercase) -> {
            if (key == null || key.isBlank()) {
                return List.of();
            }
            if (key.equals(DESCRIPTION)) {
                return lines("desc", descriptionLines);
            }
            String value = key.startsWith("vulkanmod.impact.")
                    ? key.substring(key.lastIndexOf('.') + 1)
                    : key;
            return List.of(uppercase ? value.toUpperCase(java.util.Locale.ROOT) : value);
        };
    }

    private static List<String> lines(String prefix, int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> prefix + i).toList();
    }

    private static SettingMeta.Builder meta() {
        return new SettingMeta.Builder(ID, ROUTE, "title", SettingType.BOOL, SettingSource.VOLCANIC);
    }

    private static List<DetailsItem> items(SettingMeta meta, DetailsContent.Text text) {
        return DetailsContent.items(meta, null, text, true, Integer.MAX_VALUE, true);
    }

    private static long bars(List<DetailsItem> items) {
        return items.stream().filter(DetailsItem::isBar).count();
    }

    private static boolean mentions(List<DetailsItem> items, String key) {
        return items.stream().anyMatch(i -> key.equalsIgnoreCase(i.text()));
    }

    @Test
    void withoutAMetaItShowsOnlyTheEmptyLine() {
        List<DetailsItem> items = items(null, text(3));

        assertEquals(1, items.size());
        assertEquals(DetailsContent.KEY_EMPTY, items.get(0).text());
        assertEquals(ColorToken.TEXT_FAINT, items.get(0).token());
    }

    @Test
    void aBareSettingIsJustItsTitle() {
        List<DetailsItem> items = items(meta().build(), text(0));

        assertEquals(1, items.size());
        assertEquals("TITLE", items.get(0).text());
        assertEquals(ColorToken.TEXT_PRIMARY, items.get(0).token());
    }

    @Test
    void eachBlockIsSeparatedByExactlyOneSpacerAndNeverLeadsWithOne() {
        List<DetailsItem> items = items(meta().descriptionKey(DESCRIPTION)
                .performance(ImpactLevel.HIGH).build(), text(2));

        assertFalse(items.get(0).isBlank(), "the card must not open on a blank line");
        for (int index = 1; index < items.size(); index++) {
            assertFalse(items.get(index).isBlank() && items.get(index - 1).isBlank(),
                    "two spacers in a row at " + index);
        }
        assertEquals(2, items.stream().filter(DetailsItem::isBlank).count());
    }

    @Test
    void aRatedAxisDrawsLabelThenBarThenValue() {
        List<DetailsItem> items = items(meta().performance(ImpactLevel.HIGH).build(), text(0));

        int label = items.indexOf(items.stream()
                .filter(i -> DetailsContent.KEY_PERFORMANCE.equals(i.text())).findFirst().orElseThrow());
        assertTrue(items.get(label + 1).isBar());
        assertEquals(ImpactLevel.HIGH, items.get(label + 1).bar());
        assertTrue(items.get(label + 1).accentBar(), "the performance axis carries the accent fill");
        assertEquals("HIGH", items.get(label + 2).text());
        assertEquals(ColorToken.ACCENT, items.get(label + 2).token());
    }

    @Test
    void anUnratedAxisIsAbsentAndNoneDrawsNoBar() {
        List<DetailsItem> none = items(meta().visual(ImpactLevel.NONE).build(), text(0));
        assertEquals(0, bars(none));
        assertTrue(mentions(none, DetailsContent.KEY_VISUAL));
        assertTrue(mentions(none, "NONE"));

        List<DetailsItem> absent = items(meta().build(), text(0));
        assertFalse(mentions(absent, DetailsContent.KEY_VISUAL));
        assertFalse(mentions(absent, DetailsContent.KEY_PERFORMANCE));
    }

    @Test
    void theVisualAxisNeverBorrowsTheAccent() {
        List<DetailsItem> items = items(meta().visual(ImpactLevel.HIGH).build(), text(0));

        DetailsItem bar = items.stream().filter(DetailsItem::isBar).findFirst().orElseThrow();
        assertFalse(bar.accentBar());
    }

    @Test
    void flagsShareOneSpacerAndCarryTheirGlyph() {
        List<DetailsItem> items = items(meta().recommended(true).experimental(true)
                .scope(ApplyScope.RESTART).build(), text(0));

        assertEquals(1, items.stream().filter(DetailsItem::isBlank).count());
        assertEquals(DetailsItem.Glyph.CHECK, items.stream()
                .filter(i -> DetailsContent.KEY_RECOMMENDED.equals(i.text())).findFirst().orElseThrow().glyph());
        assertEquals(DetailsItem.Glyph.FLASK, items.stream()
                .filter(i -> DetailsContent.KEY_EXPERIMENTAL.equals(i.text())).findFirst().orElseThrow().glyph());
        assertTrue(mentions(items, DetailsContent.KEY_RESTART));
    }

    @Test
    void onlyRestartScopeRaisesTheRestartFlag() {
        assertFalse(mentions(items(meta().scope(ApplyScope.SWAPCHAIN).build(), text(0)),
                DetailsContent.KEY_RESTART));
    }

    @Test
    void truncatingTheDescriptionMarksTheCut() {
        List<DetailsItem> items = DetailsContent.items(meta().descriptionKey(DESCRIPTION).build(),
                null, text(5), true, 2, true);

        List<DetailsItem> described = items.stream()
                .filter(i -> i.text() != null && i.text().startsWith("desc")).toList();
        assertEquals(2, described.size());
        assertTrue(described.get(1).text().endsWith("…"), "the cut must be visible");
    }

    @Test
    void theDisabledReasonIsWarningColoured() {
        List<DetailsItem> items = DetailsContent.items(meta().build(), "reason.key", text(0),
                true, Integer.MAX_VALUE, true);

        assertEquals(ColorToken.WARNING, items.stream()
                .filter(i -> "reason.key".equals(i.text())).findFirst().orElseThrow().token());
    }

    @Test
    void fitDropsSpacersBeforeItDropsAnything() {
        SettingMeta meta = meta().descriptionKey(DESCRIPTION).performance(ImpactLevel.HIGH)
                .visual(ImpactLevel.LOW).recommended(true).build();
        DetailsContent.Text text = text(2);
        List<DetailsItem> full = items(meta, text);

        List<DetailsItem> fitted = DetailsContent.fit(meta, null, text, full.size() - 1);

        assertEquals(0, fitted.stream().filter(DetailsItem::isBlank).count());
        assertEquals(2, bars(fitted), "the bars survive losing the spacers");
        assertTrue(mentions(fitted, DetailsContent.KEY_RECOMMENDED));
    }

    @Test
    void fitSacrificesTheDescriptionBeforeTheBarsAndFlags() {
        SettingMeta meta = meta().descriptionKey(DESCRIPTION).performance(ImpactLevel.HIGH)
                .visual(ImpactLevel.LOW).recommended(true).build();
        DetailsContent.Text text = text(9);

        List<DetailsItem> fitted = DetailsContent.fit(meta, null, text, 10);

        assertTrue(fitted.size() <= 10);
        assertEquals(2, bars(fitted), "the impact bars are the point of the card");
        assertTrue(mentions(fitted, DetailsContent.KEY_RECOMMENDED));
        assertTrue(fitted.stream().filter(i -> i.text() != null && i.text().startsWith("desc")).count()
                < 9, "the description is what gives way");
    }

    @Test
    void fitDropsTheBarsOnlyWhenNothingElseIsLeftToGive() {
        SettingMeta meta = meta().performance(ImpactLevel.HIGH).visual(ImpactLevel.LOW).build();
        DetailsContent.Text text = text(0);

        assertEquals(2, bars(DetailsContent.fit(meta, null, text, 7)));
        assertEquals(0, bars(DetailsContent.fit(meta, null, text, 5)));
    }

    @Test
    void fitNeverExceedsTheCapacityAndIsEmptyWhenThereIsNone() {
        SettingMeta meta = meta().descriptionKey(DESCRIPTION).performance(ImpactLevel.HIGH)
                .visual(ImpactLevel.LOW).recommended(true).experimental(true).build();
        DetailsContent.Text text = text(12);

        for (int capacity = 0; capacity <= 40; capacity++) {
            assertTrue(DetailsContent.fit(meta, null, text, capacity).size() <= Math.max(0, capacity),
                    "overflowed at capacity " + capacity);
        }
        assertTrue(DetailsContent.fit(meta, null, text, 0).isEmpty());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> DetailsContent.items(meta().build(), null, null, true, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> DetailsContent.items(meta().build(), null, text(1), true, -1, true));
    }
}
