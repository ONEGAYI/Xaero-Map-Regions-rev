package com.suian.xaeroregionsrev.client;

/**
 * 编辑结果回调接口。
 * 平台子项目注入实现，检查当前 Screen 类型并转发编辑结果。
 */
public interface EditResultHandler {
    /**
     * 处理编辑结果，如果当前屏幕是编辑器则转发结果。
     * @param requestId 请求 ID
     * @param success 是否成功
     * @param closeScreen 是否应关闭编辑器屏幕（success && closeScreen 才真正关屏）
     * @param message 结果消息
     */
    void handleEditResult(long requestId, boolean success, boolean closeScreen, String message);
}
