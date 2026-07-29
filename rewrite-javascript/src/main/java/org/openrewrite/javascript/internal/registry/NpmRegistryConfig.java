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
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.JavaScriptExecutionContextView;
import org.openrewrite.javascript.NpmRegistryCredentials;
import org.openrewrite.javascript.internal.registry.NpmRegistryException.Reason;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry and credential resolution for npm packages, mirroring npm's own rules:
 * {@code @scope:registry=} then {@code registry=} then the npmjs default, with
 * auth keys matched to a registry by the npm-registry-fetch walk over
 * {@code //host/path:_authToken}-style keys. {@code ${VAR}} placeholders expand
 * from the environment; a registry whose URL still contains an unresolved
 * placeholder fails loudly only when actually used.
 * <p>
 * Host-supplied configuration on {@link JavaScriptExecutionContextView} takes
 * precedence over {@code .npmrc} properties, which arrive already merged in
 * scope priority (global &lt; user &lt; project).
 */
public class NpmRegistryConfig {

    public static final String DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    private static final Pattern ENV_VAR = Pattern.compile("\\$\\{([^}]+)}");

    private final Map<String, String> npmrc;
    private final @Nullable String viewDefaultRegistry;
    private final Map<String, String> viewScopedRegistries;
    private final java.util.List<NpmRegistryCredentials> viewCredentials;
    private final Function<String, @Nullable String> env;

    public NpmRegistryConfig(Map<String, String> npmrcProperties, ExecutionContext ctx) {
        this(npmrcProperties, ctx, System::getenv);
    }

    NpmRegistryConfig(Map<String, String> npmrcProperties, ExecutionContext ctx,
                      Function<String, @Nullable String> env) {
        this.npmrc = npmrcProperties;
        this.env = env;
        JavaScriptExecutionContextView view = JavaScriptExecutionContextView.view(ctx);
        this.viewDefaultRegistry = view.getNpmDefaultRegistry();
        this.viewScopedRegistries = view.getNpmScopedRegistries() == null
                ? new LinkedHashMap<>() : view.getNpmScopedRegistries();
        this.viewCredentials = view.getRegistryCredentials();
    }

    /** Parse merged {@code .npmrc} content (ini-style {@code key=value} lines). */
    public static Map<String, String> parseNpmrc(@Nullable String content) {
        Map<String, String> props = new LinkedHashMap<>();
        if (content == null) {
            return props;
        }
        for (String line : content.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 1) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            if (value.length() >= 2 &&
                    (value.startsWith("\"") && value.endsWith("\"") ||
                            value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            props.put(key, value);
        }
        return props;
    }

    /** The registry URL (trailing slash guaranteed) serving the given package name. */
    public String registryFor(String packageName) {
        String scope = packageName.startsWith("@") && packageName.indexOf('/') > 0
                ? packageName.substring(0, packageName.indexOf('/'))
                : null;
        String url = null;
        if (scope != null) {
            url = viewScopedRegistries.get(scope);
            if (url == null) {
                url = expanded(scope + ":registry");
            }
        }
        if (url == null) {
            url = viewDefaultRegistry;
        }
        if (url == null) {
            url = expanded("registry");
        }
        if (url == null) {
            url = DEFAULT_REGISTRY;
        }
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * The {@code Authorization} header value for the given registry URL, or
     * {@code null} for anonymous access. Host-supplied credentials win; otherwise
     * npm's auth-key walk over the registry's host and path.
     */
    public @Nullable String authorizationFor(String registryUrl) {
        URI uri = URI.create(registryUrl);
        String host = uri.getHost();
        if (host != null) {
            for (NpmRegistryCredentials cred : viewCredentials) {
                if (host.equalsIgnoreCase(cred.getHost())) {
                    if (cred.getToken() != null) {
                        return "Bearer " + cred.getToken();
                    }
                    if (cred.getUsername() != null && cred.getPassword() != null) {
                        return basic(cred.getUsername(), cred.getPassword());
                    }
                }
            }
        }

        String path = uri.getPath() == null ? "/" : uri.getPath();
        String regKey = "//" + (host == null ? "" : hostWithPort(uri)) + path;
        while (regKey.length() > 2) {
            String auth = authAt(regKey);
            if (auth != null) {
                return auth;
            }
            if (regKey.endsWith("/")) {
                regKey = regKey.substring(0, regKey.length() - 1);
            } else {
                int lastSlash = regKey.lastIndexOf('/');
                regKey = regKey.substring(0, Math.max(2, lastSlash + 1));
                if ("//".equals(regKey)) {
                    break;
                }
            }
        }

        // Legacy top-level _authToken/_auth apply to whichever registry is in use.
        String token = expanded("_authToken");
        if (token != null) {
            return "Bearer " + token;
        }
        String auth = expanded("_auth");
        if (auth != null) {
            return "Basic " + auth;
        }
        return null;
    }

    private static String hostWithPort(URI uri) {
        return uri.getPort() > 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
    }

    private @Nullable String authAt(String regKey) {
        String token = expanded(regKey + ":_authToken");
        if (token != null) {
            return "Bearer " + token;
        }
        String auth = expanded(regKey + ":_auth");
        if (auth != null) {
            return "Basic " + auth;
        }
        String username = expanded(regKey + ":username");
        String password = expanded(regKey + ":_password");
        if (username != null && password != null) {
            String decoded = new String(Base64.getDecoder().decode(password), StandardCharsets.UTF_8);
            return basic(username, decoded);
        }
        return null;
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private @Nullable String expanded(String key) {
        String value = npmrc.get(key);
        if (value == null) {
            return null;
        }
        Matcher m = ENV_VAR.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String replacement = env.apply(m.group(1));
            if (replacement == null) {
                throw new NpmRegistryException(Reason.CONFIG, null,
                        ".npmrc value for " + key + " references unset environment variable ${" + m.group(1) + "}");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
