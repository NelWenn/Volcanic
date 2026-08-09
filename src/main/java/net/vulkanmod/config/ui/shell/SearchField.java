package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.ui.core.ColorToken;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.SearchIndex;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.settings.SettingsCatalog;

import java.util.ArrayList;
import java.util.List;

public final class SearchField {
    public static final int RESULT_LIMIT = 24;
    static final String KEY_HINT = "vulkanmod.ui.search.hint";

    private static final int MAX_LENGTH = 48;
    private static final int TEXT_HEIGHT = 9;
    private static final int PAD_X = 6;

    private final Rect frame;
    private final EditBox box;
    private final SearchIndex index;
    private List<SearchIndex.Entry> results = List.of();

    public SearchField(Font font, Rect frame, SearchIndex index, Theme theme) {
        if (font == null) {
            throw new IllegalArgumentException("font must not be null");
        }
        if (frame == null || frame.isEmpty()) {
            throw new IllegalArgumentException("frame must not be empty: " + frame);
        }
        if (index == null) {
            throw new IllegalArgumentException("index must not be null");
        }
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.frame = frame;
        this.index = index;

        Rect text = textArea(frame);
        this.box = new EditBox(font, text.x(), text.y(), text.width(), text.height(),
                Component.translatable(KEY_HINT));
        box.setBordered(false);
        box.setTextShadow(false);
        box.setMaxLength(MAX_LENGTH);
        box.setTextColor(theme.color(ColorToken.TEXT_PRIMARY));
        box.setHint(Component.translatable(KEY_HINT)
                .withStyle(style -> style.withColor(theme.color(ColorToken.TEXT_MUTED) & 0xFFFFFF)));
        box.setResponder(this::onQueryChanged);
    }

    public static Rect textArea(Rect frame) {
        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        int width = frame.width() - PAD_X * 2;
        if (frame.isEmpty() || width <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(frame.x() + PAD_X, frame.y() + (frame.height() - TEXT_HEIGHT) / 2,
                width, TEXT_HEIGHT);
    }

    public static SearchIndex indexOf(SettingsCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("catalog must not be null");
        }
        List<SearchIndex.Entry> entries = new ArrayList<>(catalog.registry().size());
        for (SettingMeta meta : catalog.registry().all()) {
            entries.add(new SearchIndex.Entry(meta.id(), I18n.get(meta.titleKey()),
                    meta.descriptionKey() == null ? "" : I18n.get(meta.descriptionKey()),
                    choiceLabels(catalog, meta), meta.route(), meta.source()));
        }
        return SearchIndex.of(entries);
    }

    private static List<String> choiceLabels(SettingsCatalog catalog, SettingMeta meta) {
        List<String> labels = new ArrayList<>();
        for (String choice : catalog.binding(meta.id()).choices()) {
            String label = I18n.get(choice);
            if (!label.isBlank()) {
                labels.add(label);
            }
        }
        return labels;
    }

    public EditBox widget() {
        return box;
    }

    public String query() {
        return box.getValue();
    }

    public void setQuery(String query) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null; use \"\"");
        }
        box.setValue(query);
    }

    public List<SearchIndex.Entry> results() {
        return results;
    }

    public boolean isFocused() {
        return box.isFocused();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        box.render(graphics, mouseX, mouseY, partialTick);
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        box.keyPressed(keyCode, scanCode, modifiers);
    }

    public void click(double mouseX, double mouseY, int button) {
        Rect text = textArea(frame);
        double x = Math.min(text.right() - 1, Math.max(text.x(), mouseX));
        double y = Math.min(text.bottom() - 1, Math.max(text.y(), mouseY));
        if (!box.mouseClicked(x, y, button)) {
            box.moveCursorToEnd(false);
        }
    }

    public void selectAll() {
        box.moveCursorToEnd(false);
        box.setHighlightPos(0);
    }

    private void onQueryChanged(String query) {
        this.results = index.search(query, RESULT_LIMIT);
    }
}
