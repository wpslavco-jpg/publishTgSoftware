package com.geekparser.contentplatform.publishing.application;

public final class TelegramMessageFormatter {

    private TelegramMessageFormatter() {
    }

    public static String formatPost(String title, String summaryBody) {
        String safeTitle = title == null ? "" : title.trim();
        String body = summaryBody == null ? "" : summaryBody.trim();
        if (safeTitle.isEmpty()) {
            return body;
        }
        if (looksLikeTelegramHtml(body)) {
            return "<b>" + escapeHtml(safeTitle) + "</b>\n\n" + body;
        }
        return safeTitle + "\n\n" + body;
    }

    public static String resolveParseMode(String summaryBody) {
        return looksLikeTelegramHtml(summaryBody) ? "HTML" : null;
    }

    public static String formatDraftMessage(Long articleId, String title, String summaryBody, String sourceUrl) {
        String safeTitle = title == null ? "" : title.trim();
        String body = summaryBody == null ? "" : summaryBody.trim();
        StringBuilder message = new StringBuilder();
        message.append("📝 <b>Черновик #").append(articleId).append("</b>");
        if (!safeTitle.isEmpty()) {
            message.append("\n\n<b>").append(escapeHtml(safeTitle)).append("</b>");
        }
        if (!body.isEmpty()) {
            message.append("\n\n").append(body);
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            message.append("\n\n<i>Источник: </i><a href=\"")
                    .append(escapeHtml(sourceUrl.trim()))
                    .append("\">")
                    .append(escapeHtml(sourceUrl.trim()))
                    .append("</a>");
        }
        message.append("\n\n<i>Отредактируйте в Telegram и опубликуйте из админки.</i>");
        return message.toString();
    }

    public static String draftParseMode() {
        return "HTML";
    }

    static boolean looksLikeTelegramHtml(String text) {
        return text != null && text.matches("(?s).*<(b|i|u|s|code|pre|a)\\b.*");
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
