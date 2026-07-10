package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.client.editor.RegionEditorOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

public final class RegionLabelDisplay {
    private static final String ELLIPSIS = "\u2026";
    private static final double EDGE_EPSILON = 0.001D;

    private RegionLabelDisplay() {
    }

    public record InlineLabel(String text, int x, int y) {
    }

    public static Optional<InlineLabel> layoutInlineLabel(String label, List<RegionEditorOverlay.ScreenPoint> points,
                                                          int textHeight, ToIntFunction<String> widthCalculator) {
        if (label == null || label.isBlank() || points.size() < 3) {
            return Optional.empty();
        }
        RegionEditorOverlay.ScreenPoint anchor = RegionEditorOverlay.labelAnchor(points);
        double halfHeight = Math.max(1, textHeight) / 2.0D;
        double availableWidth = Double.POSITIVE_INFINITY;
        double[] sampleYs = {
                anchor.y() - halfHeight + EDGE_EPSILON,
                anchor.y(),
                anchor.y() + halfHeight - EDGE_EPSILON
        };
        for (double sampleY : sampleYs) {
            Optional<Span> span = horizontalSpanContaining(points, anchor.x(), sampleY);
            if (span.isEmpty()) {
                return Optional.empty();
            }
            availableWidth = Math.min(availableWidth, span.get().width());
        }
        Optional<String> fittedText = fitText(label.strip(), (int) Math.floor(availableWidth), widthCalculator);
        if (fittedText.isEmpty()) {
            return Optional.empty();
        }
        String text = fittedText.get();
        int textWidth = widthCalculator.applyAsInt(text);
        return Optional.of(new InlineLabel(
                text,
                Math.round(anchor.x() - textWidth / 2.0F),
                Math.round(anchor.y() - textHeight / 2.0F)
        ));
    }

    public static boolean isHovered(List<RegionEditorOverlay.ScreenPoint> points, double mouseX, double mouseY) {
        if (points.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int current = 0, previous = points.size() - 1; current < points.size(); previous = current++) {
            RegionEditorOverlay.ScreenPoint a = points.get(current);
            RegionEditorOverlay.ScreenPoint b = points.get(previous);
            boolean intersects = (a.y() > mouseY) != (b.y() > mouseY)
                    && mouseX < (b.x() - a.x()) * (mouseY - a.y()) / (b.y() - a.y()) + a.x();
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static Optional<String> fitText(String label, int maxWidth, ToIntFunction<String> widthCalculator) {
        if (maxWidth <= 0) {
            return Optional.empty();
        }
        if (widthCalculator.applyAsInt(label) <= maxWidth) {
            return Optional.of(label);
        }
        int[] codePoints = label.codePoints().toArray();
        String shortest = new String(codePoints, 0, 1) + ELLIPSIS;
        if (widthCalculator.applyAsInt(shortest) > maxWidth) {
            return Optional.empty();
        }
        for (int visibleCodePoints = codePoints.length - 1; visibleCodePoints >= 1; visibleCodePoints--) {
            String candidate = new String(codePoints, 0, visibleCodePoints).stripTrailing() + ELLIPSIS;
            if (widthCalculator.applyAsInt(candidate) <= maxWidth) {
                return Optional.of(candidate);
            }
        }
        return Optional.of(shortest);
    }

    private static Optional<Span> horizontalSpanContaining(List<RegionEditorOverlay.ScreenPoint> points,
                                                           double anchorX, double sampleY) {
        List<Double> intersections = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            RegionEditorOverlay.ScreenPoint current = points.get(index);
            RegionEditorOverlay.ScreenPoint next = points.get((index + 1) % points.size());
            boolean crosses = (current.y() <= sampleY && next.y() > sampleY)
                    || (next.y() <= sampleY && current.y() > sampleY);
            if (!crosses) {
                continue;
            }
            double ratio = (sampleY - current.y()) / (next.y() - current.y());
            intersections.add(current.x() + ratio * (next.x() - current.x()));
        }
        intersections.sort(Double::compare);
        for (int index = 0; index + 1 < intersections.size(); index += 2) {
            double left = intersections.get(index);
            double right = intersections.get(index + 1);
            if (anchorX + EDGE_EPSILON >= left && anchorX - EDGE_EPSILON <= right) {
                return Optional.of(new Span(left, right));
            }
        }
        return Optional.empty();
    }

    private record Span(double left, double right) {
        double width() {
            return Math.max(0.0D, right - left);
        }
    }
}
