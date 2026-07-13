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
package org.openrewrite.maven.parity.synthetic;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * A MockWebServer posing as a maven repository: path-keyed responders, 404 for anything else.
 * HEAD responses never carry a body (a body on a HEAD response poisons MockWebServer keep-alive).
 */
class MockMavenRepo implements AutoCloseable {
    private final MockWebServer server = new MockWebServer();
    private final Map<String, Function<RecordedRequest, MockResponse>> routes = new ConcurrentHashMap<>();
    private final List<RecordedRequest> recorded = Collections.synchronizedList(new ArrayList<>());

    MockMavenRepo() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                recorded.add(request);
                Function<RecordedRequest, MockResponse> route = request.getPath() == null ? null : routes.get(request.getPath());
                MockResponse response = route == null ? new MockResponse().setResponseCode(404) : route.apply(request);
                if ("HEAD".equalsIgnoreCase(request.getMethod())) {
                    response.setBody("");
                }
                return response;
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    MockMavenRepo serve(String path, String body) {
        return serve(path, request -> new MockResponse().setResponseCode(200).setBody(body));
    }

    MockMavenRepo serve(String path, Function<RecordedRequest, MockResponse> responder) {
        routes.put(path, responder);
        return this;
    }

    /** Uses a literal {@code localhost} authority so {@code SnapshotNormalizer} masks the port. */
    String url() {
        return "http://localhost:" + server.getPort() + "/";
    }

    MavenRepository repo(String id) {
        return MavenRepository.builder().id(id).uri(url()).knownToExist(true).build();
    }

    List<RecordedRequest> recorded() {
        return new ArrayList<>(recorded);
    }

    List<String> requests() {
        return recorded().stream().map(r -> r.getMethod() + " " + r.getPath()).collect(toList());
    }

    /** Requests for artifact/metadata paths, excluding repository normalization probes. */
    List<String> artifactRequests() {
        return requests().stream().filter(r -> r.contains(" /org/")).collect(toList());
    }

    List<RecordedRequest> recordedArtifacts() {
        return recorded().stream()
                .filter(r -> r.getPath() != null && r.getPath().startsWith("/org/"))
                .collect(toList());
    }

    static String pomPath(String groupId, String artifactId, String version, String fileVersion) {
        return "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + fileVersion + ".pom";
    }

    static String pomXml(String groupId, String artifactId, String version) {
        return pomXml(groupId, artifactId, version, "");
    }

    //language=xml
    static String pomXml(String groupId, String artifactId, String version, String extraElements) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>%s</groupId>
              <artifactId>%s</artifactId>
              <version>%s</version>
              %s
          </project>
          """.formatted(groupId, artifactId, version, extraElements);
    }

    @Override
    public void close() {
        try {
            server.shutdown();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
