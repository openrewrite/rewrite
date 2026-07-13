/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.engine;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Replicates Maven's authenticated&rarr;anonymous fallback (MavenPomDownloader:1112-1138): retry anonymously on a
 * deterministic 4xx; on an access-denied (401-403) anonymous retry rethrow the <em>original</em> exception, otherwise
 * the retry's; without credentials, no fallback.
 */
class HttpSenderTransporterAuthFallbackTest {

    private MockWebServer server;

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                boolean authed = request.getHeader("Authorization") != null;
                switch (request.getPath()) {
                    case "/auth-ok.pom":
                        return authed ? body("<project/>") : new MockResponse().setResponseCode(401);
                    case "/fallback-anon-ok.pom":
                        return authed ? new MockResponse().setResponseCode(404) : body("<anon/>");
                    case "/both-denied.pom":
                        return new MockResponse().setResponseCode(authed ? 401 : 403);
                    case "/anon-server-error.pom":
                        return authed ? new MockResponse().setResponseCode(404) : new MockResponse().setResponseCode(500);
                    default:
                        return new MockResponse().setResponseCode(404);
                }
            }
        });
        server.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    private HttpSenderTransporter transporter(boolean withCredentials) {
        return new HttpSenderTransporter(
                new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5)),
                server.url("/").toString(),
                withCredentials ? "user" : null,
                withCredentials ? "pass" : null,
                Collections.emptyMap(),
                Duration.ofSeconds(5), Duration.ofSeconds(5),
                Collections.emptySet(), ResolutionTimeRecorder.NOOP);
    }

    private String get(HttpSenderTransporter transporter, String path) throws Exception {
        Path out = Files.createTempFile("get", ".pom");
        transporter.get(new GetTask(URI.create(path)).setDataPath(out));
        return new String(Files.readAllBytes(out));
    }

    @Test
    void authenticatedSuccess_noFallback() throws Exception {
        assertEquals("<project/>", get(transporter(true), "auth-ok.pom"));
        assertEquals(1, server.getRequestCount(), "no anonymous fallback on success");
    }

    @Test
    void authenticated4xx_fallsBackToAnonymousSuccess() throws Exception {
        assertEquals("<anon/>", get(transporter(true), "fallback-anon-ok.pom"));
        assertEquals(2, server.getRequestCount(), "authenticated 404 then anonymous 200");
    }

    @Test
    void anonymousRetryAccessDenied_rethrowsOriginal() {
        HttpSenderTransporter.HttpResponseException thrown = assertThrows(
                HttpSenderTransporter.HttpResponseException.class, () -> get(transporter(true), "both-denied.pom"));
        // Original authenticated 401, NOT the anonymous retry's 403.
        assertEquals(401, thrown.statusCode);
    }

    @Test
    void anonymousRetryNonAccessDenied_rethrowsRetry() {
        HttpSenderTransporter transporter = transporter(true);
        HttpSenderTransporter.HttpResponseException thrown = assertThrows(
                HttpSenderTransporter.HttpResponseException.class, () -> get(transporter, "anon-server-error.pom"));
        // The anonymous retry's 500 wins (server error, not access denied); classifies as a transfer error.
        assertEquals(500, thrown.statusCode);
        assertEquals(Transporter.ERROR_OTHER, transporter.classify(thrown));
    }

    @Test
    void noCredentials_noFallback() {
        HttpSenderTransporter transporter = transporter(false);
        HttpSenderTransporter.HttpResponseException thrown = assertThrows(
                HttpSenderTransporter.HttpResponseException.class, () -> get(transporter, "both-denied.pom"));
        assertEquals(403, thrown.statusCode, "no credentials: the single anonymous request's 403 propagates");
        assertEquals(1, server.getRequestCount(), "no fallback attempted without credentials");
        assertEquals(Transporter.ERROR_NOT_FOUND, transporter.classify(thrown));
    }

    private static MockResponse body(String content) {
        return new MockResponse().setResponseCode(200).setBody(content);
    }
}
