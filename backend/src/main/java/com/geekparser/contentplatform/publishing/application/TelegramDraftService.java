package com.geekparser.contentplatform.publishing.application;

import com.geekparser.contentplatform.article.domain.PreparedArticle;
import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramDraftService {

    private final PreparedArticleRepository preparedArticleRepository;
    private final TelegramSettingsService telegramSettingsService;
    private final TelegramGateway telegramGateway;

    public TelegramDraftService(PreparedArticleRepository preparedArticleRepository,
                                TelegramSettingsService telegramSettingsService,
                                TelegramGateway telegramGateway) {
        this.preparedArticleRepository = preparedArticleRepository;
        this.telegramSettingsService = telegramSettingsService;
        this.telegramGateway = telegramGateway;
    }

    @Transactional(readOnly = true)
    public DraftSendResult sendDraft(Long preparedArticleId) {
        TelegramSettingsService.TelegramCredentials credentials = telegramSettingsService.requireActiveCredentials();
        String draftChatId = telegramSettingsService.requireDraftChatId();

        PreparedArticle article = preparedArticleRepository.findById(preparedArticleId)
                .orElseThrow(() -> new IllegalArgumentException("Prepared article not found"));

        String text = TelegramMessageFormatter.formatDraftMessage(
                article.getId(),
                article.getTitle(),
                article.getSummaryBody(),
                article.getRawArticle().getCanonicalUrl()
        );

        String messageId = telegramGateway.sendMessage(
                credentials.botToken(),
                draftChatId,
                text,
                TelegramMessageFormatter.draftParseMode(),
                true
        );

        return new DraftSendResult(messageId, draftChatId);
    }

    public record DraftSendResult(String messageId, String draftChatId) {
    }
}
