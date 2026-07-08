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
import okio.Buffer;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hermetic synthetic Maven repository over MockWebServer: {@code com.example:app:1 -> lib-a:1 -> lib-b:1}. Only POMs
 * exist; anything else 404s. HEAD responses carry no body (spike lesson: a body on HEAD poisons keep-alive).
 */
class TinyMavenRepo implements AutoCloseable {

    private final MockWebServer server = new MockWebServer();
    private final Map<String, byte[]> content = new HashMap<>();
    final List<String> requested = new CopyOnWriteArrayList<>();

    TinyMavenRepo() throws IOException {
        addPom("com.example", "app", "1", dep("com.example", "lib-a", "1"));
        addPom("com.example", "lib-a", "1", dep("com.example", "lib-b", "1"));
        addPom("com.example", "lib-b", "1");

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                requested.add(request.getMethod() + " " + path);
                byte[] body = content.get(path);
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

    /** Checksum policy IGNORE so collect fetches POMs only (no .sha1/.md5 chatter). */
    RemoteRepository remoteRepository() {
        return new RemoteRepository.Builder("mock", "default", baseUrl())
                .setPolicy(new RepositoryPolicy(
                        true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
                .build();
    }

    String baseUrl() {
        return server.url("/").toString();
    }

    int serverRequestCount() {
        return server.getRequestCount();
    }

    boolean anyJarRequested() {
        return requested.stream().anyMatch(r -> r.endsWith(".jar"));
    }

    boolean anyPrefixesTxtRequested() {
        return requested.stream().anyMatch(r -> r.contains(".meta/prefixes.txt") || r.contains("prefixes.txt"));
    }

    private void addPom(String group, String artifact, String version, String... deps) {
        String path = "/" + group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".pom";
        content.put(path, pom(group, artifact, version, deps).getBytes(StandardCharsets.UTF_8));
    }

    private static String dep(String group, String artifact, String version) {
        return "<dependency><groupId>" + group + "</groupId><artifactId>" + artifact + "</artifactId><version>"
                + version + "</version></dependency>";
    }

    private static String pom(String group, String artifact, String version, String... deps) {
        StringBuilder sb = new StringBuilder();
        sb.append("<project><modelVersion>4.0.0</modelVersion>");
        sb.append("<groupId>").append(group).append("</groupId>");
        sb.append("<artifactId>").append(artifact).append("</artifactId>");
        sb.append("<version>").append(version).append("</version>");
        if (deps.length > 0) {
            sb.append("<dependencies>");
            for (String d : deps) {
                sb.append(d);
            }
            sb.append("</dependencies>");
        }
        sb.append("</project>");
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
