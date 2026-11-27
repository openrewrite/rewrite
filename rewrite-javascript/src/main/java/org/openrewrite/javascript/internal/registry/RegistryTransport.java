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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A manager's resolved request routes. Deliberately has no printable value representation: proxy URLs may contain
 * credentials. Bind only after the manager has applied its source trust and configuration precedence.
 */
public final class RegistryTransport {
    public static final RegistryTransport DEFAULT = new RegistryTransport(null, null, null);

    private final @Nullable String httpProxy;
    private final @Nullable String httpsProxy;
    private final @Nullable String noProxy;
    private final boolean requiresCustomTls;
    /**
     * Whether the route for each scheme, and the bypass list it is checked against, came only from the host's own
     * proxy variables ({@code http_proxy}, {@code https_proxy}, {@code proxy}, {@code no_proxy}) rather than from a
     * manager's configuration.
     */
    private final boolean httpFromHost;
    private final boolean httpsFromHost;

    public RegistryTransport(@Nullable String httpProxy, @Nullable String httpsProxy, @Nullable String noProxy) {
        this(httpProxy, httpsProxy, noProxy, false, false, false);
    }

    private RegistryTransport(@Nullable String httpProxy, @Nullable String httpsProxy, @Nullable String noProxy,
                              boolean requiresCustomTls, boolean httpFromHost, boolean httpsFromHost) {
        this.requiresCustomTls = requiresCustomTls;
        this.httpFromHost = httpFromHost;
        this.httpsFromHost = httpsFromHost;
        this.httpProxy = emptyToNull(httpProxy);
        this.httpsProxy = emptyToNull(httpsProxy);
        this.noProxy = noProxy;
    }

