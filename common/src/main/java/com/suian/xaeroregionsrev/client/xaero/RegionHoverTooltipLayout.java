package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionHoverTooltipLayout {
    public static final int MAX_ROWS = 8;
    private static final int VERTICAL_SAFE_MARGIN = 16;

    private RegionHoverTooltipLayout() {
    }

    public record Candidate(RegionId id, String label, int labelArgb) {
        public Candidate {
            Objects.requireNonNull(id, "Region id cannot be null.");
            Objects.requireNonNull(label, "Region label cannot be null.");
        }
    }

    public sealed interface Row permits RegionRow, OverflowRow {
    }

    public record RegionRow(Candidate candidate, int textRgb, int outlineArgb) implements Row {
    }

    public record OverflowRow(int hiddenCount) implements Row {
    }

    public record Layout(List<Row> rows, int hiddenCount) {
        public Layout {
            rows = List.copyOf(rows);
        }
    }

    public static Layout layout(List<Candidate> candidates, Optional<RegionId> selectedId,
                                int screenHeight, int rowHeight) {
        Objects.requireNonNull(candidates, "Candidates cannot be null.");
        Objects.requireNonNull(selectedId, "Selected id cannot be null.");
        if (candidates.isEmpty()) {
            return new Layout(List.of(), 0);
        }

        List<Candidate> prioritized = prioritize(candidates, selectedId);
        int safeRowHeight = Math.max(1, rowHeight);
        int heightCapacity = Math.max(1, (Math.max(0, screenHeight - VERTICAL_SAFE_MARGIN)) / safeRowHeight);
        int capacity = Math.min(MAX_ROWS, heightCapacity);
        boolean overflows = prioritized.size() > capacity;
        int detailCount = Math.min(overflows && capacity > 1 ? capacity - 1 : capacity, prioritized.size());
        int hiddenCount = prioritized.size() - detailCount;

        List<Row> rows = new ArrayList<>(capacity);
        for (int index = 0; index < detailCount; index++) {
            Candidate candidate = prioritized.get(index);
            int rgb = candidate.labelArgb() & 0x00FFFFFF;
            rows.add(new RegionRow(candidate, rgb, outlineArgbForRgb(rgb)));
        }
        if (hiddenCount > 0 && capacity > 1) {
            rows.add(new OverflowRow(hiddenCount));
        }
        return new Layout(rows, hiddenCount);
    }

    public static int outlineArgbForRgb(int rgb) {
        double luminance = relativeLuminance(rgb & 0x00FFFFFF);
        double blackContrast = (luminance + 0.05D) / 0.05D;
        double whiteContrast = 1.05D / (luminance + 0.05D);
        return blackContrast >= whiteContrast ? 0xFF000000 : 0xFFFFFFFF;
    }

    private static List<Candidate> prioritize(List<Candidate> candidates, Optional<RegionId> selectedId) {
        List<Candidate> prioritized = new ArrayList<>(candidates.size());
        selectedId.flatMap(id -> candidates.stream().filter(candidate -> candidate.id().equals(id)).findFirst())
                .ifPresent(prioritized::add);
        for (int index = candidates.size() - 1; index >= 0; index--) {
            Candidate candidate = candidates.get(index);
            if (selectedId.isEmpty() || !candidate.id().equals(selectedId.get())) {
                prioritized.add(candidate);
            }
        }
        return prioritized;
    }

    private static double relativeLuminance(int rgb) {
        return 0.2126D * linearChannel((rgb >>> 16) & 0xFF)
                + 0.7152D * linearChannel((rgb >>> 8) & 0xFF)
                + 0.0722D * linearChannel(rgb & 0xFF);
    }

    private static double linearChannel(int channel) {
        double value = channel / 255.0D;
        return value <= 0.04045D ? value / 12.92D : Math.pow((value + 0.055D) / 1.055D, 2.4D);
    }
}
