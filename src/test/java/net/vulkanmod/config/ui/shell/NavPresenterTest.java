package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.RouteId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavPresenterTest {

    @Test
    void everyTopLevelRouteIsASidebarRow() {
        NavPresenter presenter = new NavPresenter();
        assertEquals(11, presenter.tree().sidebarRows().size());
    }

    @Test
    void startsOnTheFirstSidebarRow() {
        NavPresenter presenter = new NavPresenter();
        assertEquals(RouteId.parse("overview"), presenter.stack().current());
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
    void everyNodeTitleKeyIsNonBlank() {
        NavPresenter presenter = new NavPresenter();
        for (NavNode node : presenter.tree().sidebarRows()) {
            assertFalse(node.titleKey().isBlank());
        }
    }
}
