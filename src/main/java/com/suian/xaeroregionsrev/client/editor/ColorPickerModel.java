package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.ArgbColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ColorPickerModel {
    private static final int SCREEN_MARGIN = 8;
    private static final int MAX_PANEL_WIDTH = 760;
    private static final int MAX_PANEL_HEIGHT = 320;
    private static final int MIN_PANEL_WIDTH = 360;
    private static final int MIN_PANEL_HEIGHT = 220;
    private static final int CONTROL_COLUMN_MIN_WIDTH = 170;

    private ColorPickerModel() {
    }

    public enum Channel {
        RED,
        GREEN,
        BLUE,
        ALPHA
    }

    public record Channels(int red, int green, int blue, int alpha) {
        public Channels {
            red = clamp(red);
            green = clamp(green);
            blue = clamp(blue);
            alpha = clamp(alpha);
        }

        public static Channels from(ArgbColor color) {
            int value = color.value();
            return new Channels(
                    (value >>> 16) & 0xFF,
                    (value >>> 8) & 0xFF,
                    value & 0xFF,
                    (value >>> 24) & 0xFF
            );
        }

        public Channels with(Channel channel, int value) {
            return switch (channel) {
                case RED -> new Channels(value, green, blue, alpha);
                case GREEN -> new Channels(red, value, blue, alpha);
                case BLUE -> new Channels(red, green, value, alpha);
                case ALPHA -> new Channels(red, green, blue, value);
            };
        }

        public int value(Channel channel) {
            return switch (channel) {
                case RED -> red;
                case GREEN -> green;
                case BLUE -> blue;
                case ALPHA -> alpha;
            };
        }

        public ArgbColor toColor() {
            return new ArgbColor(alpha << 24 | red << 16 | green << 8 | blue);
        }
    }

    public static List<ArgbColor> rememberColor(List<ArgbColor> colors, ArgbColor color, int limit) {
        List<ArgbColor> remembered = new ArrayList<>();
        remembered.add(color);
        for (ArgbColor existing : colors) {
            if (!existing.equals(color)) {
                remembered.add(existing);
            }
            if (remembered.size() >= limit) {
                break;
            }
        }
        return List.copyOf(remembered);
    }

    public static List<ArgbColor> releaseColor(List<ArgbColor> colors, ArgbColor color) {
        List<ArgbColor> released = new ArrayList<>();
        for (ArgbColor existing : colors) {
            if (!existing.equals(color)) {
                released.add(existing);
            }
        }
        return List.copyOf(released);
    }

    public static Layout layout(int screenWidth, int screenHeight) {
        int availableWidth = Math.max(MIN_PANEL_WIDTH, screenWidth - SCREEN_MARGIN * 2);
        int availableHeight = Math.max(MIN_PANEL_HEIGHT, screenHeight - SCREEN_MARGIN * 2);
        int width = Math.min(MAX_PANEL_WIDTH, availableWidth);
        int height = Math.min(MAX_PANEL_HEIGHT, availableHeight);
        int left = Math.max(SCREEN_MARGIN, (screenWidth - width) / 2);
        int top = Math.max(SCREEN_MARGIN, (screenHeight - height) / 2);
        int controlsLeft = left + Math.max(width / 2 + 18, width - CONTROL_COLUMN_MIN_WIDTH - 14);
        int plateSpaceWidth = Math.max(96, controlsLeft - left - 34);
        int plateSpaceHeight = Math.max(88, height - 122);
        int plateDiameter = Math.max(88, Math.min(190, Math.min(plateSpaceWidth, plateSpaceHeight)));
        int plateLeft = left + Math.max(16, (controlsLeft - left - plateDiameter) / 2);
        int plateTop = top + 48 + Math.max(0, (height - 116 - plateDiameter) / 2);
        int inputX = controlsLeft + 22;
        int sliderX = inputX + 56;
        int sliderWidth = Math.max(42, left + width - sliderX - 12);
        return new Layout(left, top, width, height, plateLeft, plateTop, plateDiameter, controlsLeft,
                inputX, sliderX, sliderWidth);
    }

    public static Optional<ArgbColor> colorAtWheel(double normalizedX, double normalizedY, int alpha) {
        double saturation = Math.sqrt(normalizedX * normalizedX + normalizedY * normalizedY);
        if (saturation > 1.0D) {
            return Optional.empty();
        }
        double angle = Math.atan2(normalizedY, normalizedX);
        double hue = angle / (Math.PI * 2.0D);
        if (hue < 0.0D) {
            hue += 1.0D;
        }
        return Optional.of(new ArgbColor(clamp(alpha) << 24 | hsvToRgb(hue, saturation, 1.0D)));
    }

    public static WheelPosition wheelPosition(Channels channels) {
        Hsv hsv = rgbToHsv(channels.red(), channels.green(), channels.blue());
        double angle = hsv.hue() * Math.PI * 2.0D;
        return new WheelPosition(Math.cos(angle) * hsv.saturation(), Math.sin(angle) * hsv.saturation());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(0xFF, value));
    }

    private static int hsvToRgb(double hue, double saturation, double value) {
        double scaledHue = hue * 6.0D;
        int sector = (int) Math.floor(scaledHue) % 6;
        double fraction = scaledHue - Math.floor(scaledHue);
        double p = value * (1.0D - saturation);
        double q = value * (1.0D - fraction * saturation);
        double t = value * (1.0D - (1.0D - fraction) * saturation);
        return switch (sector) {
            case 0 -> rgb(value, t, p);
            case 1 -> rgb(q, value, p);
            case 2 -> rgb(p, value, t);
            case 3 -> rgb(p, q, value);
            case 4 -> rgb(t, p, value);
            default -> rgb(value, p, q);
        };
    }

    private static int rgb(double red, double green, double blue) {
        return clamp((int) Math.round(red * 255.0D)) << 16
                | clamp((int) Math.round(green * 255.0D)) << 8
                | clamp((int) Math.round(blue * 255.0D));
    }

    private static Hsv rgbToHsv(int red, int green, int blue) {
        double r = red / 255.0D;
        double g = green / 255.0D;
        double b = blue / 255.0D;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        double hue;
        if (delta == 0.0D) {
            hue = 0.0D;
        } else if (max == r) {
            hue = ((g - b) / delta) / 6.0D;
        } else if (max == g) {
            hue = (((b - r) / delta) + 2.0D) / 6.0D;
        } else {
            hue = (((r - g) / delta) + 4.0D) / 6.0D;
        }
        if (hue < 0.0D) {
            hue += 1.0D;
        }
        double saturation = max == 0.0D ? 0.0D : delta / max;
        return new Hsv(hue, saturation);
    }

    public record Layout(int left, int top, int width, int height, int plateLeft, int plateTop, int plateDiameter,
                         int controlsLeft, int inputX, int sliderX, int sliderWidth) {
    }

    public record WheelPosition(double x, double y) {
    }

    private record Hsv(double hue, double saturation) {
    }
}
