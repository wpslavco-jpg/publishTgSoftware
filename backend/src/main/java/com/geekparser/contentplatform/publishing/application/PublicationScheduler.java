package com.geekparser.contentplatform.publishing.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublicationScheduler {

    private final PublishingService publishingService;

    public PublicationScheduler(PublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @Scheduled(cron = "${app.scheduling.publication-cron}")
    public void publishDueJobs() {
        publishingService.processDueJobs();
    }
}
