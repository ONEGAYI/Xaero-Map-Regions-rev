package com.suian.xaeroregionsrev.client.xaero;

public final class RegionRenderStyle {
    public static final int BOUNDARY_THICKNESS = 2;
    public static final int VERTEX_RADIUS = 3;
    public static final int THIN_BOUNDARY_THICKNESS = 1;
    public static final int THIN_VERTEX_RADIUS = 1;
    private static final float HIDE_DECORATION_MINOR_EXTENT = 24.0F;
    private static final float FULL_DECORATION_MINOR_EXTENT = 80.0F;
    private static final int BOUNDARY_ALPHA_FLOOR = 0xCC;
    private static final int BOUNDARY_ALPHA_BOOST = 0x66;
    private static final int BOUNDARY_COLOR_SHIFT = 38;

    private RegionRenderStyle() {
    }

    public static int boundaryColor(int fillColor) {
        int alpha = clamp(((fillColor >>> 24) & 0xFF) + BOUNDARY_ALPHA_BOOST);
        alpha = Math.max(BOUNDARY_ALPHA_FLOOR, alpha);
        int red = (fillColor >>> 16) & 0xFF;
        int green = (fillColor >>> 8) & 0xFF;
        int blue = fillColor & 0xFF;
        int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
        int shift = luminance >= 128 ? -BOUNDARY_COLOR_SHIFT : BOUNDARY_COLOR_SHIFT;
        return alpha << 24
                | clamp(red + shift) << 16
                | clamp(green + shift) << 8
                | clamp(blue + shift);
    }

    public static Decoration decorationForProjectedBounds(float width, float height) {
        float minorExtent = Math.min(Math.abs(width), Math.abs(height));
        if (minorExtent < HIDE_DECORATION_MINOR_EXTENT) {
            return Decoration.hidden();
        }
        if (minorExtent < FULL_DECORATION_MINOR_EXTENT) {
            return new Decoration(THIN_BOUNDARY_THICKNESS, THIN_VERTEX_RADIUS);
        }
        return new Decoration(BOUNDARY_THICKNESS, VERTEX_RADIUS);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(0xFF, value));
    }

    public record Decoration(int boundaryThickness, int vertexRadius) {
        public static Decoration hidden() {
            return new Decoration(0, 0);
        }

        public boolean visible() {
            return boundaryThickness > 0 && vertexRadius > 0;
        }
    }
}
