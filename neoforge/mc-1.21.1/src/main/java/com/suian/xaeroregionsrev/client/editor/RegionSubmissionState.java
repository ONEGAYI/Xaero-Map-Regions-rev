package com.suian.xaeroregionsrev.client.editor;

import com.suian.xaeroregionsrev.network.payload.RegionEditResultPacket;

public final class RegionSubmissionState {
    public static final long WAIT_TIMEOUT_MILLIS = 10_000L;

    private long requestId;
    private long submittedAtMillis;
    private boolean pending;
    private boolean failureReceived;

    public enum ResultAction {
        IGNORED,
        SHOW_FAILURE,
        CLOSE_SCREEN
    }

    public enum TickAction {
        NONE,
        TIMEOUT,
        RESTORE_AFTER_FAILURE
    }

    public void submit(long requestId, long nowMillis) {
        this.requestId = requestId;
        this.submittedAtMillis = nowMillis;
        this.pending = true;
        this.failureReceived = false;
    }

    public ResultAction receive(RegionEditResultPacket packet, long nowMillis) {
        if (!pending || packet.requestId() != requestId || isExpired(nowMillis)) {
            return ResultAction.IGNORED;
        }
        if (packet.success() && packet.closeScreen()) {
            clear();
            return ResultAction.CLOSE_SCREEN;
        }
        failureReceived = true;
        return ResultAction.SHOW_FAILURE;
    }

    public TickAction tick(long nowMillis) {
        if (!pending || !isExpired(nowMillis)) {
            return TickAction.NONE;
        }
        TickAction action = failureReceived ? TickAction.RESTORE_AFTER_FAILURE : TickAction.TIMEOUT;
        clear();
        return action;
    }

    public boolean canSubmit(long nowMillis) {
        return !pending || isExpired(nowMillis);
    }

    public boolean isPending() {
        return pending;
    }

    public long requestId() {
        return requestId;
    }

    private boolean isExpired(long nowMillis) {
        return nowMillis - submittedAtMillis >= WAIT_TIMEOUT_MILLIS;
    }

    private void clear() {
        pending = false;
        failureReceived = false;
        requestId = 0L;
        submittedAtMillis = 0L;
    }
}
