package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.region.RegionLimits;
import com.suian.xaeroregionsrev.region.Region;
import com.suian.xaeroregionsrev.region.RegionId;
import com.suian.xaeroregionsrev.region.RegionPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class RegionEditSession {
    private boolean editing;
    private final List<RegionPoint> draftPoints = new ArrayList<>();
    private final List<RegionPoint> redoPoints = new ArrayList<>();
    private RegionId selectedRegionId;
    private List<RegionId> candidateStack = List.of();
    private int selectionIndex = -1;

    public enum EscapeResult {
        CLEARED_DRAFT,
        EXITED_EDIT_MODE,
        IGNORED
    }

    public enum HistoryResult {
        CHANGED,
        IGNORED
    }

    public record SelectionInfo(RegionId id, int index, int total) {
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
        candidateStack = List.of(regionId);
        selectionIndex = 0;
    }

    /**
     * 推进区域选中状态机。
     *
     * @param hitStack selectStack 返回的命中区域列表（渲染顺序：底层在前，顶层在后）
     * @param clickX   本次点击世界坐标 X（当前未参与判定，保留供未来精确坐标扩展）
     * @param clickZ   本次点击世界坐标 Z（当前未参与判定，保留供未来精确坐标扩展）
     * @return true 表示选中了某个区域；false 表示未命中（已清空选中）
     */
    public boolean advanceSelection(List<Region> hitStack, int clickX, int clickZ) {
        if (hitStack.isEmpty()) {
            clearSelection();
            return false;
        }
        // 反转为顶层→底层顺序（hitStack 靠后的为顶层）
        List<RegionId> reversedStack = new ArrayList<>();
        for (int i = hitStack.size() - 1; i >= 0; i--) {
            reversedStack.add(hitStack.get(i).id());
        }

        if (selectionIndex >= 0 && new HashSet<>(reversedStack).equals(new HashSet<>(candidateStack))) {
            // 同一组重叠区域 → 推进索引（基于已有 candidateStack 快照，不受新顺序影响）
            selectionIndex = (selectionIndex + 1) % candidateStack.size();
            selectedRegionId = candidateStack.get(selectionIndex);
        } else {
            // 新的候选组 → 从顶层开始
            candidateStack = Collections.unmodifiableList(reversedStack);
            selectionIndex = 0;
            selectedRegionId = candidateStack.get(0);
        }
        return true;
    }

    public void clearSelection() {
        selectedRegionId = null;
        candidateStack = List.of();
        selectionIndex = -1;
    }

    public Optional<RegionId> selectedRegionId() {
        return Optional.ofNullable(selectedRegionId);
    }

    public Optional<SelectionInfo> selectionInfo() {
        if (selectedRegionId == null || selectionIndex < 0) {
            return Optional.empty();
        }
        return Optional.of(new SelectionInfo(selectedRegionId, selectionIndex + 1, candidateStack.size()));
    }

    public void reset() {
        editing = false;
        clearDraft();
        clearSelection();
    }
}
