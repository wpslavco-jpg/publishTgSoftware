package com.geekparser.contentplatform.admin.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/sources/sync")
    public AdminDtos.SyncResponse syncSources() {
        return adminService.syncSources();
    }

    @GetMapping("/sources")
    public List<AdminDtos.SourceResponse> listSources() {
        return adminService.listSources();
    }

    @PostMapping("/sources")
    public AdminDtos.SourceResponse createSource(@Valid @RequestBody AdminDtos.CreateSourceRequest request) {
        return adminService.createSource(request);
    }

    @PutMapping("/sources/{id}")
    public AdminDtos.SourceResponse updateSource(@PathVariable Long id,
                                                 @Valid @RequestBody AdminDtos.UpdateSourceRequest request) {
        return adminService.updateSource(id, request);
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id,
                                             @RequestParam(defaultValue = "false") boolean deleteArticles) {
        adminService.deleteSource(id, deleteArticles);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/storage/clear-articles")
    public AdminDtos.ClearArticlesResponse clearArticles() {
        return adminService.clearArticles();
    }

    @GetMapping("/articles")
    public List<AdminDtos.ArticleSummaryResponse> listArticles() {
        return adminService.listArticles();
    }

    @GetMapping("/articles/{id}")
    public AdminDtos.ArticleDetailsResponse getArticle(@PathVariable Long id) {
        return adminService.getArticle(id);
    }

    @PutMapping("/articles/{id}")
    public AdminDtos.ArticleDetailsResponse updateArticle(@PathVariable Long id,
                                                          @Valid @RequestBody AdminDtos.UpdateArticleRequest request) {
        return adminService.updateArticle(id, request);
    }

    @PostMapping("/articles/{id}/schedule")
    public AdminDtos.PublicationResponse schedule(@PathVariable Long id,
                                                  @Valid @RequestBody AdminDtos.SchedulePublicationRequest request) {
        return adminService.schedule(id, request);
    }

    @PostMapping("/articles/{id}/publish-now")
    public AdminDtos.PublicationResponse publishNow(@PathVariable Long id) {
        return adminService.publishNow(id);
    }

    @GetMapping("/publications")
    public List<AdminDtos.PublicationResponse> listPublications() {
        return adminService.listPublications();
    }

    @PostMapping("/articles/{id}/send-draft")
    public AdminDtos.SendDraftResponse sendDraft(@PathVariable Long id) {
        return adminService.sendDraft(id);
    }

    @PostMapping("/telegram/discover-chats")
    public List<AdminDtos.DiscoveredChatResponse> discoverChats(
            @RequestBody(required = false) AdminDtos.DiscoverChatsRequest request) {
        String botToken = request != null && request.botToken() != null && !request.botToken().isBlank()
                ? request.botToken()
                : adminService.requireBotTokenForDiscovery();
        return adminService.discoverTelegramChats(botToken);
    }

    @GetMapping("/telegram/config")
    public ResponseEntity<AdminDtos.TelegramConfigResponse> getTelegramConfig() {
        AdminDtos.TelegramConfigResponse config = adminService.getTelegramConfig();
        if (config == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(config);
    }

    @PostMapping("/telegram/validate")
    public AdminDtos.TelegramValidationResponse validateTelegram(@Valid @RequestBody AdminDtos.TelegramConfigRequest request) {
        return adminService.validateTelegram(request);
    }

    @PostMapping("/telegram/test-message")
    public ResponseEntity<Void> sendTestMessage(@Valid @RequestBody AdminDtos.TelegramConfigRequest request) {
        adminService.sendTelegramTestMessage(request);
        return ResponseEntity.noContent().build();
    }
}
