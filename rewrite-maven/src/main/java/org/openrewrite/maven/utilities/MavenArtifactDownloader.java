/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.utilities;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.internal.StreamUtils.readAllBytes;

public class MavenArtifactDownloader {
    private static final RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
            .handle(SocketTimeoutException.class, TimeoutException.class)
            .handleIf(throwable -> throwable instanceof UncheckedIOException && throwable.getCause() instanceof SocketTimeoutException)
            .withDelay(Duration.ofMillis(500))
            .withJitter(0.1)
            .withMaxRetries(5)
            .build();

    private final MavenArtifactCache mavenArtifactCache;
    private final Map<String, MavenSettings.Server> serverIdToServer;
    private final Consumer<Throwable> onError;
    private final HttpSender httpSender;


    public MavenArtifactDownloader(MavenArtifactCache mavenArtifactCache,
                                   @Nullable MavenSettings settings,
                                   Consumer<Throwable> onError) {
        this(mavenArtifactCache, settings, new HttpUrlConnectionSender(), onError);
    }

    public MavenArtifactDownloader(MavenArtifactCache mavenArtifactCache,
                                   @Nullable MavenSettings settings,
                                   HttpSender httpSender,
                                   Consumer<Throwable> onError) {
        this.httpSender = httpSender;
        this.mavenArtifactCache = mavenArtifactCache;
        this.onError = onError;
        this.serverIdToServer = settings == null || settings.getServers() == null ?
                new HashMap<>() :
                settings.getServers().getServers().stream()
                        .collect(toMap(MavenSettings.Server::getId, Function.identity()));
    }

    /**
     * Fetch the jar file indicated by the dependency.
     *
     * @param dependency The dependency to download.
     * @return The path on disk of the downloaded artifact or <code>null</code> if unable to download.
     */
    public @Nullable Path downloadArtifact(ResolvedDependency dependency) {
        if (dependency.getRequested().getType() != null && !"jar".equals(dependency.getRequested().getType())) {
            return null;
        }
        return mavenArtifactCache.computeArtifact(dependency, () -> {
            String baseUri = requireNonNull(dependency.getRepository(),
                    String.format("Repository for dependency '%s' was null.", dependency)).getUri();
            String path = dependency.getGroupId().replace('.', '/') + '/' +
                          dependency.getArtifactId() + '/' +
                          dependency.getVersion() + '/' +
                          dependency.getArtifactId() + '-' +
                          (dependency.getDatedSnapshotVersion() == null ? dependency.getVersion() : dependency.getDatedSnapshotVersion()) +
                          (dependency.getClassifier() == null ? "" : "-" + dependency.getClassifier()) +
                          ".jar";
            String uri = baseUri + (baseUri.endsWith("/") ? "" : "/") + path;

            InputStream bodyStream;

            if (uri.startsWith("~")) {
                bodyStream = Files.newInputStream(Paths.get(System.getProperty("user.home") + uri.substring(1)));
            } else if ("file".equals(URI.create(uri).getScheme())) {
                bodyStream = Files.newInputStream(Paths.get(URI.create(uri)));
            } else {
                MavenRepository repository = dependency.getRepository();
                HttpSender.Request.Builder request = applyAuthentication(dependency.getRepository(), httpSender.get(uri));
                try (HttpSender.Response response = Failsafe.with(retryPolicy).get(() -> httpSender.send(request.build()));
                     InputStream body = response.getBody()) {
                    if (!response.isSuccessful() || body == null) {
                        int code = response.getCode();
                        // If credentials caused a client-side error, retry anonymously
                        boolean hadAuth = serverIdToServer.get(repository.getId()) != null || repository.getUsername() != null && repository.getPassword() != null;
                        if (hadAuth && code >= 400 && code <= 499 && code != 408 && code != 425 && code != 429) {
                            bodyStream = downloadAnonymously(uri);
                            if (bodyStream != null) {
                                return bodyStream;
                            }
                        }
                        onError.accept(new MavenDownloadingException(String.format("Unable to download dependency %s:%s:%s from %s. Response was %d",
                                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), uri, code), null,
                                dependency.getRequested().getGav()));
                        return null;
                    }
                    bodyStream = new ByteArrayInputStream(readAllBytes(body));
                } catch (Throwable t) {
                    Throwable cause = t instanceof FailsafeException && t.getCause() != null ? t.getCause() : t;
                    throw new MavenDownloadingException("Unable to download dependency", cause,
                            dependency.getRequested().getGav());
                }
            }
            return bodyStream;
        }, onError);
    }

    private HttpSender.Request.Builder applyAuthentication(MavenRepository repository, HttpSender.Request.Builder request) {
        MavenSettings.Server authInfo = serverIdToServer.get(repository.getId());
        if (authInfo != null) {
            if (authInfo.getConfiguration() != null && authInfo.getConfiguration().getHttpHeaders() != null) {
                for (MavenSettings.HttpHeader header : authInfo.getConfiguration().getHttpHeaders()) {
                    request.withHeader(header.getName(), header.getValue());
                }
            }
            return request.withBasicAuthentication(authInfo.getUsername(), authInfo.getPassword());
        } else if (repository.getUsername() != null && repository.getPassword() != null) {
            return request.withBasicAuthentication(repository.getUsername(), repository.getPassword());
        }
        return request;
    }

    private @Nullable InputStream downloadAnonymously(String uri) {
        try {
            HttpSender.Request.Builder anonRequest = httpSender.get(uri);
            try (HttpSender.Response response = Failsafe.with(retryPolicy).get(() -> httpSender.send(anonRequest.build()));
                 InputStream body = response.getBody()) {
                if (response.isSuccessful() && body != null) {
                    return new ByteArrayInputStream(readAllBytes(body));
                }
            }
        } catch (Throwable ignored) {
            // Anonymous retry also failed; fall through
        }
        return null;
    }
}