    /** Only transport variables, without projecting the host environment into a project's config. */
    public static Map<String, String> environment(Environment environment) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : Arrays.asList("https_proxy", "http_proxy", "proxy", "no_proxy",
                "npm_config_https_proxy", "npm_config_proxy", "npm_config_noproxy", "npm_config_ca", "npm_config_cafile",
                "npm_config_cert", "npm_config_key", "npm_config_local_address", "npm_config_strict_ssl",
                "npm_config_offline", "npm_config_fetch_timeout")) {
            for (String spelling : Arrays.asList(key, key.toUpperCase(java.util.Locale.ROOT))) {
                String value = environment.getenv(spelling);
                if (value != null) {
                    values.put(spelling, value);
                }
            }
        }
        return values;
    }

    public static RegistryTransport npm(Map<String, ?> config, Map<String, String> env) {
        requireNpmRequestPolicy(config, env);
        Map<String, Object> capabilities = new LinkedHashMap<>(config);
        for (String key : Arrays.asList("ca", "cafile", "cert", "key", "local-address", "strict-ssl")) {
            String value = env(env, "npm_config_" + key.replace('-', '_'));
            if (value != null && !value.isEmpty()) {
                capabilities.put(key, value);
            }
        }
        boolean customTls = requireCapabilities(capabilities, false);
        String proxy = npmSetting(config, env, "https-proxy");
        if (proxy == null) {
            proxy = npmSetting(config, env, "proxy");
        }
        String bypass = npmSetting(config, env, "noproxy");
        boolean bypassFromHost = bypass == null || bypass.isEmpty();
        if (bypassFromHost) {
            bypass = env(env, "no_proxy");
        }
        boolean fromHost = proxy == null && bypassFromHost;
        return new RegistryTransport(proxy != null ? proxy : first(env(env, "https_proxy"),
                first(env(env, "http_proxy"), env(env, "proxy"))),
                proxy != null ? proxy : env(env, "https_proxy"), bypass, customTls, fromHost, fromHost);
    }

    /** Do not turn an offline install or an explicit deadline into an ordinary live request. */
    private static void requireNpmRequestPolicy(Map<String, ?> config, Map<String, String> environment) {
        Object offline = config.get("offline");
        String requested = env(environment, "npm_config_offline");
        if (requested != null && !requested.isEmpty()) {
            requested = requested.trim();
            offline = !"false".equals(requested) && !"null".equals(requested) && !"undefined".equals(requested);
        }
        if (offline != null && !Boolean.FALSE.equals(offline)) {
            throw new CannotKnow("npm network policy requires its offline registry cache, which this transport cannot safely apply");
        }
        String timeout = env(environment, "npm_config_fetch_timeout");
        if (config.containsKey("fetch-timeout") || timeout != null && !timeout.isEmpty()) {
            throw new CannotKnow("npm network policy requires fetch-timeout, which this transport cannot safely apply");
        }
    }

    public static RegistryTransport pnpm(Map<String, ?> config, Map<String, String> env) {
        // PnpmOptions passes only explicitly configured fetchTimeout, after release/source filtering.
        if (config.containsKey("fetchTimeout") && config.get("fetchTimeout") != null) {
            throw new CannotKnow("pnpm network policy requires fetchTimeout, which this transport cannot safely apply");
        }
        boolean customTls = requireCapabilities(config, true);
        String https = text(config.get("httpsProxy"));
        boolean httpsFromHost = false;
        if (https == null) {
            // pnpm's nullish fallback preserves an explicit false proxy.
            Object fallback = config.get("proxy");
            httpsFromHost = fallback == null;
            https = httpsFromHost ? env(env, "https_proxy") : text(fallback);
        }
        String http = text(config.get("httpProxy"));
        boolean httpFromHost = false;
        if (http == null) {
            httpFromHost = https == null || https.isEmpty() || httpsFromHost;
            http = first(https, first(env(env, "http_proxy"), env(env, "proxy")));
        }
        String bypass = text(config.get("noProxy"));
        boolean bypassFromHost = bypass == null && config.get("noproxy") == null;
        if (bypass == null) {
            bypass = config.get("noproxy") != null ? text(config.get("noproxy")) : env(env, "no_proxy");
        }
        return new RegistryTransport(http, https, bypass, customTls,
                httpFromHost && bypassFromHost, httpsFromHost && bypassFromHost);
    }

    /** pnpm's package-manager download uses trusted npmrc network settings with null/false disabling fallback. */
    public static RegistryTransport pnpmBootstrap(Map<String, ?> trusted, Map<String, String> env) {
        Map<String, Object> config = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : trusted.entrySet()) {
            String key = entry.getKey();
            config.put("local-address".equals(key) ? "localAddress" : "strict-ssl".equals(key) ? "strictSsl" : key,
                    entry.getValue());
        }
        boolean customTls = requireCapabilities(config, true);
        Object httpsValue = trusted.get("https-proxy") != null ? trusted.get("https-proxy") : trusted.get("proxy");
        String https = bootstrapProxy(httpsValue, env(env, "https_proxy"),
                trusted.get("https-proxy") != null || trusted.containsKey("proxy"));
        String http = bootstrapProxy(trusted.get("http-proxy"),
                first(https, first(env(env, "http_proxy"), env(env, "proxy"))), trusted.containsKey("http-proxy"));
        Object bypass = trusted.get("no-proxy") != null ? trusted.get("no-proxy") : trusted.get("noproxy");
        boolean httpsFromHost = trusted.get("https-proxy") == null && !trusted.containsKey("proxy");
        boolean httpFromHost = !trusted.containsKey("http-proxy") && (https == null || httpsFromHost);
        boolean bypassFromHost = bypass == null;
        return new RegistryTransport(http, https, bypass == null ? env(env, "no_proxy") : text(bypass), customTls,
                httpFromHost && bypassFromHost, httpsFromHost && bypassFromHost);
    }

    private static @Nullable String bootstrapProxy(@Nullable Object value, @Nullable String fallback, boolean present) {
        if (Boolean.FALSE.equals(value) || present && value == null) {
            return null;
        }
        return first(text(value), fallback);
    }

    private static boolean requireCapabilities(Map<String, ?> config, boolean camelCase) {
        for (String key : Arrays.asList("ca", "cert", "key", "local-address")) {
            String name = camelCase && "local-address".equals(key) ? "localAddress" : key;
            Object value = config.get(name);
            if (value != null && !Boolean.FALSE.equals(value) && !"".equals(value)) {
                throw new CannotKnow("the configured registry transport requires " + key +
                                     ", which this transport cannot safely apply");
            }
        }
        Object strict = config.get(camelCase ? "strictSsl" : "strict-ssl");
        Object cafile = config.get("cafile");
        return cafile != null && !"".equals(cafile) || Boolean.FALSE.equals(strict) || "false".equals(strict);
    }

    private static @Nullable String npmSetting(Map<String, ?> config, Map<String, String> env, String key) {
        String value = env(env, "npm_config_" + key.replace('-', '_'));
        return value != null && !value.isEmpty() ? "false".equals(value) ? null : value : text(config.get(key));
    }

    private static @Nullable String env(Map<String, String> env, String key) {
        String lower = env.get(key);
        String upper = env.get(key.toUpperCase(java.util.Locale.ROOT));
        if (lower != null && upper != null && !lower.equals(upper)) {
            throw new CannotKnow("conflicting upper- and lower-case registry transport environment settings");
        }
        return lower != null ? lower : upper;
    }

    private static @Nullable String first(@Nullable String first, @Nullable String second) {
        return first == null || first.isEmpty() ? second : first;
    }

    private static @Nullable String text(@Nullable Object value) {
        if (value == null || Boolean.FALSE.equals(value) || "".equals(value)) {
            return null;
        }
        if (value instanceof String) {
            if (((String) value).contains("${")) {
                throw new CannotKnow("the configured registry transport contains an unresolved environment placeholder");
            }
            return (String) value;
        }
        if (value instanceof List) {
            StringBuilder joined = new StringBuilder();
            for (Object entry : (List<?>) value) {
                if (!(entry instanceof String)) {
                    throw new CannotKnow("the configured registry bypass list contains an unsupported value");
                }
                if (joined.length() > 0) {
                    joined.append(',');
                }
                joined.append(entry);
            }
            return joined.toString();
        }
        throw new CannotKnow("the configured registry transport has an unsupported value type");
    }

    public HttpSender sender(HttpSender sender, String url) {
        // Preserve the existing host contract: a custom injected sender supplies TLS configuration itself.
        if (requiresCustomTls && !isHostSender(sender)) {
            throw new CannotKnow("the configured registry transport requires custom TLS (cafile/strict-ssl), " +
                                 "but no TLS-capable HttpSender was injected");
        }
        URI target;
        try {
            target = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new CannotKnow("the registry request URL is invalid");
        }
        boolean https = "https".equalsIgnoreCase(target.getScheme());
        String configured = https ? httpsProxy : httpProxy;
        if (configured == null) {
            return sender;
        }
        if ((https ? httpsFromHost : httpFromHost) && isHostSender(sender)) {
            // As with TLS above: a custom injected sender is the host's own client, and the host's proxy variables
            // describe the host's network, which that sender already routes. It takes the route when it can bind
            // one; only a route that a manager's configuration sets must be bound or refused.
            try {
                return sender.withProxy(route(target, configured));
            } catch (CannotKnow | UnsupportedOperationException e) {
                return sender;
            }
        }
        Proxy route = route(target, configured);
        try {
            return sender.withProxy(route);
        } catch (UnsupportedOperationException e) {
            throw new CannotKnow("the injected HttpSender cannot honor the configured registry proxy route; " +
                                 "it must implement withProxy");
        }
    }

    /** Whether {@code sender} is a host's own client rather than the default URL sender. */
    public static boolean isHostSender(HttpSender sender) {
        return sender.getClass() != HttpUrlConnectionSender.class;
    }

    private Proxy route(URI target, String configured) {
        if (bypasses(target.getHost())) {
            return Proxy.NO_PROXY;
        }
        URI proxy;
        try {
            proxy = URI.create(configured);
        } catch (IllegalArgumentException e) {
            throw new CannotKnow("the configured registry proxy URL is invalid");
        }
        if (!"http".equalsIgnoreCase(proxy.getScheme()) || proxy.getHost() == null ||
            proxy.getPort() == 0 || proxy.getPort() > 65535 || proxy.getRawQuery() != null ||
            proxy.getRawFragment() != null ||
            proxy.getRawPath() != null && !proxy.getRawPath().isEmpty() && !"/".equals(proxy.getRawPath())) {
            throw new CannotKnow("the configured registry proxy requires an unsupported scheme or URL capability; " +
                                 "only an HTTP proxy is supported");
        }
        if (proxy.getRawUserInfo() != null) {
            throw new CannotKnow("the configured registry proxy requires authentication; " +
                                 "proxy credentials cannot be safely supplied by this transport");
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(),
                proxy.getPort() < 0 ? 80 : proxy.getPort()));
    }

    // npm 11.19.1 @npmcli/agent and pnpm 11.10.0 checkNoProxy match hostname suffix segments.
    // Neither interprets '*' or host:port as a wildcard/port rule. A leading dot also matches the host itself.
    private boolean bypasses(@Nullable String hostname) {
        if (hostname == null || noProxy == null) {
            return false;
        }
        hostname = hostname.toLowerCase(java.util.Locale.ROOT);
        for (String entry : noProxy.split(",")) {
            String suffix = entry.trim().replaceAll("^\\.+|\\.+$", "").replaceAll("\\.{2,}", ".");
            if (!suffix.isEmpty() && (hostname.equals(suffix) || hostname.endsWith("." + suffix))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
