package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.ui.core.BreadcrumbModel;
import net.vulkanmod.config.ui.core.KeyAction;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.ShellLayout;
import net.vulkanmod.config.ui.core.TabStripModel;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.render.SurfacePainter;

import java.util.List;

public class VolcanicScreen extends Screen {
    private static final int SIDEBAR_SCROLL_STEP = 25;
    private static final int PRIMARY_BUTTON = 0;

    private final Screen parent;
    private final NavPresenter presenter = new NavPresenter();
    private final ShellRenderer renderer = new ShellRenderer(Theme.volcanic());
    private ShellLayout layout = ShellLayout.of(0, 0);
    private int sidebarScroll;
    private boolean drawerOpen;

    public VolcanicScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout = ShellLayout.of(this.width, this.height);
        if (!layout.hasDrawer()) {
            this.drawerOpen = false;
        }
        this.sidebarScroll = presenter.sidebar().clampScroll(this.sidebarScroll, navViewport().height());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        SurfacePainter painter = SurfacePainter.create(guiGraphics, this.font);
        renderer.render(guiGraphics, painter, this.font, layout, presenter, sidebarScroll, mouseX, mouseY, drawerOpen);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != PRIMARY_BUTTON) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = (int) mouseX;
        int y = (int) mouseY;
        if (layout.menuButton().contains(x, y)) {
            setDrawerOpen(!drawerOpen);
            return true;
        }
        if (clickSidebar(x, y)) {
            return true;
        }
        if (drawerOpen) {
            setDrawerOpen(false);
            return true;
        }
        if (clickTabStrip(x, y) || clickBreadcrumb(x, y)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (!nav.contains((int) mouseX, (int) mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int step = (int) Math.signum(scrollY) * SIDEBAR_SCROLL_STEP;
        this.sidebarScroll = presenter.sidebar().clampScroll(this.sidebarScroll - step, nav.height());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyAction action = UiKeys.actionFor(keyCode, modifiers);
        switch (action) {
            case CLOSE -> {
                if (drawerOpen) {
                    setDrawerOpen(false);
                } else {
                    this.onClose();
                }
                return true;
            }
            case BACK -> {
                if (drawerOpen) {
                    setDrawerOpen(false);
                    return true;
                }
                if (!presenter.stack().canGoBack()) {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
                presenter.back();
                return true;
            }
            case NEXT, PREVIOUS, UP, DOWN -> {
                presenter.focus().apply(action);
                return true;
            }
            case ACTIVATE -> {
                RouteId route = presenter.focusedRoute();
                if (route == null) {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
                select(route, presenter.focus().activeRegion());
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean clickSidebar(int mouseX, int mouseY) {
        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (!nav.contains(mouseX, mouseY)) {
            return false;
        }

        RouteId route = presenter.sidebar().routeAt(mouseY - nav.y() + sidebarScroll);
        if (route == null) {
            return true;
        }

        select(route, NavPresenter.REGION_SIDEBAR);
        setDrawerOpen(false);
        return true;
    }

    private boolean clickTabStrip(int mouseX, int mouseY) {
        List<Rect> boxes = renderer.tabStripBoxes(this.font, layout, presenter);
        int index = TabStripModel.indexAt(boxes, mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        select(presenter.subTabs().get(index).route(), NavPresenter.REGION_CONTENT);
        return true;
    }

    private boolean clickBreadcrumb(int mouseX, int mouseY) {
        List<Rect> segments = renderer.breadcrumbBoxes(this.font, layout, presenter);
        int index = BreadcrumbModel.indexAt(segments, mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        presenter.navigate(presenter.stack().trail().get(index));
        return true;
    }

    private void select(RouteId route, String regionId) {
        presenter.navigate(route);
        presenter.focus().focusRegion(regionId);
        presenter.focus().ring(regionId).focus(route.toString());
    }

    private void setDrawerOpen(boolean open) {
        if (!layout.hasDrawer()) {
            this.drawerOpen = false;
            return;
        }
        this.drawerOpen = open;
        presenter.focus().focusRegion(open ? NavPresenter.REGION_SIDEBAR : NavPresenter.REGION_CONTENT);
    }

    private Rect navViewport() {
        return layout.sidebarOrDrawer(true);
    }
}
