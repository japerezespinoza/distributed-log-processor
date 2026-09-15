package com.example.logprocessor.common;

/** Input contract for creating a log-processing job. */
public record CreateJobRequest(String source, String pattern) {
}
