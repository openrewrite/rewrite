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
package org.openrewrite.python.internal.pipfilelock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.Hashing;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Computes {@code _meta.hash.sha256} with pipenv's exact algorithm: the SHA-256 of
 * {@code json.dumps(data, sort_keys=True, separators=(",", ":")).encode("utf-8")} where
 * {@code data} is the plette-shaped view of the Pipfile
 * ({@code Project.calculate_pipfile_hash}, {@code pipenv/project.py}).
 */
public final class PipfileLockHash {

    public enum HashVariant {
        /**
         * Package names hashed as written; what every pipenv before 2026.4 verifies against
         * (vendored {@code plette.Pipfile.get_hash}).
         */
        LEGACY,
        /**
         * Package names PEP 503-canonicalized before hashing (pipenv >= 2026.4). Agrees with
         * {@link #LEGACY} whenever names are already canonical.
         */
        CANONICAL
    }

    /**
     * Top-level tables that are not dependency categories. Everything else hashes under its own name.
     */
    private static final Set<String> NON_CATEGORY_TABLES = new HashSet<>(Arrays.asList(
            "source", "packages", "dev-packages", "requires", "scripts", "pipfile", "pipenv"));

    private PipfileLockHash() {
    }

    /**
     * @param pipfile         the Pipfile LST; source values are hashed as written
     *                        (env placeholders such as {@code ${VAR}} are NOT expanded)
     * @param fallbackSources sources to hash when the Pipfile has no {@code [[source]]},
     *                        typically the existing lock's {@code _meta.sources}.
     *                        WARNING: passing {@code null} hashes against the pypi.org
     *                        default source, which only reproduces pipenv when the lock
     *                        being regenerated also carries no sources; callers updating
     *                        an existing lock must thread its {@code _meta.sources} here
     *                        or the hash will not verify
     * @param variant         which pipenv hashing variant to reproduce
     * @return the sha256 hex digest pipenv would store under {@code _meta.hash.sha256}
     */
    public static String hash(Toml.Document pipfile,
                              @Nullable List<Map<String, Object>> fallbackSources,
                              HashVariant variant) {
        List<Map<String, Object>> sources = new ArrayList<>();
        Map<String, Object> requires = new LinkedHashMap<>();
        Map<String, Object> defaultPackages = new LinkedHashMap<>();
        Map<String, Object> developPackages = new LinkedHashMap<>();
        Map<String, Map<String, Object>> customCategories = new LinkedHashMap<>();

        for (TomlValue value : pipfile.getValues()) {
            if (!(value instanceof Toml.Table)) {
                continue;
            }
            Toml.Table table = (Toml.Table) value;
            Toml.Identifier name = table.getName();
            if (name == null) {
                continue;
            }
            switch (name.getName()) {
                case "source":
                    sources.add(tableToMap(table));
                    break;
                case "requires":
                    requires = tableToMap(table);
                    break;
                case "packages":
                    defaultPackages = tableToMap(table);
                    break;
                case "dev-packages":
                    developPackages = tableToMap(table);
                    break;
                default:
                    if (!NON_CATEGORY_TABLES.contains(name.getName())) {
                        customCategories.put(name.getName(), tableToMap(table));
                    }
            }
        }

        if (sources.isEmpty()) {
            sources = fallbackSources != null ? fallbackSources : Collections.singletonList(defaultSource());
        }

        if (variant == HashVariant.CANONICAL) {
            defaultPackages = canonicalizeKeys(defaultPackages);
            developPackages = canonicalizeKeys(developPackages);
            for (Map.Entry<String, Map<String, Object>> category : customCategories.entrySet()) {
                category.setValue(canonicalizeKeys(category.getValue()));
            }
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sources", sources);
        meta.put("requires", requires);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_meta", meta);
        data.put("default", defaultPackages);
        data.put("develop", developPackages);
        data.putAll(customCategories);

        return Hashing.sha256Hex(CanonicalJsonEmitter.emit(data).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> defaultSource() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("name", "pypi");
        source.put("url", "https://pypi.org/simple");
        source.put("verify_ssl", true);
        return source;
    }

    /**
     * PEP 503 name normalization.
     */
    static String canonicalize(String name) {
        return Pep508Requirement.canonicalize(name);
    }

    private static Map<String, Object> canonicalizeKeys(Map<String, Object> packages) {
        Map<String, Object> canonicalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : packages.entrySet()) {
            canonicalized.put(canonicalize(entry.getKey()), entry.getValue());
        }
        return canonicalized;
    }

    private static Map<String, Object> tableToMap(Toml.Table table) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Toml value : table.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            if (!(kv.getKey() instanceof Toml.Identifier)) {
                continue;
            }
            map.put(((Toml.Identifier) kv.getKey()).getName(), toJava(kv.getValue()));
        }
        return map;
    }

    private static Object toJava(Toml value) {
        if (value instanceof Toml.Literal) {
            Object v = ((Toml.Literal) value).getValue();
            if (v instanceof String || v instanceof Boolean || v instanceof Long) {
                return v;
            }
            throw new IllegalArgumentException("Unsupported TOML literal in Pipfile: " + ((Toml.Literal) value).getSource());
        }
        if (value instanceof Toml.Table) {
            return tableToMap((Toml.Table) value);
        }
        if (value instanceof Toml.Array) {
            List<Object> list = new ArrayList<>();
            for (Toml element : ((Toml.Array) value).getValues()) {
                if (!(element instanceof Toml.Empty)) {
                    list.add(toJava(element));
                }
            }
            return list;
        }
        throw new IllegalArgumentException("Unsupported TOML value in Pipfile: " + value.getClass().getSimpleName());
    }
}
