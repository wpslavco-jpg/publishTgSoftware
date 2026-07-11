package com.geekparser.contentplatform.publishing.application;

import com.geekparser.contentplatform.config.AppProperties;
import com.geekparser.contentplatform.publishing.domain.TelegramConfig;
import com.geekparser.contentplatform.publishing.domain.TelegramConfigRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TelegramSettingsService {

    private final AppProperties appProperties;
    private final TelegramConfigRepository telegramConfigRepository;

    public TelegramSettingsService(AppProperties appProperties,
                                   TelegramConfigRepository telegramConfigRepository) {
        this.appProperties = appProperties;
        this.telegramConfigRepository = telegramConfigRepository;
    }

    public boolean isConfiguredViaEnv() {
        return hasEnvCredentials();
    }

    public Optional<TelegramCredentials> findActiveCredentials() {
        if (hasEnvCredentials()) {
            return Optional.of(fromEnv());
        }
        return telegramConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .filter(TelegramConfig::isValidated)
                .map(this::fromEntity);
    }

    public TelegramCredentials requireActiveCredentials() {
        return findActiveCredentials()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Telegram is not configured. Set APP_TELEGRAM_BOT_TOKEN and APP_TELEGRAM_CHAT_ID in .env "
                                + "or validate the bot in the admin panel."
                ));
    }

    public Optional<TelegramCredentials> findCredentialsForAdmin() {
        if (hasEnvCredentials()) {
            return Optional.of(fromEnv());
        }
        return telegramConfigRepository.findAll().stream()
                .max(java.util.Comparator.comparing(TelegramConfig::getUpdatedAt))
                .map(this::fromEntity);
    }

    public boolean isDraftChatConfigured() {
        return findDraftChatId().isPresent();
    }

    public Optional<String> findDraftChatId() {
        AppProperties.Telegram telegram = appProperties.telegram();
        if (isNotBlank(telegram.draftChatId())) {
            return Optional.of(telegram.draftChatId().trim());
        }
        return Optional.empty();
    }

    public String requireDraftChatId() {
        return findDraftChatId().orElseThrow(() -> new IllegalArgumentException(
                "Draft chat is not configured. Send /start to your bot in Telegram, "
                        + "click «Найти chat ID» in admin, then set APP_TELEGRAM_DRAFT_CHAT_ID in .env."
        ));
    }

    public String maskDraftChatId() {
        return findDraftChatId()
                .map(TelegramSettingsService::maskChatId)
                .orElse(null);
    }

    private static String maskChatId(String chatId) {
        if (chatId.length() <= 4) {
            return chatId;
        }
        return "*".repeat(Math.max(0, chatId.length() - 4)) + chatId.substring(chatId.length() - 4);
    }

    private boolean hasEnvCredentials() {
        AppProperties.Telegram telegram = appProperties.telegram();
        return isNotBlank(telegram.botToken()) && isNotBlank(telegram.chatId());
    }

    private TelegramCredentials fromEnv() {
        AppProperties.Telegram telegram = appProperties.telegram();
        return new TelegramCredentials(
                telegram.botToken().trim(),
                telegram.chatId().trim(),
                blankToNull(telegram.botUsername()),
                true,
                true
        );
    }

    private TelegramCredentials fromEntity(TelegramConfig config) {
        return new TelegramCredentials(
                config.getBotToken(),
                config.getChatId(),
                blankToNull(config.getBotUsername()),
                config.isValidated(),
                false
        );
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }

    public record TelegramCredentials(
            String botToken,
            String chatId,
            String botUsername,
            boolean validated,
            boolean fromEnv
    ) {
    }
}
