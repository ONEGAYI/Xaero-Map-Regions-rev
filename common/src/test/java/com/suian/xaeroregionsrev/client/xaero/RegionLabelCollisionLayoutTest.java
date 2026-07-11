package com.suian.xaeroregionsrev.client.xaero;

import com.suian.xaeroregionsrev.region.RegionId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionLabelCollisionLayoutTest {
    @Test
    void keepsNonOverlappingLabelsInTopToBottomPriorityOrder() {
        var bottom = candidate("bottom", 0, 0, 20, 10);
        var top = candidate("top", 40, 0, 20, 10);

        assertEquals(List.of("top", "bottom"), ids(
                RegionLabelCollisionLayout.visibleCandidates(List.of(bottom, top), Optional.empty(), 2)));
    }

    @Test
    void overlappingTopLabelSuppressesBottomLabel() {
        var bottom = candidate("bottom", 0, 0, 20, 10);
        var top = candidate("top", 10, 0, 20, 10);

        assertEquals(List.of("top"), ids(
                RegionLabelCollisionLayout.visibleCandidates(List.of(bottom, top), Optional.empty(), 0)));
    }

    @Test
    void selectedLabelWinsCollisionEvenWhenItIsBottomLayer() {
        var bottom = candidate("bottom", 0, 0, 20, 10);
        var top = candidate("top", 0, 0, 20, 10);

        assertEquals(List.of("bottom"), ids(RegionLabelCollisionLayout.visibleCandidates(
                List.of(bottom, top), Optional.of(bottom.id()), 2)));
    }

    @Test
    void safetyPaddingTreatsNearLabelsAsColliding() {
        var left = candidate("left", 0, 0, 20, 10);
        var right = candidate("right", 24, 0, 20, 10);

        assertEquals(List.of("right"), ids(
                RegionLabelCollisionLayout.visibleCandidates(List.of(left, right), Optional.empty(), 2)));
    }

    private static RegionLabelCollisionLayout.Candidate candidate(
            String id, int x, int y, int width, int height) {
        return new RegionLabelCollisionLayout.Candidate(
                new RegionId(id), id, 0xFFFFFFFF, x, y, width, height);
    }

    private static List<String> ids(List<RegionLabelCollisionLayout.Candidate> candidates) {
        return candidates.stream().map(candidate -> candidate.id().value()).toList();
    }
}
