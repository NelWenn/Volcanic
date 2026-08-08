package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProfileChipRowTest {
    private static final List<String> PROFILES = List.of("performance", "balanced", "quality", "ultra");
    private static final String CUSTOM = "custom";

    @Test
    void theMatchedProfileIsTheOnlyActiveChip() {
        List<ProfileChipRow.Chip> chips = ProfileChipRow.chips(PROFILES, CUSTOM, Optional.of("quality"));
        assertEquals(List.of("performance", "balanced", "quality", "ultra", "custom"),
                chips.stream().map(ProfileChipRow.Chip::key).toList());
        assertEquals(List.of(false, false, true, false, false),
                chips.stream().map(ProfileChipRow.Chip::active).toList());
    }

    @Test
    void customIsActiveOnlyWhenNothingMatches() {
        assertTrue(last(ProfileChipRow.chips(PROFILES, CUSTOM, Optional.empty())).active());
        assertFalse(last(ProfileChipRow.chips(PROFILES, CUSTOM, Optional.of("balanced"))).active());
        assertEquals(1, ProfileChipRow.chips(PROFILES, CUSTOM, Optional.empty()).stream()
                .filter(ProfileChipRow.Chip::active).count());
    }

    @Test
    void customIsNeverSelectableAndEveryProfileIs() {
        List<ProfileChipRow.Chip> chips = ProfileChipRow.chips(PROFILES, CUSTOM, Optional.empty());
        assertFalse(last(chips).selectable());
        assertEquals(List.of(true, true, true, true),
                chips.subList(0, PROFILES.size()).stream().map(ProfileChipRow.Chip::selectable).toList());
    }

    @Test
    void rejectsInputThatCouldNotBeShown() {
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.chips(null, CUSTOM, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.chips(PROFILES, null, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.chips(PROFILES, CUSTOM, null));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.chips(List.of(), CUSTOM, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.chips(PROFILES, "balanced", Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.chips(PROFILES, CUSTOM, Optional.of("nonesuch")));
    }

    @Test
    void chipsSitOnOneRowInOrderAndAreCentredInIt() {
        Rect row = new Rect(20, 100, 400, 27);
        List<Rect> boxes = ProfileChipRow.boxes(row, new int[]{30, 40});

        assertEquals(2, boxes.size());
        assertEquals(20, boxes.get(0).x());
        assertTrue(boxes.get(1).x() > boxes.get(0).right());
        for (Rect box : boxes) {
            assertEquals(row.y() + (row.height() - box.height()) / 2, box.y());
            assertTrue(box.y() >= row.y() && box.bottom() <= row.bottom(), "chip must stay inside the row");
        }
    }

    @Test
    void aRowShorterThanAChipStillPlacesItAtTheRowTop() {
        List<Rect> boxes = ProfileChipRow.boxes(new Rect(0, 50, 200, 4), new int[]{30});
        assertEquals(50, boxes.get(0).y());
    }

    @Test
    void anEmptyRowHasNoChips() {
        assertEquals(List.of(), ProfileChipRow.boxes(Rect.EMPTY, new int[]{30, 40}));
    }

    @Test
    void boxesRejectMissingInput() {
        assertThrows(IllegalArgumentException.class, () -> ProfileChipRow.boxes(null, new int[]{10}));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileChipRow.boxes(new Rect(0, 0, 100, 27), null));
    }

    private static ProfileChipRow.Chip last(List<ProfileChipRow.Chip> chips) {
        return chips.get(chips.size() - 1);
    }
}
