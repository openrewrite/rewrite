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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction1;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.internal.StreamUtils.readAllBytes;

public class MavenArtifactDownloader {
    private static final RetryConfig retryConfig = RetryConfig.custom()
            .retryExceptions(SocketTimeoutException.class, TimeoutException.class)
            .build();

    private static final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
    private static final Retry mavenDownloaderRetry = retryRegistry.retry("MavenDownloader");

    private final MavenArtifactCache mavenArtifactCache;
    private final Map<String, MavenSettings.Server> serverIdToServer;
    private final Consumer<Throwable> onError;
    private final HttpSender httpSender;
    private final CheckedFunction1<HttpSender.Request, HttpSender.Response> sendRequest;

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
        this.sendRequest = Retry.decorateCheckedFunction(mavenDownloaderRetry, httpSender::send);
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
    @Nullable
    public Path downloadArtifact(ResolvedDependency dependency) {
        if (dependency.getRequested().getType() != null && !"jar".equals(dependency.getRequested().getType())) {
            return null;
        }
        return mavenArtifactCache.computeArtifact(dependency, () -> {
            try {
                String uri = requireNonNull(dependency.getRepository(),
                        String.format("Respository for dependency '%s' was null.", dependency)).getUri() + "/" +
                        dependency.getGroupId().replace('.', '/') + '/' +
                        dependency.getArtifactId() + '/' +
                        dependency.getVersion() + '/' +
                        dependency.getArtifactId() + '-' +
                        (dependency.getDatedSnapshotVersion() == null ? dependency.getVersion() : dependency.getDatedSnapshotVersion()) +
                        ".jar";

                InputStream bodyStream;

                if (uri.startsWith("~")) {
                    bodyStream = Files.newInputStream(Paths.get(System.getProperty("user.home") + uri.substring(1)));
                } else {
                    HttpSender.Request.Builder request = applyAuthentication(dependency.getRepository(), httpSender.get(uri));
                    try(HttpSender.Response response = sendRequest.apply(request.build());
                        InputStream body = response.getBody()) {
                        if (!response.isSuccessful() || body == null) {
                            onError.accept(new MavenDownloadingException(String.format("Unable to download dependency %s:%s:%s. Response was %d",
                                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), response.getCode()), null,
                                    dependency.getRequested().getGav()));
                            return null;
                        }
                        bodyStream = new ByteArrayInputStream(readAllBytes(body));
                    } catch (Throwable t) {
                        throw new MavenDownloadingException("Unable to download dependency", t,
                                dependency.getRequested().getGav());
                    }

                }
                return bodyStream;

            } catch (Throwable t) {
                onError.accept(t);
            }
            return null;
        }, onError);
    }

    private HttpSender.Request.Builder applyAuthentication(MavenRepository repository, HttpSender.Request.Builder request) {
        MavenSettings.Server authInfo = serverIdToServer.get(repository.getId());
        if (authInfo != null) {
            return request.withBasicAuthentication(authInfo.getUsername(), authInfo.getPassword());
        } else if (repository.getUsername() != null && repository.getPassword() != null) {
            return request.withBasicAuthentication(repository.getUsername(), repository.getPassword());
        }
        return request;
    }
}
