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
package org.openrewrite.python.internal.index;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonIndexCredentials;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.toml.tree.Toml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * Resolves the package indexes to lock against, mirroring pipenv's population order:
 * host-supplied indexes on the {@link PythonExecutionContextView}, then Pipfile
 * {@code [[source]]} blocks, then the existing lock's {@code _meta.sources}, then
 * {@code PIP_INDEX_URL}/{@code PIP_EXTRA_INDEX_URL}, then pip.conf, then pypi.org.
 */
public final class IndexDiscovery {
    private static final String DEFAULT_INDEX_URL = "https://pypi.org/simple";

    private IndexDiscovery() {
    }

    public static List<PythonPackageIndex> discover(ExecutionContext ctx,
                                                    Toml.@Nullable Document pipfile,
                                                    @Nullable List<Map<String, Object>> lockMetaSources,
                                                    Environment env) {
        PythonExecutionContextView view = PythonExecutionContextView.view(ctx);
        List<PythonPackageIndex> explicit = view.getPackageIndexes();
        if (!explicit.isEmpty()) {
            return explicit;
        }
        List<PythonPackageIndex> indexes = fromPipfile(pipfile, env);
        if (indexes.isEmpty()) {
            indexes = fromLockSources(lockMetaSources, env);
        }
        if (indexes.isEmpty()) {
            indexes = fromPipConfiguration(env);
        }
        List<PythonPackageIndex> withCredentials = new ArrayList<>(indexes.size());
        for (PythonPackageIndex index : indexes) {
            withCredentials.add(fillCredentials(index, view.getIndexCredentials(), env));
        }
        return withCredentials;
    }

    private static List<PythonPackageIndex> fromPipfile(Toml.@Nullable Document pipfile, Environment env) {
        if (pipfile == null) {
            return emptyList();
        }
        List<PythonPackageIndex> sources = new ArrayList<>();
        for (Toml value : pipfile.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                Toml.Identifier name = table.getName();
                if (name != null && "source".equals(name.getName())) {
                    PythonPackageIndex index = fromSourceTable(table, env);
                    if (index != null) {
                        sources.add(index);
                    }
                }
            }
        }
        return sources;
    }

    private static @Nullable PythonPackageIndex fromSourceTable(Toml.Table table, Environment env) {
        String name = null;
        String url = null;
        Boolean verifySsl = null;
        String verifySslText = null;
        for (Toml value : table.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            if (!(kv.getKey() instanceof Toml.Identifier)) {
                continue;
            }
            String key = ((Toml.Identifier) kv.getKey()).getName();
            if ("name".equals(key)) {
                name = literalString(kv.getValue());
            } else if ("url".equals(key)) {
                url = literalString(kv.getValue());
            } else if ("verify_ssl".equals(key) && kv.getValue() instanceof Toml.Literal) {
                Object v = ((Toml.Literal) kv.getValue()).getValue();
                if (v instanceof Boolean) {
                    verifySsl = (Boolean) v;
                } else if (v instanceof String) {
                    verifySslText = (String) v;
                }
            }
        }
        if (url == null) {
            return null;
        }
        EnvExpansion.Expansion expanded = EnvExpansion.expand(url, env);
        boolean ssl = verifySsl != null ? verifySsl :
                verifySslText == null || !"false".equalsIgnoreCase(EnvExpansion.expandVars(verifySslText, env).trim());
        String expandedName = name != null ? EnvExpansion.expandVars(name, env) : defaultName(expanded.url);
        return new PythonPackageIndex(expandedName, expanded.url, ssl, null, null,
                expanded.unresolvedPlaceholders);
    }

    private static List<PythonPackageIndex> fromLockSources(@Nullable List<Map<String, Object>> lockMetaSources,
                                                            Environment env) {
        if (lockMetaSources == null) {
            return emptyList();
        }
        List<PythonPackageIndex> sources = new ArrayList<>();
        for (Map<String, Object> source : lockMetaSources) {
            Object url = source.get("url");
            if (!(url instanceof String)) {
                continue;
            }
            // _meta.sources are written with env placeholders unexpanded, so expand like a Pipfile source
            EnvExpansion.Expansion expanded = EnvExpansion.expand((String) url, env);
            Object name = source.get("name");
            Object verifySsl = source.get("verify_ssl");
            sources.add(new PythonPackageIndex(
                    name instanceof String ? (String) name : defaultName(expanded.url),
                    expanded.url,
                    !(verifySsl instanceof Boolean) || (Boolean) verifySsl,
                    null,
                    null,
                    expanded.unresolvedPlaceholders));
        }
        return sources;
    }

    private static List<PythonPackageIndex> fromPipConfiguration(Environment env) {
        String indexUrl = env.getenv("PIP_INDEX_URL");
        String extra = env.getenv("PIP_EXTRA_INDEX_URL");
        List<String> extraUrls = extra != null ? PipConf.splitWhitespace(extra) : null;
        if (indexUrl == null || extraUrls == null) {
            PipConf conf = PipConf.load(env);
            if (indexUrl == null) {
                indexUrl = conf.getIndexUrl();
            }
            if (extraUrls == null) {
                extraUrls = conf.getExtraIndexUrls();
            }
        }
        List<PythonPackageIndex> indexes = new ArrayList<>();
        // pipenv names its derived default source "pypi" regardless of the URL
        indexes.add(new PythonPackageIndex("pypi", indexUrl != null ? indexUrl : DEFAULT_INDEX_URL,
                true, null, null, false));
        for (String extraUrl : extraUrls) {
            indexes.add(new PythonPackageIndex(defaultName(extraUrl), extraUrl, true, null, null, false));
        }
        return indexes;
    }

    private static PythonPackageIndex fillCredentials(PythonPackageIndex index,
                                                      List<PythonIndexCredentials> credentials, Environment env) {
        if (index.getUsername() != null || index.isUnresolvedPlaceholders()) {
            return index;
        }
        return fillFromUrlOrHost(index, credentials, env);
    }

    /**
     * Shared credential fill: URL-embedded userinfo, then host-matched view
     * credentials, then netrc. Also used by {@link UvIndexDiscovery}.
     */
    static PythonPackageIndex fillFromUrlOrHost(PythonPackageIndex index,
                                                List<PythonIndexCredentials> credentials, Environment env) {
        int[] userinfo = Urls.userinfoRange(index.getUrl());
        if (userinfo != null) {
            String raw = index.getUrl().substring(userinfo[0], userinfo[1]);
            int colon = raw.indexOf(':');
            String username = colon < 0 ? raw : raw.substring(0, colon);
            String password = colon < 0 ? null : raw.substring(colon + 1);
            return index.withUsername(EnvExpansion.percentDecode(username))
                    .withPassword(password == null ? null : EnvExpansion.percentDecode(password));
        }
        String host = Urls.host(index.getUrl());
        if (host != null) {
            for (PythonIndexCredentials credential : credentials) {
                if (host.equalsIgnoreCase(credential.getHost())) {
                    return index.withUsername(credential.getUsername()).withPassword(credential.getPassword());
                }
            }
            Netrc.Login login = Netrc.find(env, host);
            if (login != null) {
                return index.withUsername(login.getLogin()).withPassword(login.getPassword());
            }
        }
        return index;
    }

    private static @Nullable String literalString(Toml value) {
        if (value instanceof Toml.Literal) {
            Object v = ((Toml.Literal) value).getValue();
            if (v instanceof String) {
                return (String) v;
            }
        }
        return null;
    }

    private static String defaultName(String url) {
        String host = Urls.host(url);
        return host != null ? host : "source";
    }
}
