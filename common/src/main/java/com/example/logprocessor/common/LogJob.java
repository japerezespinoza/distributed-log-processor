package com.example.logprocessor.common;

import java.time.Instant;

/** Shared representation of a log-processing job. Coordination is intentionally out of scope. */
public record LogJob(String id, String source, Instant submittedAt, JobStatus status) {
}
