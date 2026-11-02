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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.npm.JsonParseEvenBetterErrors;
import org.openrewrite.javascript.internal.lock.npm.NpmRejection;
import org.openrewrite.javascript.internal.lock.npm.PackumentSource;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The registry documents of a build, fetched through the run's {@link NpmRegistryClient} from the registry each name
 * is configured to, and parsed once per build.
 */
public final class NpmRegistryPackuments implements PackumentSource {

    private final NodeRegistries registries;
    private final NpmRegistryClient client;
    private final Map<String, Map<String, Object>> parsed = new HashMap<>();

    public NpmRegistryPackuments(NodeRegistries registries, NpmRegistryClient client) {
        this.registries = registries;
        this.client = client;
    }

    @Override
    public Map<String, Object> packument(String url, String name, boolean fullMetadata) {
        String key = (fullMetadata ? "full:" : "corgi:") + url + " " + name;
        Map<String, Object> document = parsed.get(key);
        if (document == null) {
            NodeRegistry registry = registries.registryAt(name, url);
            document = parse(fetch(() -> client.getPackumentText(registry, name, fullMetadata), name, null), name);
            parsed.put(key, document);
        }
        return document;
    }

    @Override
    public Map<String, Object> manifest(String url, String name, String version) {
        String key = "manifest:" + url + " " + name + "@" + version;
        Map<String, Object> document = parsed.get(key);
        if (document == null) {
            NodeRegistry registry = registries.registryAt(name, url);
            document = parse(fetch(() -> client.getManifestText(registry, name, version), name, version), name);
            parsed.put(key, document);
        }
        return document;
    }

    private static String fetch(Supplier<String> fetch, String name, @Nullable String version) {
        try {
            return fetch.get();
        } catch (NodeRegistryException e) {
            switch (e.getReason()) {
                case PACKAGE_NOT_FOUND:
                case VERSION_NOT_FOUND:
                    throw new NpmRejection("E404", "404 Not Found - GET " + url(e) + " - Not found",
                            version == null ? name : name + "@" + version);
                default:
                    // unreachable or refusing the credentials: the registry's own failure, reported as such
                    throw e;
            }
        }
    }

    private static String url(NodeRegistryException e) {
        String message = e.getMessage();
        int from = message.indexOf("from ");
        return from < 0 ? e.getRegistryUrl() : message.substring(from + "from ".length());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String text, String name) {
        Object value;
        try {
            value = JsonParseEvenBetterErrors.jsonParse(text);
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            throw new CannotKnow("the registry's response for " + name + " is not JSON: " + e.getMessage());
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("the registry's response for " + name + " is not an object");
        }
        return (Map<String, Object>) value;
    }
}
