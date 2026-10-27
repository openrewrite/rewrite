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
package org.openrewrite.maven.internal;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.remote.RemoteAuthenticator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Authenticates {@link org.openrewrite.remote.Remote} downloads from a Maven repository with the {@code httpHeaders}
 * and the username and password of its {@code <server>} in the Maven settings. Credentials are only sent to URIs
 * under the repository's URI.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MavenRepositoryRemoteAuthenticator implements RemoteAuthenticator {
    String scheme;
    String host;
    int port;
    String path;

    @ToString.Exclude
    List<MavenSettings.HttpHeader> httpHeaders;

    @Nullable
    String username;

    @ToString.Exclude
    @Nullable
    String password;

    /**
     * @param repository A repository that mirrors and credentials from the settings have already been applied to.
     * @param settings   The settings holding the {@code <server>} whose id equals the repository's id, if any.
     * @return An authenticator for the repository, or {@code null} when there are no credentials to send.
     */
    public static @Nullable MavenRepositoryRemoteAuthenticator forRepository(MavenRepository repository,
                                                                             @Nullable MavenSettings settings) {
        URI uri;
        try {
            uri = URI.create(repository.getUri());
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            return null;
        }

        List<MavenSettings.HttpHeader> httpHeaders = new ArrayList<>();
        MavenSettings.Server server = server(repository, settings);
        if (server != null && server.getConfiguration() != null && server.getConfiguration().getHttpHeaders() != null) {
            for (MavenSettings.HttpHeader header : server.getConfiguration().getHttpHeaders()) {
                if (isResolved(header.getName()) && isResolved(header.getValue())) {
                    httpHeaders.add(header);
                }
            }
        }
        boolean hasCredentials = isResolved(repository.getUsername()) && isResolved(repository.getPassword());
        if (httpHeaders.isEmpty() && !hasCredentials) {
            return null;
        }

        String path = uri.getPath() == null ? "/" : uri.getPath();
        return new MavenRepositoryRemoteAuthenticator(
                uri.getScheme().toLowerCase(Locale.ROOT),
                uri.getHost().toLowerCase(Locale.ROOT),
                port(uri),
                path.endsWith("/") ? path : path + "/",
                httpHeaders,
                hasCredentials ? repository.getUsername() : null,
                hasCredentials ? repository.getPassword() : null
        );
    }

    @Override
    public boolean appliesTo(URI uri) {
        return uri.getScheme() != null && scheme.equals(uri.getScheme().toLowerCase(Locale.ROOT)) &&
               uri.getHost() != null && host.equals(uri.getHost().toLowerCase(Locale.ROOT)) &&
               port == port(uri) &&
               uri.getPath() != null && uri.getPath().startsWith(path);
    }

    @Override
    public HttpSender.Request.Builder authenticate(URI uri, HttpSender.Request.Builder request) {
        for (MavenSettings.HttpHeader header : httpHeaders) {
            request.withHeader(header.getName(), header.getValue());
        }
        if (username != null) {
            request.withBasicAuthentication(username, password);
        }
        return request;
    }

    private static MavenSettings.@Nullable Server server(MavenRepository repository, @Nullable MavenSettings settings) {
        if (repository.getId() == null || settings == null || settings.getServers() == null) {
            return null;
        }
        for (MavenSettings.Server server : settings.getServers().getServers()) {
            if (repository.getId().equals(server.getId())) {
                return server;
            }
        }
        return null;
    }

    private static boolean isResolved(@Nullable String value) {
        return value != null && !value.contains("${");
    }

    private static int port(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        return "https".equals(scheme) ? 443 : "http".equals(scheme) ? 80 : -1;
    }
}
