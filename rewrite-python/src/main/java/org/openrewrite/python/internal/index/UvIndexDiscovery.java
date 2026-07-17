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
import org.openrewrite.SourceFile;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonIndexCredentials;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

/**
 * Resolves the package indexes uv would lock against, mirroring uv's precedence:
 * host-supplied indexes on the {@link PythonExecutionContextView} win outright;
 * otherwise env-var indexes ({@code UV_DEFAULT_INDEX}, {@code UV_INDEX}) precede
 * config-file {@code [[tool.uv.index]]} entries (project {@code uv.toml} over
 * {@code pyproject.toml [tool.uv]}, then user- and system-level {@code uv.toml}),
 * which precede the legacy {@code extra-index-url} then {@code index-url} keys;
 * pypi.org is the fallback default. Named indexes are deduplicated first-wins, the
 * default index always sorts last, and {@code find-links} flat locations follow it.
 * <p>
 * Unlike pipenv, uv performs no environment-variable expansion inside configuration
 * values; URLs are taken literally.
 */
public final class UvIndexDiscovery {
    private static final String DEFAULT_INDEX_URL = "https://pypi.org/simple";

    private UvIndexDiscovery() {
    }

    public static List<UvIndex> discover(ExecutionContext ctx,
                                         Toml.@Nullable Document pyproject,
                                         @Nullable Path projectDir,
                                         Environment env) {
        PythonExecutionContextView view = PythonExecutionContextView.view(ctx);
        List<PythonPackageIndex> override = view.getPackageIndexes();
        if (!override.isEmpty()) {
            List<UvIndex> wrapped = new ArrayList<>(override.size());
            for (PythonPackageIndex index : override) {
                wrapped.add(new UvIndex(index, true, false, false, false));
            }
            return wrapped;
        }

        List<Entry> entries = new ArrayList<>();
        List<Entry> extras = new ArrayList<>();
        List<Entry> flat = new ArrayList<>();
        Entry legacyDefault = null;

        // Env vars are uv's CLI-argument equivalents: UV_DEFAULT_INDEX leads the list
        // as the default, UV_INDEX entries precede all config-file entries.
        String envDefault = emptyToNull(env.getenv("UV_DEFAULT_INDEX"));
        if (envDefault != null) {
            entries.add(parseEnvIndex(envDefault, projectDir).asDefault());
        }
        String envIndex = emptyToNull(env.getenv("UV_INDEX"));
        if (envIndex != null) {
            for (String token : PipConf.splitWhitespace(envIndex)) {
                entries.add(parseEnvIndex(token, projectDir));
            }
        }
        String envExtra = emptyToNull(env.getenv("UV_EXTRA_INDEX_URL"));
        if (envExtra != null) {
            for (String url : PipConf.splitWhitespace(envExtra)) {
                extras.add(new Entry(null, url, false, false, false, projectDir));
            }
        }
        String envIndexUrl = emptyToNull(env.getenv("UV_INDEX_URL"));
        if (envIndexUrl != null) {
            legacyDefault = new Entry(null, envIndexUrl, true, false, false, projectDir);
        }
        String envFindLinks = emptyToNull(env.getenv("UV_FIND_LINKS"));
        if (envFindLinks != null) {
            for (String location : envFindLinks.split(",")) {
                if (!location.trim().isEmpty()) {
                    flat.add(new Entry(null, location.trim(), false, false, true, projectDir));
                }
            }
        }

        if (!boolish(env.getenv("UV_NO_CONFIG"))) {
            for (Config config : loadConfigs(pyproject, projectDir, env)) {
                entries.addAll(config.indexes);
                extras.addAll(config.extras);
                if (legacyDefault == null) {
                    legacyDefault = config.indexUrl;
                }
                flat.addAll(config.findLinks);
            }
        }

        entries.addAll(extras);
        if (legacyDefault != null) {
            entries.add(legacyDefault);
        }
        return finalize(entries, flat, view, env);
    }

