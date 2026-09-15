package com.example.logprocessor.worker.lifecycle;

import com.example.logprocessor.worker.processing.LogSearchProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class WorkerLifecycleTest {

    @Test
    void retriesRegistrationAfterCoordinatorUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WorkerLifecycle lifecycle = new WorkerLifecycle(builder, "worker-1", "http://coordinator", new LogSearchProcessor());

        server.expect(requestTo("http://coordinator/workers/register"))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://coordinator/workers/register"))
                .andExpect(method(POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://coordinator/workers/worker-1/heartbeat"))
                .andExpect(method(POST))
                .andRespond(withSuccess());

        lifecycle.register();
        lifecycle.heartbeat();
        lifecycle.retryRegistration();
        lifecycle.heartbeat();
        server.verify();
    }

    @Test
    void reRegistersAfterHeartbeatReturnsNotFound() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WorkerLifecycle lifecycle = new WorkerLifecycle(builder, "worker-1", "http://coordinator", new LogSearchProcessor());

        server.expect(requestTo("http://coordinator/workers/register"))
                .andExpect(method(POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://coordinator/workers/worker-1/heartbeat"))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://coordinator/workers/register"))
                .andExpect(method(POST))
                .andRespond(withSuccess());
        server.expect(requestTo("http://coordinator/workers/worker-1/heartbeat"))
                .andExpect(method(POST))
                .andRespond(withSuccess());

        lifecycle.register();
        lifecycle.heartbeat();
        lifecycle.heartbeat();
        server.verify();
    }
}
