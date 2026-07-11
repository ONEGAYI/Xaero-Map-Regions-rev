package com.suian.xaeroregionsrev.client.editor;

import java.util.function.ToIntFunction;

/**
 * 选中 HUD 的显示文本计算。纯计算逻辑，不依赖 Minecraft API，可单元测试。
 */
public record SelectionHudText(String displayText, String fullText, boolean truncated) {

    private static final String ELLIPSIS = "...";
    private static final String LAYER_SEPARATOR = "  ";

    /**
     * 构造 HUD 显示文本。
     *
     * @param label     区域名称
     * @param index     当前层（从 1 开始）
     * @param total     总层数
     * @param textWidth 文本宽度计算函数（对应 Font::width，String → 像素宽度）
     * @param maxWidth  最大允许宽度（像素）
     */
    public static SelectionHudText of(String label, int index, int total,
                                       ToIntFunction<String> textWidth, int maxWidth) {
        String prefix = total >= 2 ? index + "/" + total + LAYER_SEPARATOR : "";
        String fullText = prefix + label;

        if (textWidth.applyAsInt(fullText) <= maxWidth) {
            return new SelectionHudText(fullText, fullText, false);
        }

        // 截断 label 部分，保留 prefix 完整
        int ellipsisWidth = textWidth.applyAsInt(ELLIPSIS);
        String truncatedLabel = truncateToWidth(label, maxWidth - textWidth.applyAsInt(prefix) - ellipsisWidth, textWidth);
        String displayText = prefix + truncatedLabel + ELLIPSIS;
        return new SelectionHudText(displayText, fullText, true);
    }

    private static String truncateToWidth(String text, int availableWidth, ToIntFunction<String> textWidth) {
        if (availableWidth <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (textWidth.applyAsInt(sb.toString()) > availableWidth) {
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
        }
        return sb.toString();
    }
}
