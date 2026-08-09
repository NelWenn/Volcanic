package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShellLayoutTest {
    @Test
    void breakpointsMatchTheDesignSystem() {
        assertEquals(Breakpoint.COMPACT, Breakpoint.forWidth(480));
        assertEquals(Breakpoint.COMPACT, Breakpoint.forWidth(519));
        assertEquals(Breakpoint.MEDIUM, Breakpoint.forWidth(520));
        assertEquals(Breakpoint.MEDIUM, Breakpoint.forWidth(640));
        assertEquals(Breakpoint.MEDIUM, Breakpoint.forWidth(800));
        assertEquals(Breakpoint.WIDE, Breakpoint.forWidth(801));
        assertEquals(Breakpoint.WIDE, Breakpoint.forWidth(854));
    }

    @Test
    void theScreenBoxCoversEveryRegionOfTheShell() {
        ShellLayout layout = ShellLayout.of(854, 480);

        assertEquals(new Rect(0, 0, 854, 480), layout.screen());
        assertEquals(layout.bottomBar().bottom(), layout.screen().bottom());
        assertEquals(layout.details().right(), layout.screen().right());
    }

    @Test
    void wideLayoutHasThreeColumns() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertTrue(layout.hasDetailsPanel());
        assertEquals(ShellLayout.SIDEBAR_WIDTH, layout.sidebar().width());
        assertEquals(196, layout.details().width());
        assertEquals(854 - ShellLayout.SIDEBAR_WIDTH - ShellLayout.DETAILS_WIDTH, layout.content().width());
    }

    @Test
    void mediumLayoutDropsTheDetailsPanel() {
        ShellLayout layout = ShellLayout.of(640, 360);
        assertFalse(layout.hasDetailsPanel());
        assertTrue(layout.details().isEmpty());
        assertEquals(ShellLayout.SIDEBAR_WIDTH, layout.sidebar().width());
        assertEquals(640 - ShellLayout.SIDEBAR_WIDTH, layout.content().width());
    }

    @Test
    void compactLayoutDropsTheSidebarAndGivesTheWidthToContent() {
        ShellLayout layout = ShellLayout.of(480, 270);
        assertFalse(layout.hasDetailsPanel());
        assertTrue(layout.hasDrawer());
        assertTrue(layout.sidebar().isEmpty());
        assertEquals(480, layout.content().width());
        assertEquals(0, layout.content().x());
    }

    @Test
    void compactDrawerOverlaysTheContentAtFullSidebarWidth() {
        ShellLayout layout = ShellLayout.of(480, 270);
        Rect drawer = layout.drawer();
        assertEquals(ShellLayout.SIDEBAR_WIDTH, drawer.width());
        assertEquals(layout.content().x(), drawer.x());
        assertEquals(layout.content().y() + ShellLayout.SIDEBAR_TOP_PAD, drawer.y(),
                "the drawer gets the same top margin as the sidebar it replaces");
        assertEquals(layout.content().bottom(), drawer.bottom());
    }

    @Test
    void widerLayoutsHaveNoDrawerAndNoMenuButton() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertFalse(layout.hasDrawer());
        assertTrue(layout.drawer().isEmpty());
        assertTrue(layout.menuButton().isEmpty());
        assertEquals(ShellLayout.SIDEBAR_WIDTH, layout.sidebar().width());
    }

    @Test
    void menuButtonSitsInsideTheCompactTopBar() {
        ShellLayout layout = ShellLayout.of(480, 270);
        Rect button = layout.menuButton();
        assertFalse(button.isEmpty());
        assertTrue(button.y() >= layout.topBar().y());
        assertTrue(button.bottom() <= layout.topBar().bottom());
        assertTrue(button.right() <= layout.topBar().right());
    }

    @Test
    void sidebarOrDrawerFollowsTheDrawerStateOnlyWhenCompact() {
        ShellLayout compact = ShellLayout.of(480, 270);
        assertTrue(compact.sidebarOrDrawer(false).isEmpty());
        assertEquals(compact.drawer(), compact.sidebarOrDrawer(true));

        ShellLayout medium = ShellLayout.of(700, 400);
        assertEquals(medium.sidebar(), medium.sidebarOrDrawer(false));
        assertEquals(medium.sidebar(), medium.sidebarOrDrawer(true));
    }

    @Test
    void theTopBarReservesItsHeightAndTheApplyBarOverlays() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertEquals(new Rect(0, 0, 854, 32), layout.topBar());
        assertEquals(new Rect(0, 480 - 34, 854, 34), layout.bottomBar());
        assertEquals(32 + ShellLayout.SIDEBAR_TOP_PAD, layout.sidebar().y());
        assertEquals(480 - 32 - ShellLayout.SIDEBAR_TOP_PAD, layout.sidebar().height(),
                "the body runs to the bottom so an absent apply bar costs nothing");
        assertEquals(layout.content().bottom(), layout.sidebar().bottom(),
                "the pad is taken off the top, never off the bottom");
        assertEquals(layout.content().bottom(), layout.bottomBar().bottom());
        assertEquals(34, layout.overlayReserve());
    }

    @Test
    void theApplyBarOverlapsTheBodyRatherThanShorteningIt() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertTrue(layout.bottomBar().y() < layout.content().bottom(),
                "the bar sits over the content, so showing it must not move anything");
        assertTrue(layout.bottomBar().y() > layout.content().y());
    }

    @Test
    void columnsTileWithoutGapOrOverlap() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertEquals(layout.sidebar().right(), layout.content().x());
        assertEquals(layout.content().right(), layout.details().x());
        assertEquals(854, layout.details().right());
    }

    @Test
    void searchFieldSitsAtTheRightEndOfTheTopBar() {
        ShellLayout layout = ShellLayout.of(854, 480);
        Rect field = layout.searchField();

        assertFalse(field.isEmpty());
        assertTrue(field.y() >= layout.topBar().y());
        assertTrue(field.bottom() <= layout.topBar().bottom());
        assertTrue(field.right() < layout.topBar().right(), "the field keeps a margin from the edge");
        assertEquals(layout.topBar().y() + (layout.topBar().height() - field.height()) / 2, field.y());
    }

    @Test
    void searchFieldClearsTheBrandAndTheMenuButton() {
        ShellLayout compact = ShellLayout.of(480, 270);
        assertTrue(compact.searchField().x() > compact.menuButton().right(),
                "the field must not sit on top of the drawer button");
        assertTrue(ShellLayout.of(640, 360).searchField().x() > ShellLayout.SIDEBAR_WIDTH / 2);
    }

    @Test
    void compactGetsANarrowerSearchFieldThanTheWiderBreakpoints() {
        assertTrue(ShellLayout.of(480, 270).searchField().width()
                < ShellLayout.of(640, 360).searchField().width());
        assertEquals(ShellLayout.of(640, 360).searchField().width(),
                ShellLayout.of(854, 480).searchField().width());
    }

    @Test
    void aTopBarTooNarrowForTheFieldOffersNone() {
        assertTrue(ShellLayout.of(160, 270).searchField().isEmpty());
    }

    @Test
    void aCollapsedTopBarOffersNoSearchField() {
        assertTrue(ShellLayout.of(854, 10).searchField().isEmpty());
    }

    @Test
    void applyAndDiscardSitSideBySideInsideTheBottomBar() {
        ShellLayout layout = ShellLayout.of(854, 480);
        Rect bar = layout.bottomBar();
        Rect apply = layout.applyButton();
        Rect discard = layout.discardButton();

        assertFalse(apply.isEmpty());
        assertFalse(discard.isEmpty());
        assertEquals(apply.y(), discard.y());
        assertEquals(apply.height(), discard.height());
        assertTrue(discard.right() < apply.x(), "the two buttons must not overlap");
        assertTrue(discard.x() >= bar.x());
        assertTrue(apply.right() <= bar.right());
        assertTrue(apply.y() >= bar.y() && apply.bottom() <= bar.bottom());
    }

    @Test
    void aBottomBarTooNarrowForBothButtonsOffersNeither() {
        ShellLayout layout = ShellLayout.of(90, 480);
        assertTrue(layout.applyButton().isEmpty());
        assertTrue(layout.discardButton().isEmpty());
    }

    @Test
    void aCollapsedBottomBarOffersNoButtons() {
        ShellLayout layout = ShellLayout.of(854, 20);
        assertTrue(layout.bottomBar().isEmpty());
        assertTrue(layout.applyButton().isEmpty());
        assertTrue(layout.discardButton().isEmpty());
    }

    @Test
    void degenerateViewportDoesNotProduceNegativeContent() {
        ShellLayout layout = ShellLayout.of(40, 20);
        assertFalse(layout.content().isEmpty() && layout.content().width() < 0);
        assertTrue(layout.content().width() >= 0);
        assertTrue(layout.content().height() >= 0);
    }

    @Test
    void theBrandImagesSitAtTheirRealSizeAndClearTheSearchField() {
        for (int width : new int[] {854, 640, 520, 480}) {
            ShellLayout layout = ShellLayout.of(width, 480);
            Rect logo = layout.brandLogo();
            Rect title = layout.brandTitle();

            assertEquals(ShellLayout.LOGO_W, logo.width(), "width " + width);
            assertEquals(ShellLayout.LOGO_H, logo.height());
            assertEquals(layout.topBar().y() + (layout.topBar().height() - ShellLayout.LOGO_H) / 2, logo.y());
            if (!title.isEmpty()) {
                assertEquals(ShellLayout.TITLE_W, title.width());
                assertEquals(ShellLayout.TITLE_H, title.height());
                assertTrue(title.x() >= logo.right(), "the wordmark never sits on the logo");
                if (!layout.searchField().isEmpty()) {
                    assertTrue(title.right() <= layout.searchField().x(),
                            "width " + width + ": the wordmark collided with the search field");
                }
            }
        }
    }

    @Test
    void aCollapsedTopBarDrawsNoBrand() {
        ShellLayout layout = ShellLayout.of(854, 10);
        assertTrue(layout.brandLogo().isEmpty());
        assertTrue(layout.brandTitle().isEmpty());
    }

    @Test
    void theFavouritesButtonNeverOverlapsTheBrandOrTheSearchField() {
        for (int width : new int[] {854, 700, 640, 520, 480, 400, 320}) {
            ShellLayout layout = ShellLayout.of(width, 480);
            Rect button = layout.favoritesButton(40);
            if (button.isEmpty()) {
                continue;
            }
            assertTrue(button.x() >= layout.brandTitle().right(), "width " + width + ": over the wordmark");
            assertTrue(button.x() >= layout.brandLogo().right(), "width " + width + ": over the logo");
            if (!layout.searchField().isEmpty()) {
                assertTrue(button.right() <= layout.searchField().x(),
                        "width " + width + ": over the search field");
            }
            assertTrue(button.right() <= layout.topBar().right());
        }
    }

    @Test
    void theFavouritesButtonAnchorsToTheBarWhenTheSearchFieldIsGone() {
        ShellLayout narrow = ShellLayout.of(200, 480);
        assertTrue(narrow.searchField().isEmpty(), "this width must have no search field");
        assertFalse(narrow.favoritesButton(40).isEmpty(),
                "with no search field the button is the only way to the favourites page");
    }

    @Test
    void theFavouritesButtonIsIconOnlyAtCompact() {
        ShellLayout compact = ShellLayout.of(480, 480);
        assertEquals(Breakpoint.COMPACT, compact.breakpoint());
        assertEquals(ShellLayout.FAV_PAD * 2 + ShellLayout.FAV_ICON,
                compact.favoritesButton(200).width(), "the label is dropped, not squeezed");
    }

    @Test
    void theFavouritesButtonRejectsANegativeLabel() {
        assertThrows(IllegalArgumentException.class, () -> ShellLayout.of(854, 480).favoritesButton(-1));
    }
}
