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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A session-scoped record of repository endpoints (host:port) that challenged an anonymous request and
 * required credentials. Once an endpoint is known to require authentication, subsequent requests can send
 * credentials preemptively instead of paying another anonymous round-trip.
 * <p>
 * This mirrors the per-session {@code BasicAuthCache} that Apache Maven Resolver keeps on its HTTP client:
 * anonymous-first on the first contact with a host, then preemptive once the host has challenged. The cache
 * lives on the {@link ExecutionContext}, so it is shared across POM/metadata resolution and artifact download
 * whenever they run against the same context, and it never crosses process or serialization boundaries.
 */
public final class MavenAuthenticationCache {
    private static final String MESSAGE_KEY = "org.openrewrite.maven.internal.authenticationRequiredEndpoints";

    private MavenAuthenticationCache() {
    }

    /**
     * @return {@code true} if the endpoint for the given URI has already required authentication in this session.
     */
    public static boolean requiresAuthentication(ExecutionContext ctx, String uri) {
        String endpoint = endpoint(uri);
        return endpoint != null && endpoints(ctx).contains(endpoint);
    }

    /**
     * Record that the endpoint for the given URI required authentication, so later requests to the same
     * endpoint can authenticate preemptively.
     */
    public static void rememberRequiresAuthentication(ExecutionContext ctx, String uri) {
        String endpoint = endpoint(uri);
        if (endpoint != null) {
            endpoints(ctx).add(endpoint);
        }
    }

    private static @Nullable String endpoint(String uri) {
        try {
            URI parsed = URI.create(uri);
            String host = parsed.getHost();
            return host == null ? null : host + ':' + parsed.getPort();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Set<String> endpoints(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(MESSAGE_KEY, k -> ConcurrentHashMap.newKeySet());
    }
}
