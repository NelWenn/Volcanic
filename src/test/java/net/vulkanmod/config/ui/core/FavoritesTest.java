package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FavoritesTest {
    private static final SettingId A = SettingId.parse("vulkanmod:culling.occlusion");
    private static final SettingId B = SettingId.parse("vulkanmod:culling.entity");
    private static final SettingId C = SettingId.parse("minecraft:display.vsync");

    @Test
    void nothingIsFavouritedToStart() {
        Favorites favorites = new Favorites();
        assertEquals(0, favorites.count());
        assertEquals(List.of(), favorites.ids());
        assertFalse(favorites.contains(A));
    }

    @Test
    void togglingAddsThenRemoves() {
        Favorites favorites = new Favorites();

        favorites.toggle(A);
        assertTrue(favorites.contains(A));
        assertEquals(1, favorites.count());

        favorites.toggle(A);
        assertFalse(favorites.contains(A));
        assertEquals(0, favorites.count());
    }

    @Test
    void togglingOneIdLeavesTheOthersAlone() {
        Favorites favorites = new Favorites();
        favorites.toggle(A);
        favorites.toggle(B);

        favorites.toggle(A);

        assertFalse(favorites.contains(A));
        assertTrue(favorites.contains(B));
        assertFalse(favorites.contains(C));
    }

    @Test
    void idsComeBackInInsertionOrder() {
        Favorites favorites = new Favorites();
        favorites.toggle(C);
        favorites.toggle(A);
        favorites.toggle(B);

        assertEquals(List.of(C, A, B), favorites.ids());
    }

    @Test
    void anIdAddedBackAgainGoesToTheEnd() {
        Favorites favorites = new Favorites();
        favorites.toggle(A);
        favorites.toggle(B);
        favorites.toggle(C);

        favorites.toggle(B);
        favorites.toggle(B);

        assertEquals(List.of(A, C, B), favorites.ids());
    }

    @Test
    void replaceAllDropsWhateverWasThere() {
        Favorites favorites = new Favorites();
        favorites.toggle(A);

        favorites.replaceAll(List.of(C, B));

        assertEquals(List.of(C, B), favorites.ids());
        assertFalse(favorites.contains(A));
    }

    @Test
    void replaceAllWithAnEmptyListClearsEverything() {
        Favorites favorites = new Favorites();
        favorites.toggle(A);

        favorites.replaceAll(List.of());

        assertEquals(0, favorites.count());
        assertEquals(List.of(), favorites.ids());
    }

    @Test
    void replaceAllKeepsTheFirstPositionOfARepeatedId() {
        Favorites favorites = new Favorites();

        favorites.replaceAll(List.of(A, B, A));

        assertEquals(List.of(A, B), favorites.ids());
        assertEquals(2, favorites.count());
    }

    @Test
    void replaceAllCopiesTheListItWasGiven() {
        Favorites favorites = new Favorites();
        List<SettingId> source = new ArrayList<>(List.of(A));

        favorites.replaceAll(source);
        source.add(B);

        assertEquals(List.of(A), favorites.ids());
    }

    @Test
    void aRejectedReplaceAllLeavesTheFavouritesIntact() {
        Favorites favorites = new Favorites();
        favorites.toggle(A);

        assertThrows(IllegalArgumentException.class, () -> favorites.replaceAll(Arrays.asList(B, null)));

        assertEquals(List.of(A), favorites.ids());
    }

    @Test
    void theReturnedListCannotEditTheFavourites() {
        Favorites favorites = new Favorites();
        favorites.toggle(A);
        List<SettingId> ids = favorites.ids();

        assertThrows(UnsupportedOperationException.class, () -> ids.add(B));

        assertEquals(1, favorites.count());
    }

    @Test
    void rejectsNullInput() {
        Favorites favorites = new Favorites();
        assertThrows(IllegalArgumentException.class, () -> favorites.toggle(null));
        assertThrows(IllegalArgumentException.class, () -> favorites.contains(null));
        assertThrows(IllegalArgumentException.class, () -> favorites.replaceAll(null));
        assertThrows(IllegalArgumentException.class, () -> favorites.replaceAll(Arrays.asList(A, null)));
    }
}
