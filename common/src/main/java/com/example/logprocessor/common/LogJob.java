package com.example.logprocessor.common;

import java.time.Instant;

/** Shared representation of a log-processing job. Coordination is intentionally out of scope. */
public record LogJob(
        String id, String source, String pattern, Instant submittedAt, JobStatus status,
        String workerId, Instant leaseUntil, Long matchCount, int attemptCount, int maxAttempts, String lastError) {

    public LogJob(String id, String source, String pattern, Instant submittedAt, JobStatus status) {
        this(id, source, pattern, submittedAt, status, null, null, null, 0, 3, null);
    }

    public LogJob(String id, String source, String pattern, Instant submittedAt, JobStatus status, String workerId) {
        this(id, source, pattern, submittedAt, status, workerId, null, null, 0, 3, null);
    }

    public LogJob(String id, String source, String pattern, Instant submittedAt, JobStatus status,
                  String workerId, Instant leaseUntil, Long matchCount) {
        this(id, source, pattern, submittedAt, status, workerId, leaseUntil, matchCount, 0, 3, null);
    }
}
