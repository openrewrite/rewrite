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

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import org.eclipse.aether.spi.connector.transport.AbstractTransporter;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * The sole network {@link org.eclipse.aether.spi.connector.transport.Transporter}: every byte is pumped through an
 * injected OpenRewrite {@link HttpSender}. All checksum/resume/negative-caching logic lives above this in
 * BasicRepositoryConnector; this owns byte transfer plus the load-bearing {@link #classify(Throwable)}, timeout-only
 * retry, authenticated&rarr;anonymous fallback, http&rarr;https upgrade, per-server headers/timeouts, and run-scoped
 * dead-endpoint memory — ported from {@code MavenPomDownloader} to match its behavior exactly.
 */
class HttpSenderTransporter extends AbstractTransporter {

    // Matches MavenPomDownloader.retryPolicy exactly: timeouts only, 5 retries, 500ms + 10% jitter.
    private static final RetryPolicy<HttpSender.Response> RETRY_POLICY = RetryPolicy.<HttpSender.Response>builder()
            .handle(SocketTimeoutException.class, TimeoutException.class)
            .handleIf(t -> t instanceof UncheckedIOException && t.getCause() instanceof SocketTimeoutException)
            .withDelay(Duration.ofMillis(500))
            .withJitter(0.1)
            .withMaxRetries(5)
            .build();

    private final HttpSender httpSender;
    private final URI baseUri;

    // Legacy MavenPomDownloader.normalizeRepository prefers https for an http:// repository URL. Here the upgrade is
    // learned on demand: an http request failing at the connection level (e.g. a TLS-only endpoint answering plaintext
    // with garbage) is retried once over https, and a TLS answer sticks for the rest of this transport's life.
    private volatile boolean httpsUpgraded;

    private final @Nullable String username;
    private final @Nullable String password;
    private final Map<String, String> headers;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Set<String> unreachableHosts;
    private final ResolutionTimeRecorder resolutionTimeRecorder;

    HttpSenderTransporter(HttpSender httpSender, String repositoryUrl, @Nullable String username,
                          @Nullable String password, Map<String, String> headers, Duration connectTimeout,
                          Duration readTimeout, Set<String> unreachableHosts,
                          ResolutionTimeRecorder resolutionTimeRecorder) {
        this.httpSender = httpSender;
        String url = repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        this.baseUri = URI.create(url);
        this.username = username;
        this.password = password;
        this.headers = headers;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.unreachableHosts = unreachableHosts;
        this.resolutionTimeRecorder = resolutionTimeRecorder;
    }

    /**
     * The one method driving all of Maven's negative caching. A deterministic 4xx (client error unlikely to change:
     * everything except 408/425/429) means the resource is authoritatively absent, so {@code ERROR_NOT_FOUND}; anything
     * else (5xx, timeouts, the transient 4xx, connection failures) is a transfer error, so {@code ERROR_OTHER}. Mirrors
     * {@code HttpSenderResponseException.isClientSideException()}.
     */
    @Override
    public int classify(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof HttpResponseException) {
                return isDeterministicClientError(((HttpResponseException) t).statusCode) ? ERROR_NOT_FOUND : ERROR_OTHER;
            }
        }
        return ERROR_OTHER;
    }

    /** 400&ndash;499 except 408 (timeout), 425 (too early), 429 (too many requests). */
    static boolean isDeterministicClientError(int code) {
        return code >= 400 && code <= 499 && code != 408 && code != 425 && code != 429;
    }

    /** 401&ndash;403: an access-denied response that anonymous retry should surface as the original failure. */
    private static boolean isAccessDenied(int code) {
        return code > 400 && code <= 403;
    }

    @Override
    protected void implPeek(PeekTask task) throws Exception {
        // PEEK -> HEAD: existence check without downloading the body.
        try (HttpSender.Response response = exchange(HttpSender.Method.HEAD, resolve(task.getLocation()))) {
            // Success closed here; failures already threw in exchange().
        }
    }

    @Override
    protected void implGet(GetTask task) throws Exception {
        HttpSender.Response response = exchange(HttpSender.Method.GET, resolve(task.getLocation()));
        try {
            long length = contentLength(response.getHeaders());
            try (InputStream body = response.getBody()) {
                utilGet(task, body, true, length, false);
            }
        } finally {
            response.close();
        }
    }

    @Override
    protected void implPut(PutTask task) {
        // The engine never deploys; uploads route through rewrite's own machinery, not this transport.
        throw new UnsupportedOperationException("HttpSenderTransporter does not support PUT (deploy)");
    }

    @Override
    protected void implClose() {
    }

    /**
     * Replicates Apache Maven's authenticated&rarr;anonymous fallback (MavenPomDownloader:1112-1138): try authenticated;
     * on a deterministic 4xx with credentials present, retry anonymously; if the anonymous retry is access-denied
     * (401-403) rethrow the <em>original</em> exception, otherwise the retry's.
     */
    private HttpSender.Response exchange(HttpSender.Method method, String url) throws Exception {
        checkReachable(url);
        try {
            return send(method, url, true);
        } catch (HttpResponseException e) {
            if (hasCredentials() && isDeterministicClientError(e.statusCode)) {
                try {
                    return send(method, url, false);
                } catch (HttpResponseException retry) {
                    if (isAccessDenied(retry.statusCode)) {
                        throw e;
                    }
                    throw retry;
                }
            }
            throw e;
        }
    }

    /**
     * Sends one request, retrying an http connection-level failure once over https (see {@link #httpsUpgraded}).
     * When both fail, the host is remembered as unreachable this run so sibling requests short-circuit, and the
     * original http failure propagates.
     */
    private HttpSender.Response send(HttpSender.Method method, String url, boolean authenticated)
            throws HttpResponseException, IOException {
        String effectiveUrl = httpsUpgraded ? toHttps(url) : url;
        try {
            return sendOnce(method, effectiveUrl, authenticated);
        } catch (HttpResponseException e) {
            throw e;
        } catch (IOException e) {
            if (httpsUpgraded || !url.regionMatches(true, 0, "http://", 0, 7)) {
                if (!(e instanceof SocketTimeoutException)) {
                    markUnreachable(url);
                }
                throw e;
            }
            try {
                HttpSender.Response response = sendOnce(method, toHttps(url), authenticated);
                httpsUpgraded = true;
                return response;
            } catch (HttpResponseException tls) {
                httpsUpgraded = true; // the server answered over TLS; its status is the authoritative answer
                throw tls;
            } catch (IOException tlsFailure) {
                if (!(e instanceof SocketTimeoutException)) {
                    markUnreachable(url);
                }
                throw e;
            }
        }
    }

    private static String toHttps(String url) {
        return "https://" + url.substring("http://".length());
    }

    /** Sends one request with timeout-only retry; throws {@link HttpResponseException} on any HTTP &ge; 400. */
    private HttpSender.Response sendOnce(HttpSender.Method method, String url, boolean authenticated)
            throws HttpResponseException, IOException {
        HttpSender.Request request = build(method, url, authenticated);
        long start = System.nanoTime();
        try {
            return Failsafe.with(RETRY_POLICY).get(() -> {
                HttpSender.Response response = httpSender.send(request);
                int code = response.getCode();
                if (code >= 400) {
                    response.close();
                    throw new HttpResponseException(code);
                }
                return response;
            });
        } catch (FailsafeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof HttpResponseException) {
                throw (HttpResponseException) cause;
            }
            if (cause instanceof UncheckedIOException) {
                throw ((UncheckedIOException) cause).getCause();
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            resolutionTimeRecorder.record(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    private HttpSender.Request build(HttpSender.Method method, String url, boolean authenticated) {
        HttpSender.Request.Builder builder = httpSender.newRequest(url).withMethod(method)
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.withHeader(header.getKey(), header.getValue());
        }
        if (authenticated && hasCredentials()) {
            builder.withBasicAuthentication(username, password);
        }
        return builder.build();
    }

    private void checkReachable(String url) throws IOException {
        String authority = authority(URI.create(url));
        if (authority != null && unreachableHosts.contains(authority)) {
            throw new IOException("host " + authority + " is known to be unreachable this run");
        }
    }

    private void markUnreachable(String url) {
        String authority = authority(URI.create(url));
        if (authority != null) {
            unreachableHosts.add(authority);
        }
    }

    private boolean hasCredentials() {
        return username != null && password != null;
    }

    private String resolve(URI location) {
        return baseUri.resolve(location).toString();
    }

    private static @Nullable String authority(URI uri) {
        if (uri.getHost() == null) {
            return null;
        }
        int port = uri.getPort();
        if (port < 0) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }
        return uri.getHost() + ":" + port;
    }

    private static long contentLength(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if ("Content-Length".equalsIgnoreCase(e.getKey()) && e.getValue() != null && !e.getValue().isEmpty()) {
                try {
                    return Long.parseLong(e.getValue().get(0));
                } catch (NumberFormatException ignored) {
                    return -1L;
                }
            }
        }
        return -1L;
    }

    /** Carries the HTTP status so {@link #classify} and the auth fallback can branch on it. */
    static final class HttpResponseException extends Exception {
        final int statusCode;

        HttpResponseException(int statusCode) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
        }
    }
}
