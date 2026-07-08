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
package org.openrewrite.maven.parity.corpus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpSender;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class RecordingHttpSenderTest {

    @TempDir
    Path store;

    private static final String URL_A = "https://repo1.maven.org/maven2/com/google/guava/guava/33.0.0-jre/guava-33.0.0-jre.pom";

    private HttpSender stubDelegate(int status, String body, AtomicInteger calls) {
        return request -> {
            calls.incrementAndGet();
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Content-Type", singletonList("text/xml"));
            headers.put("ETag", singletonList("\"abc123\""));
            headers.put("Date", singletonList("now")); // must not be recorded
            return new HttpSender.Response(status,
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), headers, () -> {
            });
        };
    }

    @Test
    void recordThenReplayRoundTrip() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RecordingHttpSender recorder = RecordingHttpSender.record(store, stubDelegate(200, "<project/>", calls));
        try (HttpSender.Response response = recorder.send(recorder.get(URL_A).build())) {
            assertEquals(200, response.getCode());
            assertEquals("<project/>", new String(response.getBodyAsBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(1, calls.get());

        RecordingHttpSender replayer = RecordingHttpSender.replay(store);
        try (HttpSender.Response response = replayer.send(replayer.get(URL_A).build())) {
            assertEquals(200, response.getCode());
            assertEquals("<project/>", new String(response.getBodyAsBytes(), StandardCharsets.UTF_8));
            assertEquals(singletonList("text/xml"), response.getHeaders().get("Content-Type"));
            assertNull(response.getHeaders().get("Date"));
        }
        assertEquals(1, calls.get());
    }

    @Test
    void errorStatusRoundTrips() {
        AtomicInteger calls = new AtomicInteger();
        RecordingHttpSender recorder = RecordingHttpSender.record(store, stubDelegate(404, "not found", calls));
        try (HttpSender.Response response = recorder.send(recorder.get(URL_A).build())) {
            assertEquals(404, response.getCode());
        }
        RecordingHttpSender replayer = RecordingHttpSender.replay(store);
        try (HttpSender.Response response = replayer.send(replayer.get(URL_A).build())) {
            assertEquals(404, response.getCode());
            assertFalse(response.isSuccessful());
        }
    }

    @Test
    void delegateExceptionRoundTrips() {
        RecordingHttpSender recorder = RecordingHttpSender.record(store, request -> {
            throw new UncheckedIOException(new IOException("connection refused"));
        });
        assertThrows(UncheckedIOException.class, () -> recorder.send(recorder.get(URL_A).build()));

        RecordingHttpSender replayer = RecordingHttpSender.replay(store);
        UncheckedIOException replayed =
                assertThrows(UncheckedIOException.class, () -> replayer.send(replayer.get(URL_A).build()));
        assertTrue(replayed.getCause().getMessage().contains("connection refused"));
    }

    @Test
    void replayMissThrowsWithMissingUrl() {
        RecordingHttpSender replayer = RecordingHttpSender.replay(store);
        IllegalStateException miss =
                assertThrows(IllegalStateException.class, () -> replayer.send(replayer.get(URL_A).build()));
        assertTrue(miss.getMessage().contains("guava-33.0.0-jre.pom"), miss.getMessage());
        assertTrue(miss.getMessage().contains("GET"), miss.getMessage());
    }

    @Test
    void recordIsReadThrough() {
        AtomicInteger calls = new AtomicInteger();
        RecordingHttpSender recorder = RecordingHttpSender.record(store, stubDelegate(200, "body", calls));
        recorder.send(recorder.get(URL_A).build()).close();
        recorder.send(recorder.get(URL_A).build()).close();
        assertEquals(1, calls.get(), "second RECORD send must be served from the store");
    }

    @Test
    void storeIsDeterministic() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RecordingHttpSender first = RecordingHttpSender.record(store, stubDelegate(200, "same body", calls));
        first.send(first.get(URL_A).build()).close();
        Map<Path, byte[]> before = storeContents();

        deleteStore();
        RecordingHttpSender second = RecordingHttpSender.record(store, stubDelegate(200, "same body", calls));
        second.send(second.get(URL_A).build()).close();
        Map<Path, byte[]> after = storeContents();

        assertEquals(before.keySet(), after.keySet());
        before.forEach((path, bytes) -> assertArrayEquals(bytes, after.get(path), "differs: " + path));
    }

    @Test
    void storeLayoutIsTwoCharPrefixed() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RecordingHttpSender recorder = RecordingHttpSender.record(store, stubDelegate(200, "x", calls));
        recorder.send(recorder.get(URL_A).build()).close();
        try (Stream<Path> entries = Files.list(store)) {
            Path prefix = entries.findFirst().orElseThrow();
            assertEquals(2, prefix.getFileName().toString().length());
            try (Stream<Path> hashes = Files.list(prefix)) {
                Path hashDir = hashes.findFirst().orElseThrow();
                assertEquals(64, hashDir.getFileName().toString().length());
                assertTrue(hashDir.getFileName().toString().startsWith(prefix.getFileName().toString()));
                assertTrue(Files.exists(hashDir.resolve("meta.json")));
                assertTrue(Files.exists(hashDir.resolve("body.bin")));
            }
        }
    }

    @Test
    void canonicalizationSortsQueryParameters() throws Exception {
        assertEquals(
                RecordingHttpSender.canonicalUrl(new URL("https://example.com/search?b=2&a=1")),
                RecordingHttpSender.canonicalUrl(new URL("https://example.com/search?a=1&b=2")));
    }

    @Test
    void canonicalizationStripsTrailingSlashAndDefaultPort() throws Exception {
        assertEquals(
                RecordingHttpSender.canonicalUrl(new URL("https://example.com:443/maven2/")),
                RecordingHttpSender.canonicalUrl(new URL("https://example.com/maven2")));
        assertEquals("https://example.com/", RecordingHttpSender.canonicalUrl(new URL("HTTPS://EXAMPLE.COM")));
        assertNotEquals(
                RecordingHttpSender.canonicalUrl(new URL("https://example.com:8443/maven2")),
                RecordingHttpSender.canonicalUrl(new URL("https://example.com/maven2")));
    }

    @Test
    void canonicalizationCollapsesDuplicateSlashes() throws Exception {
        assertEquals(
                RecordingHttpSender.canonicalUrl(new URL("https://example.com/maven2//com/google/guava")),
                RecordingHttpSender.canonicalUrl(new URL("https://example.com/maven2/com/google/guava")));
    }

    @Test
    void equivalentUrlsShareOneStoreEntry() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RecordingHttpSender recorder = RecordingHttpSender.record(store, stubDelegate(200, "x", calls));
        recorder.send(recorder.get("https://example.com/search?b=2&a=1").build()).close();
        recorder.send(recorder.get("https://example.com:443/search?a=1&b=2").build()).close();
        assertEquals(1, calls.get());
        try (Stream<Path> entries = Files.walk(store)) {
            assertEquals(1, entries.filter(p -> p.getFileName().toString().equals("meta.json")).count());
        }
    }

    private Map<Path, byte[]> storeContents() throws IOException {
        Map<Path, byte[]> contents = new HashMap<>();
        try (Stream<Path> walk = Files.walk(store)) {
            for (Path path : walk.filter(Files::isRegularFile).toList()) {
                contents.put(store.relativize(path), Files.readAllBytes(path));
            }
        }
        return contents;
    }

    private void deleteStore() throws IOException {
        try (Stream<Path> walk = Files.walk(store)) {
            walk.sorted((a, b) -> b.compareTo(a))
                    .filter(p -> !p.equals(store))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
}
