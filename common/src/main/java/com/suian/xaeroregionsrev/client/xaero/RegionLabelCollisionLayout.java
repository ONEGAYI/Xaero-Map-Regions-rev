package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegionLabelCollisionLayout {
    private RegionLabelCollisionLayout() {
    }

    public record Candidate(RegionId id, String text, int textArgb,
                            int x, int y, int width, int height) {
        public Candidate {
            Objects.requireNonNull(id, "Region id cannot be null.");
            Objects.requireNonNull(text, "Label text cannot be null.");
        }
    }

    public static List<Candidate> visibleCandidates(List<Candidate> candidates,
                                                     Optional<RegionId> selectedId, int padding) {
        Objects.requireNonNull(candidates, "Candidates cannot be null.");
        Objects.requireNonNull(selectedId, "Selected id cannot be null.");
        int safePadding = Math.max(0, padding);
        List<Candidate> accepted = new ArrayList<>();
        List<Rect> occupied = new ArrayList<>();
        for (Candidate candidate : prioritize(candidates, selectedId)) {
            Rect bounds = Rect.of(candidate, safePadding);
            if (occupied.stream().noneMatch(bounds::intersects)) {
                accepted.add(candidate);
                occupied.add(bounds);
            }
        }
        return List.copyOf(accepted);
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

    private record Rect(int left, int top, int right, int bottom) {
        static Rect of(Candidate candidate, int padding) {
            return new Rect(candidate.x() - padding, candidate.y() - padding,
                    candidate.x() + candidate.width() + padding,
                    candidate.y() + candidate.height() + padding);
        }

        boolean intersects(Rect other) {
            return left <= other.right && right >= other.left
                    && top <= other.bottom && bottom >= other.top;
        }
    }
}
