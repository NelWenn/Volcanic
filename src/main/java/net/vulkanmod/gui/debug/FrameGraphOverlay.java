package net.vulkanmod.gui.debug;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.vulkanmod.gui.HUD;
import net.vulkanmod.render.framegraph.Format;
import net.vulkanmod.render.framegraph.FrameGraph;
import net.vulkanmod.render.framegraph.FrameGraph.Node;
import net.vulkanmod.render.framegraph.FrameGraph.ResourceInfo;
import net.vulkanmod.render.framegraph.Phase;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FrameGraphOverlay extends HUD {

    private static final int PANEL_BG = 0xE6151922;
    private static final int HEADER_BG = 0xF01B2333;
    private static final int PANEL_BORDER = 0x33FFFFFF;
    private static final int NODE_BG = 0xFF232A36;
    private static final int NODE_BG_HOVER = 0xFF2E3A4C;
    private static final int NODE_BORDER = 0x40FFFFFF;
    private static final int TEXT_PRIMARY = 0xFFEAEFF5;
    private static final int TEXT_SECONDARY = 0xFF8B96A5;
    private static final int READY_COLOR = 0xFF3DDC97;
    private static final int NOT_READY_COLOR = 0xFFFF5C5C;
    private static final int EDGE_COLOR = 0xFF454F5F;
    private static final int HISTORY_EDGE_COLOR = 0xFF3E8FD0;
    private static final int TOOLTIP_BG = 0xF00E1117;
    private static final int TOOLTIP_BORDER = 0x50FFFFFF;

    private static final int[] PHASE_COLORS = {
            0xFF29B6F6, // FRAME_START
            0xFFAB47BC, // MID_RENDER
            0xFFFFA726  // POST_PROCESS
    };

    private static final int NODE_W = 100;
    private static final int NODE_H = 40;
    private static final int H_GAP = 20;
    private static final int V_GAP = 22;
    private static final int LANE_LABEL_H = 13;
    private static final int LANE_GAP = 14;
    private static final int PADDING = 8;
    private static final int HEADER_H = 20;
    private static final int PANEL_MAX_WIDTH = 820;
    private static final int MAX_VIEWPORT_H = 460;
    private static final int SCROLLBAR_W = 4;
    private static final double SCROLL_SPEED = NODE_H + V_GAP;

    private static final float PANEL_RADIUS = 10f;
    private static final float NODE_RADIUS = 8f;
    private static final float TOOLTIP_RADIUS = 6f;
    private static final float LINE_THICKNESS = 3f;
    private static final long OPEN_ANIM_MS = 220L;
    private static final long HOVER_ANIM_MS = 140L;

    private record NodeBox(Node node, int x, int y, int w, int h, int phaseColor) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<NodeBox> layout = new ArrayList<>();
    private Node hoveredNode;
    private long hoverStartMs;
    private Node fadingOutNode;
    private long hoverExitStartMs;

    private double scrollOffset;
    private int panelXCache, panelWidthCache, viewportTop, viewportBottom;
    private double maxScrollCache;
    private long openStartMs = -1;

    public FrameGraphOverlay() {
        super("vulkanmod.keybind.toggle_framegraph_overlay", GLFW.GLFW_KEY_PAGE_DOWN, "Volcanic");
        this.setEnabled(false);
    }

    private static String basename(String path) {
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    @Override
    public void toggle() {
        super.toggle();

        MouseHandler mouse = Minecraft.getInstance().mouseHandler;

        if (isEnabled()) {
            mouse.releaseMouse();
            openStartMs = System.currentTimeMillis();
        } else {
            mouse.grabMouse();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isEnabled())
            return false;

        scrollOffset = Mth.clamp(scrollOffset - scrollY * SCROLL_SPEED, 0, maxScrollCache);
        return true;
    }

    @Override
    public boolean mouseButton(int button) {
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        FrameGraph graph = Renderer.getInstance().getMainPass().getFrameGraph().get();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        long now = System.currentTimeMillis();

        List<Node> passes = graph != null ? graph.getPasses() : List.of();

        if (openStartMs < 0) openStartMs = now;

        float openEase = easeOutCubic(clamp01((now - openStartMs) / (float) OPEN_ANIM_MS));
        int entryOffset = Math.round((1f - openEase) * 12f);

        int panelX = 10;
        int panelY = 10 - entryOffset;
        int panelWidth = Math.min(guiGraphics.guiWidth() - panelX * 2, PANEL_MAX_WIDTH);
        int canvasX = panelX + PADDING;
        int canvasWidth = panelWidth - PADDING * 2;

        Map<Phase, List<Node>> byPhase = new EnumMap<>(Phase.class);
        for (Node n : passes) byPhase.computeIfAbsent(n.phase, p -> new ArrayList<>()).add(n);

        List<NodeBox> contentBoxes = new ArrayList<>();
        int cursorY = 0;

        for (Phase phase : Phase.values()) {
            List<Node> laneNodes = byPhase.get(phase);
            if (laneNodes == null || laneNodes.isEmpty()) continue;

            cursorY += LANE_LABEL_H;

            int rowX = canvasX;
            int rowY = cursorY;
            int phaseColor = PHASE_COLORS[phase.ordinal() % PHASE_COLORS.length];

            for (Node n : laneNodes) {
                if (rowX + NODE_W > canvasX + canvasWidth && rowX > canvasX) {
                    rowX = canvasX;
                    rowY += NODE_H + V_GAP;
                }

                contentBoxes.add(new NodeBox(n, rowX, rowY, NODE_W, NODE_H, phaseColor));
                rowX += NODE_W + H_GAP;
            }

            cursorY = rowY + NODE_H + LANE_GAP;
        }

        int contentHeight = Math.max(cursorY - LANE_GAP, 0) + PADDING;
        int maxViewport = Math.clamp(guiGraphics.guiHeight() - panelY * 2 - HEADER_H - PADDING * 2, 30, MAX_VIEWPORT_H);
        int viewportHeight = Math.clamp(contentHeight, 30, maxViewport);
        double maxScroll = Math.max(0, contentHeight - viewportHeight);

        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        int viewportY = panelY + HEADER_H + PADDING;
        int panelHeight = HEADER_H + PADDING * 2 + viewportHeight;

        this.panelXCache = panelX;
        this.panelWidthCache = panelWidth;
        this.viewportTop = viewportY;
        this.viewportBottom = viewportY + viewportHeight;
        this.maxScrollCache = maxScroll;

        int scrollShift = viewportY - (int) Math.round(scrollOffset);
        layout.clear();

        for (NodeBox b : contentBoxes)
            layout.add(new NodeBox(b.node(), b.x(), b.y() + scrollShift, b.w(), b.h(), b.phaseColor()));

        Window window = mc.getWindow();

        double mouseX = mc.mouseHandler.xpos() * (double) guiGraphics.guiWidth() / window.getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) guiGraphics.guiHeight() / window.getScreenHeight();

        Node newlyHovered = null;
        boolean mouseInViewport = mouseY >= viewportTop && mouseY <= viewportBottom;

        if (mouseInViewport) {
            for (NodeBox box : layout) {
                if (box.contains(mouseX, mouseY)) {
                    newlyHovered = box.node;
                    break;
                }
            }
        }

        if (newlyHovered != hoveredNode) {
            if (hoveredNode != null) {
                fadingOutNode = hoveredNode;
                hoverExitStartMs = now;
            }

            hoveredNode = newlyHovered;
            hoverStartMs = now;
        }

        drawPanelBackground(guiGraphics, panelX, panelY, panelWidth, panelHeight, openEase);
        drawHeader(guiGraphics, font, panelX, panelY, panelWidth, graph, passes);

        if (graph == null) {
            guiGraphics.drawString(font, "§7No active frame graph.", canvasX, viewportY, TEXT_SECONDARY, false);
            return;
        }

        Map<String, ResourceInfo> resources = new IdentityHashMap<>();
        for (ResourceInfo ri : graph.getResourceInfos()) resources.put(ri.name(), ri);

        Map<Node, NodeBox> boxByNode = new IdentityHashMap<>();
        for (NodeBox b : layout) boxByNode.put(b.node(), b);

        guiGraphics.enableScissor(panelX, viewportY - PADDING, panelX + panelWidth, viewportY + viewportHeight);

        drawEdges(guiGraphics, passes, boxByNode, now);

        for (NodeBox box : layout)
            drawNode(guiGraphics, font, box, now);

        for (Phase phase : Phase.values()) {
            List<Node> laneNodes = byPhase.get(phase);

            if (laneNodes == null || laneNodes.isEmpty()) continue;

            NodeBox first = boxByNode.get(laneNodes.getFirst());

            if (first != null) {
                int color = PHASE_COLORS[phase.ordinal() % PHASE_COLORS.length];

                String phaseName = phase.name().replace('_', ' ');
                int phaseWidth = font.width(phaseName);
                int phasePadding = LANE_LABEL_H / 4;

                fillRoundedRect(guiGraphics, first.x() - 6, first.y() - LANE_LABEL_H - phasePadding, phaseWidth + 30, LANE_LABEL_H, 1f, true, true, withAlphaScale(0xFF000000, 0.4f));
                fillRoundedRect(guiGraphics, first.x() - 4, first.y() - LANE_LABEL_H - phasePadding, 2, LANE_LABEL_H - 2, 1f, color);

                guiGraphics.drawString(font, "§l" + phaseName,
                        first.x(), first.y() - LANE_LABEL_H, color, false);
            }
        }

        guiGraphics.disableScissor();

        if (maxScroll > 0)
            drawScrollbar(guiGraphics, panelX, panelWidth, viewportY, viewportHeight, contentHeight, scrollOffset);

        if (hoveredNode != null) {
            NodeBox box = boxByNode.get(hoveredNode);
            drawTooltip(guiGraphics, font, hoveredNode, box, resources, now);
        }
    }

    private void drawScrollbar(GuiGraphics g, int panelX, int panelWidth, int viewportY, int viewportHeight, int contentHeight, double scrollOffset) {
        int trackX = panelX + panelWidth - PADDING - SCROLLBAR_W;
        fillRoundedRect(g, trackX, viewportY, SCROLLBAR_W, viewportHeight, SCROLLBAR_W / 2f, 0x30FFFFFF);

        int thumbH = Math.max(16, (int) ((long) viewportHeight * viewportHeight / contentHeight));
        double maxScroll = contentHeight - viewportHeight;
        int thumbY = viewportY + (int) Math.round((viewportHeight - thumbH) * (scrollOffset / maxScroll));

        fillRoundedRect(g, trackX, thumbY, SCROLLBAR_W, thumbH, SCROLLBAR_W / 2f, 0x90FFFFFF);
    }

    private void drawPanelBackground(GuiGraphics g, int x, int y, int w, int h, float openEase) {
        int border = withAlphaScale(PANEL_BORDER, openEase);
        int bg = withAlphaScale(PANEL_BG, openEase);
        int headerTop = withAlphaScale(HEADER_BG, openEase);

        fillRoundedRect(g, x, y, w, h, PANEL_RADIUS, border);
        fillRoundedRect(g, x + 1, y + 1, w - 2, h - 2, PANEL_RADIUS - 1, bg);
        fillRoundedRectGradientV(g, x + 1, y + 1, w - 2, HEADER_H - 1, PANEL_RADIUS - 1, true, false, headerTop, bg);
    }

    private void drawHeader(GuiGraphics g, Font font, int x, int y, int w, FrameGraph graph, List<Node> passes) {
        String title = "§l§fFRAME GRAPH" + (graph != null ? " §7· §f" + graph.getId() : "");
        g.drawString(font, title, x + PADDING, y + 8, TEXT_PRIMARY, false);
        if (graph == null) return;

        int notReady = 0;
        for (Node n : passes) if (!isReady(n)) notReady++;
        int resCount = graph.getResourceInfos().size();

        String stats = notReady == 0
                ? "§f" + passes.size() + " passes §7· §f" + resCount + " targets §7· §aall ready"
                : "§f" + passes.size() + " passes §7· §f" + resCount + " targets §7· §c" + notReady + " pending";

        int statsWidth = font.width(stats);

        g.drawString(font, stats, x + w - PADDING - statsWidth, y + 8, TEXT_SECONDARY, false);
    }

    private float hoverAmount(Node node, long now) {
        if (node == hoveredNode)
            return easeOutCubic(clamp01((now - hoverStartMs) / (float) HOVER_ANIM_MS));

        if (node == fadingOutNode)
            return 1f - easeOutCubic(clamp01((now - hoverExitStartMs) / (float) HOVER_ANIM_MS));

        return 0f;
    }

    private void drawNode(GuiGraphics g, Font font, NodeBox box, long now) {
        boolean ready = isReady(box.node());
        float hoverT = hoverAmount(box.node(), now);

        int x = box.x(), w = box.w(), h = box.h();
        int y = box.y() - Math.round(hoverT * 2f);

        if (hoverT > 0.01f) {
            int glowAlpha = Math.round(80 * hoverT);
            fillRoundedRect(g, x - 3, y - 3, w + 6, h + 6, NODE_RADIUS + 3, withAlpha(box.phaseColor(), glowAlpha));
        }

        int bg = lerpColor(NODE_BG, NODE_BG_HOVER, hoverT);
        int border = lerpColor(NODE_BORDER, withAlpha(box.phaseColor(), 170), hoverT);

        fillRoundedRect(g, x, y, w, h, NODE_RADIUS, border);
        fillRoundedRect(g, x + 1, y + 1, w - 2, h - 2, NODE_RADIUS - 1, bg);
        fillRoundedRect(g, x + 3, y + 1, w - 6, 3, 2f, true, false, box.phaseColor());

        int dotColor = ready ? READY_COLOR : pulse(NOT_READY_COLOR, now, 700L);
        fillCircle(g, x + w - 7.5f, y + 7.5f, 2.75f, dotColor);

        String title = box.node().isExecutor()
                ? "⚙ " + box.node().executor.getSimpleName()
                : box.node().pipeline.getSimpleName();
        g.drawString(font, trimToWidth(font, title, w - 14), x + 6, y + 7, TEXT_PRIMARY, false);

        String outLabel = FrameGraph.SWAPCHAIN.equals(box.node().output) ? "→ present" : "→ " + box.node().output;
        g.drawString(font, "§7" + trimToWidth(font, outLabel, w - 12), x + 6, y + 19, TEXT_SECONDARY, false);

        String meta = box.node().phase.name().charAt(0) + " · " + box.node().inputs.size() + " in";
        g.drawString(font, "§8" + meta, x + 6, y + h - 12, TEXT_SECONDARY, false);
    }

    private void drawEdges(GuiGraphics g, List<Node> passes, Map<Node, NodeBox> boxByNode, long now) {
        for (int i = 0; i < passes.size(); i++) {
            Node consumer = passes.get(i);
            NodeBox consumerBox = boxByNode.get(consumer);

            if (consumerBox == null) continue;

            for (String input : consumer.inputs.values()) {
                Node producer = findProducer(passes, i, input);
                if (producer == null || producer == consumer) continue;

                NodeBox producerBox = boxByNode.get(producer);
                if (producerBox == null) continue;

                drawConnector(g, producerBox, consumerBox, input.endsWith("_history"), now);
            }
        }
    }

    private static Node findProducer(List<Node> passes, int consumerIndex, String resourceName) {
        String base = resourceName.endsWith("_history")
                ? resourceName.substring(0, resourceName.length() - "_history".length())
                : resourceName;

        for (int j = consumerIndex - 1; j >= 0; j--) {
            Node candidate = passes.get(j);
            if (base.equals(candidate.output)) return candidate;
        }

        return null;
    }

    private void drawConnector(GuiGraphics g, NodeBox from, NodeBox to, boolean history, long now) {
        int x0 = from.x() + from.w();
        int y0 = from.y() + from.h() / 2;
        int x1 = to.x();
        int y1 = to.y() + to.h() / 2;

        int color = history ? HISTORY_EDGE_COLOR : EDGE_COLOR;

        List<int[]> points = new ArrayList<>();

        points.add(new int[]{x0, y0});
        if (y0 != y1) {
            int midX = x0 + (x1 - x0) / 2;

            points.add(new int[]{midX, y0});
            points.add(new int[]{midX, y1});
        }

        points.add(new int[]{x1, y1});

        for (int i = 0; i < points.size() - 1; i++)
            drawAxisLine(g, points.get(i), points.get(i + 1), LINE_THICKNESS, color);

        for (int[] p : points)
            fillCircle(g, p[0], p[1], LINE_THICKNESS / 2f, color);
    }

    private static void drawAxisLine(GuiGraphics g, int[] a, int[] b, float thickness, int color) {
        float half = thickness / 2f;

        if (a[1] == b[1]) {
            int minX = Math.min(a[0], b[0]), maxX = Math.max(a[0], b[0]);
            g.fill(minX, Math.round(a[1] - half), maxX, Math.round(a[1] + half), color);
        } else if (a[0] == b[0]) {
            int minY = Math.min(a[1], b[1]), maxY = Math.max(a[1], b[1]);
            g.fill(Math.round(a[0] - half), minY, Math.round(a[0] + half), maxY, color);
        }
    }

    private static double dist(int[] a, int[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    private void drawTooltip(GuiGraphics g, Font font, Node node, NodeBox box, Map<String, ResourceInfo> resources, long now) {
        if (box == null) return;

        float alpha = easeOutCubic(clamp01((now - hoverStartMs) / (float) HOVER_ANIM_MS));
        if (alpha <= 0f) return;

        List<String> lines = new ArrayList<>();

        lines.add("§l§f" + (node.isExecutor() ? "Executor Pass" : "Pipeline Pass"));
        lines.add("§7Phase: §f" + node.phase.name());
        lines.add(node.isExecutor()
                ? "§7Executor: §f" + basename(node.executor.getName())
                : "§7Pipeline: §f" + basename(node.pipeline.getName()));
        lines.add("§7Status: " + (isReady(node) ? "§aready" : "§cnot compiled yet"));
        lines.add("");
        lines.add("§7Inputs (" + node.inputs.size() + "):");

        if (node.inputs.isEmpty())
            lines.add("  §8none");
        else {
            for (Map.Entry<Integer, String> e : node.inputs.entrySet()) {
                boolean history = e.getValue().endsWith("_history");
                String base = history ? e.getValue().substring(0, e.getValue().length() - "_history".length()) : e.getValue();
                String tag = history ? " §9[history]" : (resources.containsKey(base) ? "" : " §e[external]");
                lines.add("  §8[" + e.getKey() + "] §f" + e.getValue() + tag);
            }
        }

        lines.add("");
        if (FrameGraph.SWAPCHAIN.equals(node.output))
            lines.add("§7Output: §f→ swapchain (present)");
        else {
            lines.add("§7Output: §f" + node.output);
            ResourceInfo ri = resources.get(node.output);

            if (ri != null) {
                lines.add("  §7Format: §f" + formatName(ri.vkFormat()));
                lines.add("  §7Scale: §f" + trimFloat(ri.scale()) + "x  §7Clear: §f" + trimFloat(ri.clear()));
                lines.add("  §7Ping-pong: §f" + (ri.pingpong() ? "yes" : "no"));
                lines.add("  §7Size: §f" + (ri.width() > 0 ? ri.width() + "x" + ri.height() : "not allocated"));
                lines.add("  §7Allocated: " + (ri.allocated() ? "§ayes" : "§cno"));
                lines.add("  §7Written@pass " + ri.firstWritePass() + " §7· last read@pass " + ri.lastReadPass());
            }
        }

        int textW = 0;

        for (String line : lines) textW = Math.max(textW, font.width(line));

        int tw = textW + PADDING * 2;
        int th = lines.size() * 10 + PADDING * 2;

        int tx = box.x() + box.w() + 8;
        int ty = box.y();
        if (tx + tw > g.guiWidth() - 4) tx = box.x() - tw - 8;

        tx = Mth.clamp(tx, 4, Math.max(4, g.guiWidth() - tw - 4));
        ty = Mth.clamp(ty, 4, Math.max(4, g.guiHeight() - th - 4));
        ty += Math.round((1f - alpha) * 4f);

        int borderColor = withAlpha(TOOLTIP_BORDER, Math.round(140 * alpha));
        int bgColor = withAlpha(TOOLTIP_BG, Math.round(240 * alpha));

        fillRoundedRect(g, tx, ty, tw, th, TOOLTIP_RADIUS, borderColor);
        fillRoundedRect(g, tx + 1, ty + 1, tw - 2, th - 2, TOOLTIP_RADIUS - 1, bgColor);

        int ly = ty + PADDING;
        int textAlpha = Math.round(255 * alpha);

        for (String line : lines) {
            g.drawString(font, line, tx + PADDING, ly, withAlpha(0xFFFFFFFF, textAlpha), false);
            ly += 10;
        }
    }

    private static boolean isReady(Node node) {
        return node.isExecutor() || PipelineRegistry.getOrNull(node.pipeline) != null;
    }

    private static String formatName(int vk) {
        for (Format f : Format.values())
            if (f.vk == vk) return f.name();

        return "VK(" + vk + ")";
    }

    private static String trimFloat(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            if (font.width(sb.toString() + text.charAt(i) + "…") > maxWidth) break;
            sb.append(text.charAt(i));
        }

        return sb + "…";
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int withAlphaScale(int argb, float t) {
        int a = (argb >>> 24) & 0xFF;
        return withAlpha(argb, Math.round(a * clamp01(t)));
    }

    private static float clamp01(float t) {
        return Mth.clamp(t, 0f, 1f);
    }

    private static float easeOutCubic(float t) {
        float f = clamp01(t) - 1f;
        return f * f * f + 1f;
    }

    private static int lerpColor(int c0, int c1, float t) {
        t = clamp01(t);

        int a = Math.round(Mth.lerp(t, (c0 >>> 24) & 0xFF, (c1 >>> 24) & 0xFF));
        int r = Math.round(Mth.lerp(t, (c0 >> 16) & 0xFF, (c1 >> 16) & 0xFF));
        int gr = Math.round(Mth.lerp(t, (c0 >> 8) & 0xFF, (c1 >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(t, c0 & 0xFF, c1 & 0xFF));

        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private static void hFillAA(GuiGraphics g, double xLeft, double xRight, int y, int color) {
        if (xRight - xLeft < 0.02) return;

        int fullStart = (int) Math.ceil(xLeft - 1e-6);
        int fullEnd = (int) Math.floor(xRight + 1e-6);

        if (fullEnd <= fullStart) {
            int col = (int) Math.floor((xLeft + xRight) * 0.5);
            g.fill(col, y, col + 1, y + 1, withAlphaScale(color, (float) (xRight - xLeft)));
            return;
        }

        g.fill(fullStart, y, fullEnd, y + 1, color);

        double leftCov = fullStart - xLeft;
        if (leftCov > 0.02) g.fill(fullStart - 1, y, fullStart, y + 1, withAlphaScale(color, (float) leftCov));

        double rightCov = xRight - fullEnd;
        if (rightCov > 0.02) g.fill(fullEnd, y, fullEnd + 1, y + 1, withAlphaScale(color, (float) rightCov));
    }

    private static double cornerInset(int row, int h, float r, boolean roundTop, boolean roundBottom) {
        if (roundTop && row < r) {
            double dy = r - (row + 0.5);
            return r - Math.sqrt(Math.max(0, (double) r * r - dy * dy));
        }

        if (roundBottom && row >= h - r) {
            double dy = (row + 0.5) - (h - r);
            return r - Math.sqrt(Math.max(0, (double) r * r - dy * dy));
        }

        return 0;
    }

    private static void fillRoundedRect(GuiGraphics g, int x, int y, int w, int h, float radius, boolean roundTop, boolean roundBottom, int color) {
        if (w <= 0 || h <= 0) return;

        float r = Math.clamp(radius, 0, Math.min(w, h) / 2f);

        for (int row = 0; row < h; row++) {
            double inset = cornerInset(row, h, r, roundTop, roundBottom);

            hFillAA(g, x + inset, x + w - inset, y + row, color);
        }
    }

    private static void fillRoundedRect(GuiGraphics g, int x, int y, int w, int h, float radius, int color) {
        fillRoundedRect(g, x, y, w, h, radius, true, true, color);
    }

    private static void fillRoundedRectGradientV(GuiGraphics g, int x, int y, int w, int h, float radius,
                                                  boolean roundTop, boolean roundBottom, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;

        float r = Math.clamp(radius, 0, Math.min(w, h) / 2f);

        for (int row = 0; row < h; row++) {
            double inset = cornerInset(row, h, r, roundTop, roundBottom);
            int color = lerpColor(colorTop, colorBottom, h <= 1 ? 0f : row / (float) (h - 1));

            hFillAA(g, x + inset, x + w - inset, y + row, color);
        }
    }

    private static void fillCircle(GuiGraphics g, float cx, float cy, float r, int color) {
        int top = Mth.floor(cy - r);
        int bottom = Mth.ceil(cy + r) - 1;

        for (int y = top; y <= bottom; y++) {
            double dy = (y + 0.5) - cy;
            double under = (double) r * r - dy * dy;

            if (under < 0) continue;

            double dx = Math.sqrt(under);

            hFillAA(g, cx - dx, cx + dx, y, color);
        }
    }

    private static int pulse(int color, long now, long ms) {
        float t = (float) ((now % ms) / (double) ms) * (float) (Math.PI * 2);
        int alpha = Math.round(150 + 105 * Mth.sin(t));

        return withAlpha(color, alpha);
    }
}
