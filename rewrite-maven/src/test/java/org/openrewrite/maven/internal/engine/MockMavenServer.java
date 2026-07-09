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
package org.openrewrite.maven.internal.engine;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hermetic synthetic Maven repository over MockWebServer serving pom XML at standard layout paths. Only registered poms
 * exist; everything else 404s. Records requests so tests can count network traffic (proving the warm / known-absent
 * paths are truly zero-I/O).
 */
class MockMavenServer implements AutoCloseable {

    private final MockWebServer server = new MockWebServer();
    private final Map<String, byte[]> pomsByPath = new ConcurrentHashMap<>();
    final List<String> requests = new CopyOnWriteArrayList<>();

    MockMavenServer() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public @NonNull MockResponse dispatch(RecordedRequest request) {
                requests.add(request.getMethod() + " " + request.getPath());
                byte[] body = pomsByPath.get(request.getPath());
                if (body == null) {
                    return new MockResponse().setResponseCode(404);
                }
                MockResponse response = new MockResponse().setResponseCode(200);
                if ("HEAD".equals(request.getMethod())) {
                    return response.setHeader("Content-Length", String.valueOf(body.length));
                }
                return response.setBody(new Buffer().write(body));
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    MockMavenServer pom(String groupId, String artifactId, String version, String xml) {
        pomsByPath.put(path(groupId, artifactId, version), xml.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /** Serve a snapshot pom whose file carries the dated build ({@code artifactId-datedVersion.pom}) under the base-version directory. */
    MockMavenServer snapshotPom(String groupId, String artifactId, String baseVersion, String datedVersion, String xml) {
        String path = "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + baseVersion + "/" +
                artifactId + "-" + datedVersion + ".pom";
        pomsByPath.put(path, xml.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /** Serve {@code maven-metadata.xml} at the group/artifact directory (the GA-level version listing). */
    MockMavenServer metadata(String groupId, String artifactId, String xml) {
        pomsByPath.put("/" + groupId.replace('.', '/') + "/" + artifactId + "/maven-metadata.xml",
                xml.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    private static String path(String groupId, String artifactId, String version) {
        return "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
    }

    String baseUrl() {
        return server.url("/").toString();
    }

    int requestCount() {
        return requests.size();
    }

    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
