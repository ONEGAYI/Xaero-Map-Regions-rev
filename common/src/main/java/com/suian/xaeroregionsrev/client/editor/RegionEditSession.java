package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegionEditSession {
    private boolean editing;
    private final List<RegionPoint> draftPoints = new ArrayList<>();
    private final List<RegionPoint> redoPoints = new ArrayList<>();
    private RegionId selectedRegionId;

    public enum EscapeResult {
        CLEARED_DRAFT,
        EXITED_EDIT_MODE,
        IGNORED
    }

    public enum HistoryResult {
        CHANGED,
        IGNORED
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        if (this.editing == editing) {
            return;
        }
        this.editing = editing;
        if (!editing) {
            clearDraft();
            clearSelection();
        }
    }

    public void toggleEditing() {
        setEditing(!editing);
    }

    public boolean addDraftPoint(RegionPoint point) {
        if (!editing) {
            return false;
        }
        if (draftPoints.size() >= RegionLimits.MAX_POINTS_PER_REQUEST) {
            return false;
        }
        draftPoints.add(point);
        redoPoints.clear();
        return true;
    }

    public List<RegionPoint> draftPoints() {
        return List.copyOf(draftPoints);
    }

    public void clearDraft() {
        draftPoints.clear();
        redoPoints.clear();
    }

    public HistoryResult undoDraftPoint() {
        if (!editing || draftPoints.isEmpty()) {
            return HistoryResult.IGNORED;
        }
        redoPoints.add(draftPoints.remove(draftPoints.size() - 1));
        return HistoryResult.CHANGED;
    }

    public HistoryResult redoDraftPoint() {
        if (!editing || redoPoints.isEmpty() || draftPoints.size() >= RegionLimits.MAX_POINTS_PER_REQUEST) {
            return HistoryResult.IGNORED;
        }
        draftPoints.add(redoPoints.remove(redoPoints.size() - 1));
        return HistoryResult.CHANGED;
    }

    public boolean canSubmitDraft() {
        return editing && draftPoints.size() >= 3;
    }

    public EscapeResult handleEscape() {
        if (!draftPoints.isEmpty()) {
            clearDraft();
            return EscapeResult.CLEARED_DRAFT;
        }
        if (editing) {
            setEditing(false);
            return EscapeResult.EXITED_EDIT_MODE;
        }
        return EscapeResult.IGNORED;
    }

    public void select(RegionId regionId) {
        selectedRegionId = regionId;
    }

    public void clearSelection() {
        selectedRegionId = null;
    }

    public Optional<RegionId> selectedRegionId() {
        return Optional.ofNullable(selectedRegionId);
    }

    public void reset() {
        editing = false;
        clearDraft();
        clearSelection();
    }
}
