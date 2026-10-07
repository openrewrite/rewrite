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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of yarn src/registries/npm-registry.js {@code request} and util/request-manager.js's cache of requests by url
 * (yarn 1.22.22): a package's metadata, asked for once per url in an install.
 */
final class NpmRegistry {

    private static final Pattern REGISTRY_PREFIX = Pattern.compile("^https?:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENFORCED_HTTPS = Pattern.compile(
            "^https?://([^/]+\\.)?(yarnpkg\\.com|npmjs\\.(org|com))(/|\\z)");

    private final YarnClassicOptions config;
    private final EventLoop loop;
    private final PackumentSource source;
    private final Map<String, EventLoop.Io<Object>> cache = new HashMap<>();

    NpmRegistry(YarnClassicOptions config, EventLoop loop, PackumentSource source) {
        this.config = config;
        this.loop = loop;
        this.source = source;
    }

    static String escapeName(String name) {
        int slash = name.indexOf('/');
        return slash < 0 ? name : name.substring(0, slash) + "%2f" + name.substring(slash + 1);
    }

    static String getRequestUrl(String registry, String pathname) {
        String resolved = pathname;
        if (!REGISTRY_PREFIX.matcher(pathname).find()) {
            resolved = (registry.endsWith("/") ? registry : registry + "/") + pathname;
        }
        if (ENFORCED_HTTPS.matcher(resolved).find()) {
            resolved = resolved.replaceFirst("^http://", "https://");
        }
        return resolved;
    }

    /**
     * The parsed packument; the request rejects with the registry's {@code Not found} when it has no such package.
     */
    EventLoop.Io<Object> request(String pathname, Set<Integer> causal) {
        String registry = config.getRegistry(pathname);
        String url = getRequestUrl(registry, pathname);
        EventLoop.Io<Object> cached = cache.get(url);
        if (cached != null) {
            return cached;
        }
        String name = pathname.replaceFirst("%2f", "/");
        EventLoop.Io<Object> io = loop.io(causal, () -> {
            String body = source.packument(registry, name);
            if (body == null) {
                throw new JsError(JsError.ERROR, url + ": Not found");
            }
            return parse(body);
        });
        cache.put(url, io);
        return io;
    }

    // the request library's json mode: a body that does not parse is handed over as the string it is
    private static @Nullable Object parse(String body) {
        try {
            return PlainObject.fromJson(Js.jsonParse(body));
        } catch (JsError e) {
            return body;
        }
    }
}