    /**
     * Package name (PEP 503 canonical) to index names pinned via {@code [tool.uv.sources]}
     * entries of the form {@code pkg = { index = "name" }} (multiple when marker-gated).
     * Feed a package's pins to {@link UvIndex#usableFor(String)}.
     */
    public static Map<String, List<String>> sourceIndexPins(Toml.@Nullable Document pyproject) {
        if (pyproject == null) {
            return emptyMap();
        }
        Map<String, List<String>> pins = new LinkedHashMap<>();
        for (Toml value : pyproject.getValues()) {
            if (!(value instanceof Toml.Table)) {
                continue;
            }
            Toml.Table table = (Toml.Table) value;
            String name = tableName(table);
            if ("tool.uv.sources".equals(name)) {
                for (Toml sourceValue : table.getValues()) {
                    if (!(sourceValue instanceof Toml.KeyValue)) {
                        continue;
                    }
                    Toml.KeyValue kv = (Toml.KeyValue) sourceValue;
                    String packageName = keyName(kv);
                    if (packageName != null) {
                        addPins(pins, packageName, kv.getValue());
                    }
                }
            } else if (name != null && name.startsWith("tool.uv.sources.")) {
                String packageName = unquote(name.substring("tool.uv.sources.".length()));
                String index = literalString(table, "index");
                if (index != null) {
                    pin(pins, packageName, index);
                }
            }
        }
        return pins;
    }

    private static void addPins(Map<String, List<String>> pins, String packageName, Toml value) {
        if (value instanceof Toml.Table) {
            String index = literalString((Toml.Table) value, "index");
            if (index != null) {
                pin(pins, packageName, index);
            }
        } else if (value instanceof Toml.Array) {
            for (Toml element : ((Toml.Array) value).getValues()) {
                if (element instanceof Toml.Table) {
                    String index = literalString((Toml.Table) element, "index");
                    if (index != null) {
                        pin(pins, packageName, index);
                    }
                }
            }
        }
    }

    private static void pin(Map<String, List<String>> pins, String packageName, String index) {
        String canonical = SimpleIndexClient.canonicalName(packageName);
        List<String> names = pins.get(canonical);
        if (names == null) {
            names = new ArrayList<>();
            pins.put(canonical, names);
        }
        names.add(index);
    }

    private static List<UvIndex> finalize(List<Entry> entries, List<Entry> flat,
                                          PythonExecutionContextView view, Environment env) {
        Set<String> seen = new HashSet<>();
        List<Entry> implicit = new ArrayList<>();
        Entry defaultEntry = null;
        for (Entry entry : entries) {
            if (entry.name != null && !seen.add(entry.name)) {
                continue;
            }
            if (entry.defaultIndex) {
                // first default wins; later defaults (e.g. a config default shadowed by
                // UV_DEFAULT_INDEX) are dropped, matching uv's IndexLocations
                if (defaultEntry == null) {
                    defaultEntry = entry;
                }
                continue;
            }
            implicit.add(entry);
        }
        List<UvIndex> result = new ArrayList<>(implicit.size() + 1 + flat.size());
        for (Entry entry : implicit) {
            result.add(toUvIndex(entry, false, view, env));
        }
        if (defaultEntry != null) {
            result.add(toUvIndex(defaultEntry, true, view, env));
        } else {
            result.add(new UvIndex(new PythonPackageIndex("pypi", DEFAULT_INDEX_URL, true, null, null, false),
                    false, true, false, false));
        }
        for (Entry entry : flat) {
            result.add(toUvIndex(entry, false, view, env));
        }
        return result;
    }

    private static UvIndex toUvIndex(Entry entry, boolean defaultIndex,
                                     PythonExecutionContextView view, Environment env) {
        String url = resolveUrl(entry);
        String displayName = entry.name != null ? entry.name : defaultName(url);
        PythonPackageIndex index = new PythonPackageIndex(displayName, url, true, null, null, false);
        index = fillCredentials(index, entry.name, view.getIndexCredentials(), env);
        return new UvIndex(index, entry.name != null, defaultIndex, entry.explicit, entry.flat);
    }

    private static String resolveUrl(Entry entry) {
        if (entry.url.contains("://")) {
            return entry.url;
        }
        Path path = Paths.get(entry.url);
        if (!path.isAbsolute() && entry.baseDir != null) {
            // uv resolves relative paths against the directory of the defining config file
            return entry.baseDir.resolve(path).normalize().toString();
        }
        return entry.url;
    }

    private static PythonPackageIndex fillCredentials(PythonPackageIndex index, @Nullable String declaredName,
                                                      List<PythonIndexCredentials> credentials, Environment env) {
        // uv prefers UV_INDEX_{NAME}_USERNAME/PASSWORD over URL-embedded credentials
        if (declaredName != null) {
            String key = toEnvVar(declaredName);
            String username = env.getenv("UV_INDEX_" + key + "_USERNAME");
            String password = env.getenv("UV_INDEX_" + key + "_PASSWORD");
            if (username != null || password != null) {
                return index.withUsername(username).withPassword(password);
            }
        }
        return IndexDiscovery.fillFromUrlOrHost(index, credentials, env);
    }

