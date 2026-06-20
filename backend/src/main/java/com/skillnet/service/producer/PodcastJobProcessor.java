package com.skillnet.service.producer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PodcastJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(PodcastJobProcessor.class);

    private final PodcastGenerationService podcastGenerationService;

    @Async
    public void runAsync(Long jobId) {
        try {
            podcastGenerationService.processJob(jobId);
        } catch (Exception ex) {
            log.error("Podcast background job failed jobId={}", jobId, ex);
            podcastGenerationService.markJobFailed(jobId, ex);
        }
    }
}
