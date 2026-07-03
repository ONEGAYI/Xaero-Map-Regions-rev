package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegionEditSession {
    private boolean editing;
    private final List<RegionPoint> draftPoints = new ArrayList<>();
    private RegionId selectedRegionId;

    public enum EscapeResult {
        CLEARED_DRAFT,
        EXITED_EDIT_MODE,
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
        draftPoints.add(point);
        return true;
    }

    public List<RegionPoint> draftPoints() {
        return List.copyOf(draftPoints);
    }

    public void clearDraft() {
        draftPoints.clear();
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
