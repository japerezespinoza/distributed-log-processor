package com.example.logprocessor.worker.processing;

import com.example.logprocessor.common.JobStatus;
import com.example.logprocessor.common.LogJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSearchProcessorTest {

    private final LogSearchProcessor processor = new LogSearchProcessor();

    @Test
    void countsLinesContainingPattern(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("application.log");
        Files.writeString(logFile, "INFO started\nERROR failed\nINFO recovered ERROR\n");
        LogJob job = new LogJob("job-1", logFile.toString(), "ERROR", Instant.now(), JobStatus.RUNNING);

        assertEquals(2, processor.countMatches(job));
    }
}
