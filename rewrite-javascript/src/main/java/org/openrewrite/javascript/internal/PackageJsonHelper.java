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
package org.openrewrite.javascript.internal;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.*;

/**
 * Shared utilities for npm dependency recipes operating on package.json files
 * and their associated lock files.
 */
@UtilityClass
public class PackageJsonHelper {

    private static final Set<String> LOCK_FILE_NAMES = new LinkedHashSet<>(Arrays.asList(
            "package-lock.json", "pnpm-lock.yaml", "yarn.lock", "bun.lock"));

    /** True when the basename matches one of the recognised npm-ecosystem lock files. */
    public static boolean isLockFile(String basename) {
        return LOCK_FILE_NAMES.contains(basename);
    }

    /** Map a lock file path to the sibling {@code package.json}. */
    public static Path correspondingPackageJsonPath(Path lockFilePath) {
        return lockFilePath.resolveSibling("package.json");
    }

    // --- ctx side-channel for cross-recipe chaining ----------------------

    private static final String LIVE_PACKAGE_JSON_TREES =
            "org.openrewrite.javascript.livePackageJsonTrees";

    @SuppressWarnings("unchecked")
    public static @Nullable SourceFile getLiveTree(ExecutionContext ctx, Path packageJsonPath) {
        Map<Path, SourceFile> map = (Map<Path, SourceFile>) ctx.getMessage(LIVE_PACKAGE_JSON_TREES);
        return map == null ? null : map.get(packageJsonPath);
    }

    public static void putLiveTree(ExecutionContext ctx, Path packageJsonPath, SourceFile tree) {
        Map<Path, SourceFile> map = ctx.computeMessageIfAbsent(LIVE_PACKAGE_JSON_TREES, k -> new HashMap<>());
        map.put(packageJsonPath, tree);
    }

    // --- .npmrc serialization -------------------------------------------

    /**
     * Serialize the marker's npmrc configs into a {@code Map<filename, content>}
     * suitable to seed a temp directory before running the package manager.
     * Returns {@code null} if there are no configs.
     */
    public static @Nullable Map<String, String> serializeConfigFiles(NodeResolutionResult marker) {
        List<Npmrc> configs = marker.getNpmrcConfigs();
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        // Merge configs in scope priority (Global → User → Project; later overrides earlier).
        Map<String, String> merged = new LinkedHashMap<>();
        List<NpmrcScope> order = Arrays.asList(NpmrcScope.Global, NpmrcScope.User, NpmrcScope.Project);
        for (NpmrcScope scope : order) {
            for (Npmrc cfg : configs) {
                if (cfg.getScope() == scope && cfg.getProperties() != null) {
                    merged.putAll(cfg.getProperties());
                }
            }
        }
        if (merged.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : merged.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        Map<String, String> out = new LinkedHashMap<>();
        out.put(".npmrc", sb.toString());
        return out;
    }

    // --- Reparse helpers (preserve identity + markers) ------------------

    public static Json.Document reparseJson(Json.Document original, String newContent) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        Optional<SourceFile> parsed = parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).findFirst();
        if (parsed.isPresent() && parsed.get() instanceof Json.Document) {
            Json.Document doc = (Json.Document) parsed.get();
            return doc.withId(original.getId()).withMarkers(original.getMarkers());
        }
        return original;
    }

    public static Yaml.Documents reparseYaml(Yaml.Documents original, String newContent) {
        YamlParser parser = new YamlParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        Optional<SourceFile> parsed = parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).findFirst();
        if (parsed.isPresent() && parsed.get() instanceof Yaml.Documents) {
            Yaml.Documents docs = (Yaml.Documents) parsed.get();
            return docs.withId(original.getId()).withMarkers(original.getMarkers());
        }
        return original;
    }

    public static PlainText reparsePlainText(PlainText original, String newContent) {
        return original.withText(newContent);
    }

    /**
     * Reparse a regenerated lock file's content, dispatching by the runtime type
     * of the original SourceFile.
     */
    public static SourceFile reparseLock(SourceFile original, String newContent) {
        if (original instanceof Json.Document) {
            return reparseJson((Json.Document) original, newContent);
        }
        if (original instanceof Yaml.Documents) {
            return reparseYaml((Yaml.Documents) original, newContent);
        }
        if (original instanceof PlainText) {
            return reparsePlainText((PlainText) original, newContent);
        }
        return original;
    }
}
