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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wraps a delegate {@link HttpSender}, persisting every exchange under a store directory keyed by
 * {@code sha256(method + "\n" + canonical-url)} so that later runs can be served entirely from the
 * store. REPLAY mode has no delegate at all; a store miss is a hard failure carrying the missing
 * URL, which is what makes replay runs provably hermetic.
 */
public class RecordingHttpSender implements HttpSender {
    public enum Mode {
        RECORD, REPLAY
    }

    private static final List<String> RECORDED_HEADERS = Arrays.asList(
            "Content-Type", "Content-Length", "ETag", "Last-Modified", "Location");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Mode mode;
    private final Path store;

    private final @Nullable HttpSender delegate;

    private RecordingHttpSender(Mode mode, Path store, @Nullable HttpSender delegate) {
        this.mode = mode;
        this.store = store;
        this.delegate = delegate;
    }

    /**
     * RECORD is read-through: a store hit is served from the store without touching the network,
     * so re-recording is incremental. A deliberate refresh means deleting the store first.
     */
    public static RecordingHttpSender record(Path store, HttpSender delegate) {
        return new RecordingHttpSender(Mode.RECORD, store, delegate);
    }

    public static RecordingHttpSender replay(Path store) {
        return new RecordingHttpSender(Mode.REPLAY, store, null);
    }

    @Override
    public Response send(Request request) {
        String canonicalUrl = canonicalUrl(request.getUrl());
        String method = request.getMethod().name();
        Path dir = entryDir(method, canonicalUrl);
        if (Files.exists(dir.resolve("meta.json"))) {
            return fromStore(dir);
        }
        if (mode == Mode.REPLAY) {
            throw new IllegalStateException("Replay miss (hermeticity violation): " + method + " " + canonicalUrl +
                                            " is not in the store at " + store);
        }
        return recordExchange(request, method, canonicalUrl, dir);
    }

    private Response recordExchange(Request request, String method, String canonicalUrl, Path dir) {
        //noinspection DataFlowIssue
        try (Response response = delegate.send(request)) {
            int code = response.getCode();
            Map<String, String> headers = headerSubset(response.getHeaders());
            byte[] body = readAll(response.getBody());
            persist(dir, responseMeta(method, canonicalUrl, code, headers), body);
            return new Response(code, new ByteArrayInputStream(body), replayHeaders(headers), () -> {
            });
        } catch (UncheckedIOException e) {
            persist(dir, exceptionMeta(method, canonicalUrl, e.getCause()), null);
            throw e;
        }
    }

    private Response fromStore(Path dir) {
        try {
            JsonNode meta = MAPPER.readTree(Files.readAllBytes(dir.resolve("meta.json")));
            if (meta.has("exception")) {
                throw new UncheckedIOException(new IOException(meta.get("exception").get("message").asText()));
            }
            Path bodyFile = dir.resolve("body.bin");
            byte[] body = Files.exists(bodyFile) ? Files.readAllBytes(bodyFile) : new byte[0];
            Map<String, String> headers = new LinkedHashMap<>();
            meta.get("headers").fields().forEachRemaining(e -> headers.put(e.getKey(), e.getValue().asText()));
            return new Response(meta.get("status").asInt(), new ByteArrayInputStream(body), replayHeaders(headers), () -> {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ObjectNode responseMeta(String method, String canonicalUrl, int code, Map<String, String> headers) {
        ObjectNode meta = MAPPER.createObjectNode();
        meta.put("method", method);
        meta.put("url", canonicalUrl);
        meta.put("status", code);
        ObjectNode headerNode = meta.putObject("headers");
        headers.forEach(headerNode::put);
        return meta;
    }

    private ObjectNode exceptionMeta(String method, String canonicalUrl, Throwable cause) {
        ObjectNode meta = MAPPER.createObjectNode();
        meta.put("method", method);
        meta.put("url", canonicalUrl);
        ObjectNode exception = meta.putObject("exception");
        exception.put("type", cause.getClass().getName());
        exception.put("message", cause.getMessage() == null ? "" : cause.getMessage());
        return meta;
    }

    private void persist(Path dir, ObjectNode meta, byte @Nullable [] body) {
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve("meta.json"),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(meta));
            if (body != null) {
                Files.write(dir.resolve("body.bin"), body);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path entryDir(String method, String canonicalUrl) {
        String hash = sha256Hex(method + "\n" + canonicalUrl);
        return store.resolve(hash.substring(0, 2)).resolve(hash);
    }

    /**
     * Deterministic sorted headers so recording the same exchange twice produces identical bytes.
     */
    private static Map<String, String> headerSubset(Map<String, List<String>> raw) {
        Map<String, String> subset = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : raw.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            for (String recorded : RECORDED_HEADERS) {
                if (recorded.equalsIgnoreCase(e.getKey())) {
                    subset.put(recorded, String.join(",", e.getValue()));
                }
            }
        }
        return subset;
    }

    private static Map<String, List<String>> replayHeaders(Map<String, String> subset) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        subset.forEach((k, v) -> headers.put(k, Arrays.asList(v.split(","))));
        return headers;
    }

    /**
     * Canonicalization: lower-cased scheme and host, default ports dropped, duplicate slashes in
     * the path collapsed, trailing slash stripped (except the root path), query parameters sorted,
     * fragment dropped.
     */
    public static String canonicalUrl(URL url) {
        String scheme = url.getProtocol().toLowerCase(Locale.ROOT);
        String host = url.getHost().toLowerCase(Locale.ROOT);
        int port = url.getPort();
        boolean defaultPort = port == -1 ||
                              ("http".equals(scheme) && port == 80) ||
                              ("https".equals(scheme) && port == 443);
        String path = url.getPath().replaceAll("/{2,}", "/");
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            path = "/";
        }
        String query = url.getQuery();
        StringBuilder canonical = new StringBuilder(scheme).append("://").append(host);
        if (!defaultPort) {
            canonical.append(':').append(port);
        }
        canonical.append(path);
        if (query != null && !query.isEmpty()) {
            String[] params = query.split("&");
            Arrays.sort(params);
            canonical.append('?').append(String.join("&", params));
        }
        return canonical.toString();
    }

    private static byte[] readAll(InputStream is) {
        try (InputStream in = is) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int read;
            while ((read = in.read(data)) != -1) {
                buffer.write(data, 0, read);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
