package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NavStackTest {

    private static final RouteId OVERVIEW = RouteId.parse("overview");
    private static final RouteId RENDERING = RouteId.parse("rendering");
    private static final RouteId CULLING = RouteId.parse("rendering.culling");

    private static NavTree tree() {
        return new NavTree.Builder()
                .add(new NavNode(OVERVIEW, "k.overview", null, true))
                .add(new NavNode(RENDERING, "k.rendering", null, true))
                .add(new NavNode(CULLING, "k.culling", null, false))
                .build();
    }

    private static NavStack stack() {
        return new NavStack(tree(), OVERVIEW);
    }

    @Test
    void startsAtTheInitialRouteWithNoHistory() {
        NavStack stack = stack();
        assertEquals(OVERVIEW, stack.current());
        assertFalse(stack.canGoBack());
        assertFalse(stack.canGoForward());
    }

    @Test
    void navigateMovesAndEnablesBack() {
        NavStack stack = stack();
        assertTrue(stack.navigate(RENDERING));
        assertEquals(RENDERING, stack.current());
        assertTrue(stack.canGoBack());
    }

    @Test
    void navigatingToTheCurrentRouteIsANoOp() {
        NavStack stack = stack();
        assertFalse(stack.navigate(OVERVIEW));
        assertFalse(stack.canGoBack());
    }

    @Test
    void navigatingToAnUnknownRouteChangesNothing() {
        NavStack stack = stack();
        assertFalse(stack.navigate(RouteId.parse("nope")));
        assertEquals(OVERVIEW, stack.current());
        assertFalse(stack.canGoBack());
    }

    @Test
    void backAndForwardRetraceTheSamePath() {
        NavStack stack = stack();
        stack.navigate(RENDERING);
        stack.navigate(CULLING);
        assertTrue(stack.back());
        assertEquals(RENDERING, stack.current());
        assertTrue(stack.back());
        assertEquals(OVERVIEW, stack.current());
        assertFalse(stack.back());
        assertTrue(stack.forward());
        assertEquals(RENDERING, stack.current());
    }

    @Test
    void navigatingAfterBackDropsTheForwardHistory() {
        NavStack stack = stack();
        stack.navigate(RENDERING);
        stack.navigate(CULLING);
        stack.back();
        assertTrue(stack.canGoForward());
        stack.navigate(OVERVIEW);
        assertFalse(stack.canGoForward());
        assertFalse(stack.forward());
    }

    @Test
    void trailIsTheAncestorChainOfTheCurrentRoute() {
        NavStack stack = stack();
        stack.navigate(CULLING);
        assertEquals(List.of(RENDERING, CULLING), stack.trail());
    }

    @Test
    void trailOfATopLevelRouteIsJustItself() {
        assertEquals(List.of(OVERVIEW), stack().trail());
    }

    @Test
    void anInitialRouteAbsentFromTheTreeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new NavStack(tree(), RouteId.parse("nope")));
    }

    @Test
    void navigatingToANullRouteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> stack().navigate(null));
    }
}