    /**
     * uv's {@code IndexName::to_env_var}: ASCII alphanumerics uppercased, everything
     * else becomes {@code _}.
     */
    static String toEnvVar(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static Entry parseEnvIndex(String token, @Nullable Path baseDir) {
        // uv's Index::from_str accepts `name=url` when the name part has no colon
        int eq = token.indexOf('=');
        if (eq > 0 && isIndexName(token.substring(0, eq))) {
            return new Entry(token.substring(0, eq), token.substring(eq + 1), false, false, false, baseDir);
        }
        return new Entry(null, token, false, false, false, baseDir);
    }

    private static boolean isIndexName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                    c == '-' || c == '_' || c == '.')) {
                return false;
            }
        }
        return !name.isEmpty();
    }

    private static List<Config> loadConfigs(Toml.@Nullable Document pyproject, @Nullable Path projectDir,
                                            Environment env) {
        List<Config> configs = new ArrayList<>(3);
        String explicitFile = emptyToNull(env.getenv("UV_CONFIG_FILE"));
        if (explicitFile != null) {
            // UV_CONFIG_FILE replaces the entire discovered chain
            Config config = parseUvTomlFile(Paths.get(explicitFile));
            if (config != null) {
                configs.add(config);
            }
            return configs;
        }
        Config project = projectDir != null ? parseUvTomlFile(projectDir.resolve("uv.toml")) : null;
        if (project == null && pyproject != null) {
            // a uv.toml next to the pyproject masks its [tool.uv] table entirely
            project = readConfig(pyproject, "tool.uv", projectDir);
        }
        if (project != null) {
            configs.add(project);
        }
        Config user = parseUvTomlFile(userConfigDir(env).resolve("uv").resolve("uv.toml"));
        if (user != null) {
            configs.add(user);
        }
        if (!boolish(env.getenv("UV_NO_SYSTEM_CONFIG"))) {
            Path systemFile = systemConfigFile(env);
            Config system = systemFile != null ? parseUvTomlFile(systemFile) : null;
            if (system != null) {
                configs.add(system);
            }
        }
        return configs;
    }

    private static Path userConfigDir(Environment env) {
        if (env.osName().toLowerCase(Locale.ROOT).contains("win")) {
            String appData = env.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return Paths.get(appData);
            }
        }
        String xdg = env.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isEmpty()) {
            return Paths.get(xdg);
        }
        return env.userHome().resolve(".config");
    }

    private static @Nullable Path systemConfigFile(Environment env) {
        if (env.osName().toLowerCase(Locale.ROOT).contains("win")) {
            String programData = env.getenv("PROGRAMDATA");
            return programData != null && !programData.isEmpty() ?
                    Paths.get(programData).resolve("uv").resolve("uv.toml") : null;
        }
        String xdg = env.getenv("XDG_CONFIG_DIRS");
        String dirs = xdg != null && !xdg.isEmpty() ? xdg : "/etc/xdg";
        for (String dir : dirs.split(":")) {
            if (dir.isEmpty()) {
                continue;
            }
            Path candidate = Paths.get(dir).resolve("uv").resolve("uv.toml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return Paths.get("/etc/uv/uv.toml");
    }

    private static @Nullable Config parseUvTomlFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        String content;
        try {
            content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
        Optional<SourceFile> parsed = new TomlParser().parse(content).findFirst();
        if (!parsed.isPresent() || !(parsed.get() instanceof Toml.Document)) {
            return null;
        }
        return readConfig((Toml.Document) parsed.get(), "", file.toAbsolutePath().getParent());
    }

    private static Config readConfig(Toml.Document doc, String scope, @Nullable Path baseDir) {
        String indexTable = scope.isEmpty() ? "index" : scope + ".index";
        List<Entry> indexes = new ArrayList<>();
        List<Entry> extras = new ArrayList<>();
        List<Entry> findLinks = new ArrayList<>();
        Entry indexUrl = null;
        for (Toml value : doc.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                String name = tableName(table);
                if (indexTable.equals(name)) {
                    Entry entry = parseIndexTable(table, baseDir);
                    if (entry != null) {
                        indexes.add(entry);
                    }
                } else if (scope.equals(name)) {
                    indexUrl = readScopedKeys(table.getValues(), baseDir, indexes, extras, findLinks, indexUrl);
                }
            } else if (scope.isEmpty() && value instanceof Toml.KeyValue) {
                indexUrl = readScopedKeys(singletonList(value), baseDir, indexes, extras, findLinks, indexUrl);
            }
        }
        return new Config(indexes, extras, indexUrl, findLinks);
    }

    private static @Nullable Entry readScopedKeys(List<? extends Toml> values, @Nullable Path baseDir,
                                                  List<Entry> indexes, List<Entry> extras,
                                                  List<Entry> findLinks, @Nullable Entry indexUrl) {
        for (Toml value : values) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            String key = keyName(kv);
            if ("index".equals(key) && kv.getValue() instanceof Toml.Array) {
                for (Toml element : ((Toml.Array) kv.getValue()).getValues()) {
                    if (element instanceof Toml.Table) {
                        Entry entry = parseIndexTable((Toml.Table) element, baseDir);
                        if (entry != null) {
                            indexes.add(entry);
                        }
                    }
                }
            } else if ("index-url".equals(key)) {
                String url = literalString(kv.getValue());
                if (url != null && indexUrl == null) {
                    indexUrl = new Entry(null, url, true, false, false, baseDir);
                }
            } else if ("extra-index-url".equals(key) && kv.getValue() instanceof Toml.Array) {
                for (String url : stringList((Toml.Array) kv.getValue())) {
                    extras.add(new Entry(null, url, false, false, false, baseDir));
                }
            } else if ("find-links".equals(key) && kv.getValue() instanceof Toml.Array) {
                for (String location : stringList((Toml.Array) kv.getValue())) {
                    findLinks.add(new Entry(null, location, false, false, true, baseDir));
                }
            }
        }
        return indexUrl;
    }

    private static @Nullable Entry parseIndexTable(Toml.Table table, @Nullable Path baseDir) {
        String url = literalString(table, "url");
        if (url == null) {
            return null;
        }
        String name = literalString(table, "name");
        Boolean defaultIndex = literalBoolean(table, "default");
        Boolean explicit = literalBoolean(table, "explicit");
        String format = literalString(table, "format");
        return new Entry(name, url,
                Boolean.TRUE.equals(defaultIndex),
                Boolean.TRUE.equals(explicit),
                "flat".equalsIgnoreCase(format),
                baseDir);
    }

    private static @Nullable String tableName(Toml.Table table) {
        Toml.Identifier name = table.getName();
        return name != null ? name.getName() : null;
    }

    private static @Nullable String keyName(Toml.KeyValue kv) {
        if (kv.getKey() instanceof Toml.Identifier) {
            return unquote(((Toml.Identifier) kv.getKey()).getName());
        }
        return null;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static @Nullable String literalString(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (key.equals(keyName(kv))) {
                    return literalString(kv.getValue());
                }
            }
        }
        return null;
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

    private static @Nullable Boolean literalBoolean(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (key.equals(keyName(kv)) && kv.getValue() instanceof Toml.Literal) {
                    Object v = ((Toml.Literal) kv.getValue()).getValue();
                    if (v instanceof Boolean) {
                        return (Boolean) v;
                    }
                }
            }
        }
        return null;
    }

    private static List<String> stringList(Toml.Array array) {
        List<String> strings = new ArrayList<>();
        for (Toml element : array.getValues()) {
            String s = literalString(element);
            if (s != null) {
                strings.add(s);
            }
        }
        return strings;
    }

    private static @Nullable String emptyToNull(@Nullable String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static boolean boolish(@Nullable String s) {
        return s != null && !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
    }

    private static String defaultName(String url) {
        String host = Urls.host(url);
        return host != null ? host : url;
    }

    private static final class Entry {
        final @Nullable String name;
        final String url;
        final boolean defaultIndex;
        final boolean explicit;
        final boolean flat;
        final @Nullable Path baseDir;

        Entry(@Nullable String name, String url, boolean defaultIndex, boolean explicit, boolean flat,
              @Nullable Path baseDir) {
            this.name = name;
            this.url = url;
            this.defaultIndex = defaultIndex;
            this.explicit = explicit;
            this.flat = flat;
            this.baseDir = baseDir;
        }

        Entry asDefault() {
            return new Entry(name, url, true, explicit, flat, baseDir);
        }
    }

    private static final class Config {
        final List<Entry> indexes;
        final List<Entry> extras;
        final @Nullable Entry indexUrl;
        final List<Entry> findLinks;

        Config(List<Entry> indexes, List<Entry> extras, @Nullable Entry indexUrl, List<Entry> findLinks) {
            this.indexes = indexes;
            this.extras = extras;
            this.indexUrl = indexUrl;
            this.findLinks = findLinks;
        }
    }
}
