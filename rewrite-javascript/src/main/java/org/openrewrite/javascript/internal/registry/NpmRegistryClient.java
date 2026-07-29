/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.registry.NpmRegistryException.Reason;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches full packuments over the run's {@link HttpSender}. Full (not abbreviated)
 * metadata is required because npm itself resolves with {@code fullMetadata: true} —
 * lock entries carry {@code license}, {@code funding} and {@code bin}, which the
 * abbreviated document omits. Responses are cached per client instance; the engine
 * only fetches metadata for packages an edit actually moves.
 */
public class NpmRegistryClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpSender httpSender;
    private final NpmRegistryConfig config;
    private final Map<String, Packument> cache = new ConcurrentHashMap<>();

    public NpmRegistryClient(HttpSender httpSender, NpmRegistryConfig config) {
        this.httpSender = httpSender;
        this.config = config;
    }

    public Packument packument(String packageName) {
        String registry = config.registryFor(packageName);
        String url = registry + encodeName(packageName);
        return cache.computeIfAbsent(url, u -> fetch(registry, u, packageName));
    }

    private Packument fetch(String registry, String url, String packageName) {
        HttpSender.Request request;
        try {
            HttpSender.Request.Builder builder = httpSender.get(url)
                    .withHeader("Accept", "application/json");
            String authorization = config.authorizationFor(registry);
            if (authorization != null) {
                builder = builder.withHeader("Authorization", authorization);
            }
            request = builder.build();
        } catch (NpmRegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new NpmRegistryException(Reason.CONFIG, registry, "Invalid registry URL: " + url, e);
        }
        try (HttpSender.Response response = httpSender.send(request)) {
            int code = response.getCode();
            if (code == 401 || code == 403) {
                throw new NpmRegistryException(Reason.AUTH_FAILED, registry, "HTTP " + code + " from " + url);
            }
            if (code == 404) {
                throw new NpmRegistryException(Reason.NOT_FOUND, registry,
                        "Package " + packageName + " not found at " + registry);
            }
            if (!response.isSuccessful()) {
                throw new NpmRegistryException(Reason.UNREACHABLE, registry, "HTTP " + code + " from " + url);
            }
            JsonNode body = MAPPER.readTree(response.getBodyAsBytes());
            if (!(body instanceof ObjectNode)) {
                throw new NpmRegistryException(Reason.UNREACHABLE, registry,
                        "Unexpected packument payload from " + url);
            }
            return new Packument((ObjectNode) body);
        } catch (NpmRegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new NpmRegistryException(Reason.UNREACHABLE, registry,
                    "Failed to fetch " + packageName + " from " + registry + ": " + e, e);
        }
    }

    static String encodeName(String packageName) {
        // Scoped names URL-encode the separating slash, matching npm-package-arg.
        return packageName.startsWith("@") ? packageName.replace("/", "%2f") : packageName;
    }
}
