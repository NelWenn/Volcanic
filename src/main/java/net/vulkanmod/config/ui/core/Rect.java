package net.vulkanmod.config.ui.core;

public record Rect(int x, int y, int width, int height) {
    public static final Rect EMPTY = new Rect(0, 0, 0, 0);

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }

    public Rect inset(int amount) {
        return new Rect(x + amount, y + amount,
                Math.max(0, width - amount * 2), Math.max(0, height - amount * 2));
    }

    public Rect translated(int dx, int dy) {
        return dx == 0 && dy == 0 ? this : new Rect(x + dx, y + dy, width, height);
    }

    public Rect dropTop(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        return new Rect(x, y + amount, width, Math.max(0, height - amount));
    }

    public Rect withHeight(int newHeight) {
        return new Rect(x, y, width, Math.max(0, newHeight));
    }

    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }
}
