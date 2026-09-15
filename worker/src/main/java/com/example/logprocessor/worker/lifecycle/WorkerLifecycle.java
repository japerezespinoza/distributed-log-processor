package com.example.logprocessor.worker.lifecycle;

import com.example.logprocessor.common.CompletionRequest;
import com.example.logprocessor.common.FailureRequest;
import com.example.logprocessor.common.LogJob;
import com.example.logprocessor.worker.processing.LogSearchProcessor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WorkerLifecycle.class);

    private final RestClient coordinatorClient;
    private final String workerId;
    private final LogSearchProcessor logSearchProcessor;
    private final AtomicBoolean processing = new AtomicBoolean();
    private final AtomicBoolean registered = new AtomicBoolean();

    public WorkerLifecycle(
            RestClient.Builder restClientBuilder,
            @Value("${worker.id}") String workerId,
            @Value("${coordinator.url}") String coordinatorUrl,
            LogSearchProcessor logSearchProcessor) {
        this.coordinatorClient = restClientBuilder.baseUrl(coordinatorUrl).build();
        this.workerId = workerId;
        this.logSearchProcessor = logSearchProcessor;
    }

    @PostConstruct
    public synchronized void register() {
        if (registered.get()) {
            return;
        }

        try {
            coordinatorClient.post()
                    .uri("/workers/register")
                    .body(Map.of("workerId", workerId))
                    .retrieve()
                    .toBodilessEntity();
            registered.set(true);
            log.info("Worker {} registered with coordinator", workerId);
        } catch (RestClientException exception) {
            log.warn("Worker {} registration failed", workerId, exception);
        }
    }


    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void retryRegistration() {
        register();
    }

    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void claimAndProcessJob() {
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        try {
            ResponseEntity<LogJob> response = coordinatorClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/jobs/claim")
                            .queryParam("workerId", workerId)
                            .build())
                    .retrieve()
                    .toEntity(LogJob.class);

            LogJob job = response.getBody();
            if (response.getStatusCode().value() == 204 || job == null) {
                return;
            }

            final long matchCount;
            try {
                matchCount = logSearchProcessor.countMatches(job);
            } catch (Exception exception) {
                log.error("Worker {} failed to process job {}", workerId, job.id(), exception);
                reportFailure(job, exception);
                return;
            }

            coordinatorClient.post()
                    .uri("/jobs/{jobId}/complete", job.id())
                    .body(new CompletionRequest(workerId, matchCount))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Worker {} completed job {} with {} matches", workerId, job.id(), matchCount);
        } catch (Exception exception) {
            log.error("Worker {} failed while claiming or completing a job", workerId, exception);
        } finally {
            processing.set(false);
        }
    }

    private void reportFailure(LogJob job, Exception exception) {
        String errorMessage = exception.getMessage();
        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = exception.getClass().getSimpleName();
        }
        try {
            coordinatorClient.post()
                    .uri("/jobs/{jobId}/fail", job.id())
                    .body(new FailureRequest(workerId, errorMessage))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException failureException) {
            log.error("Worker {} could not report failure for job {}", workerId, job.id(), failureException);
        }
    }

    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void heartbeat() {
        if (!registered.get()) {
            return;
        }

        try {
            coordinatorClient.post()
                    .uri("/workers/{workerId}/heartbeat", workerId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Worker {} heartbeat sent", workerId);
        } catch (HttpClientErrorException.NotFound exception) {
            registered.set(false);
            log.warn("Worker {} is no longer registered; attempting registration", workerId);
            register();
        } catch (RestClientException exception) {
            log.warn("Worker {} heartbeat failed", workerId, exception);
        }
    }
}