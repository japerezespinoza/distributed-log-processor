package com.example.logprocessor.coordinator.persistence;

import com.example.logprocessor.common.JobStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    private String id;
    private String source;
    private String pattern;
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private String workerId;
    private Instant leaseUntil;
    private Long matchCount;
    private int attemptCount = 0;
    private int maxAttempts = 3;
    private String lastError;

    protected JobEntity() {
    }

    public JobEntity(String id, String source, String pattern, Instant submittedAt, JobStatus status) {
        this(id, source, pattern, submittedAt, status, null);
    }

    public JobEntity(String id, String source, String pattern, Instant submittedAt, JobStatus status, String workerId) {
        this.id = id;
        this.source = source;
        this.pattern = pattern;
        this.submittedAt = submittedAt;
        this.status = status;
        this.workerId = workerId;
        this.attemptCount = 0;
        this.maxAttempts = 3;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public String getPattern() { return pattern; }
    public Instant getSubmittedAt() { return submittedAt; }
    public JobStatus getStatus() { return status; }
    public String getWorkerId() { return workerId; }
    public Instant getLeaseUntil() { return leaseUntil; }
    public Long getMatchCount() { return matchCount; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }

    public void markRunning(String workerId) {
        this.status = JobStatus.RUNNING;
        this.workerId = workerId;
        this.attemptCount++;
        this.leaseUntil = Instant.now().plusSeconds(20);
    }

    public void complete(long matchCount) {
        this.status = JobStatus.COMPLETED;
        this.leaseUntil = null;
        this.matchCount = matchCount;
    }

    public void fail(String errorMessage) {
        this.lastError = errorMessage;
        this.workerId = null;
        this.leaseUntil = null;
        this.status = attemptCount < maxAttempts ? JobStatus.QUEUED : JobStatus.FAILED;
    }

    public void requeueExpiredJob() {
        this.status = JobStatus.QUEUED;
        this.workerId = null;
        this.leaseUntil = null;
    }
}
