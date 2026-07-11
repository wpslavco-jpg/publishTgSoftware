package com.geekparser.contentplatform.article.application;

import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import com.geekparser.contentplatform.article.domain.RawArticle;
import com.geekparser.contentplatform.article.domain.RawArticleRepository;
import com.geekparser.contentplatform.publishing.domain.PublicationJobRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleStorageCleanupService {

    private final PublicationJobRepository publicationJobRepository;
    private final PreparedArticleRepository preparedArticleRepository;
    private final RawArticleRepository rawArticleRepository;
    private final MarkdownStorageService markdownStorageService;

    public ArticleStorageCleanupService(PublicationJobRepository publicationJobRepository,
                                        PreparedArticleRepository preparedArticleRepository,
                                        RawArticleRepository rawArticleRepository,
                                        MarkdownStorageService markdownStorageService) {
        this.publicationJobRepository = publicationJobRepository;
        this.preparedArticleRepository = preparedArticleRepository;
        this.rawArticleRepository = rawArticleRepository;
        this.markdownStorageService = markdownStorageService;
    }

    @Transactional
    public ClearArticlesResult clearAllArticles() {
        List<String> markdownPaths = rawArticleRepository.findAllMarkdownPaths();
        long deletedPublications = publicationJobRepository.count();
        publicationJobRepository.deleteAllInBatch();
        long deletedPrepared = preparedArticleRepository.count();
        preparedArticleRepository.deleteAllInBatch();
        long deletedRaw = rawArticleRepository.count();
        rawArticleRepository.deleteAllInBatch();
        int deletedFiles = markdownStorageService.clearRawStorageDirectory();
        if (deletedFiles < markdownPaths.size()) {
            deletedFiles = Math.max(deletedFiles, markdownStorageService.deleteFiles(markdownPaths));
        }
        return new ClearArticlesResult(deletedRaw, deletedPrepared, deletedPublications, deletedFiles);
    }

    @Transactional
    public DeleteSourceArticlesResult deleteArticlesForSource(Long sourceId) {
        List<RawArticle> rawArticles = rawArticleRepository.findBySourceId(sourceId);
        List<String> markdownPaths = rawArticles.stream()
                .map(RawArticle::getMarkdownPath)
                .toList();
        List<Long> preparedIds = preparedArticleRepository.findIdsBySourceId(sourceId);
        if (!preparedIds.isEmpty()) {
            publicationJobRepository.deleteByPreparedArticleIdIn(preparedIds);
            preparedArticleRepository.deleteAllByIdInBatch(preparedIds);
        }
        long deletedRaw = rawArticles.size();
        if (!rawArticles.isEmpty()) {
            rawArticleRepository.deleteAllInBatch(rawArticles);
        }
        int deletedFiles = markdownStorageService.deleteFiles(markdownPaths);
        return new DeleteSourceArticlesResult(deletedRaw, preparedIds.size(), deletedFiles);
    }

    public record ClearArticlesResult(
            long deletedRawArticles,
            long deletedPreparedArticles,
            long deletedPublications,
            int deletedMarkdownFiles
    ) {
    }

    public record DeleteSourceArticlesResult(
            long deletedRawArticles,
            long deletedPreparedArticles,
            int deletedMarkdownFiles
    ) {
    }
}
