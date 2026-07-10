package com.suian.xaeroregionsrev.client;

public final class ClientRegionEditResultHandler {
    private static EditResultHandler handler;

    private ClientRegionEditResultHandler() {
    }

    public static void setEditResultHandler(EditResultHandler editResultHandler) {
        handler = editResultHandler;
    }

    public static void handle(long requestId, boolean success, boolean closeScreen, String message) {
        if (handler != null) {
            handler.handleEditResult(requestId, success, closeScreen, message);
        }
    }
}
