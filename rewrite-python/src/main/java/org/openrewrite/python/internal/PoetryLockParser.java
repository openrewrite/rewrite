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
package org.openrewrite.python.internal;

import org.openrewrite.python.internal.poetrylock.PoetryLock;
import org.openrewrite.python.internal.poetrylock.PoetryLockDependency;
import org.openrewrite.python.internal.poetrylock.PoetryLockFormatException;
import org.openrewrite.python.internal.poetrylock.PoetryLockPackage;
import org.openrewrite.python.internal.poetrylock.PoetryLockReader;
import org.openrewrite.python.internal.poetrylock.PoetryLockSource;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts resolved-dependency information from poetry.lock for overlay onto the
 * {@link PythonResolutionResult} marker. Python resolution is flat, so each package name maps to
 * exactly one {@link ResolvedDependency}; {@code [package.dependencies]} edges link the graph.
 */
public class PoetryLockParser {

    /**
     * Find and parse the poetry.lock beside (or above) the given pyproject directory.
     */
    public static List<ResolvedDependency> findAndParse(Path pyprojectDir, @Nullable Path boundary) {
        Path lockFile = UvLockParser.findLockFile(pyprojectDir, boundary, "poetry.lock");
        if (lockFile == null) {
            return Collections.emptyList();
        }
        try {
            return parse(new String(Files.readAllBytes(lockFile), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static List<ResolvedDependency> parse(String content) {
        PoetryLock lock;
        try {
            lock = PoetryLockReader.parse(content);
        } catch (PoetryLockFormatException e) {
            return Collections.emptyList();
        }

        List<ResolvedDependency> resolved = new ArrayList<>();
        Map<String, ResolvedDependency> byName = new LinkedHashMap<>();
        Map<String, List<String>> edges = new LinkedHashMap<>();

        for (PoetryLockPackage pkg : lock.getPackages()) {
            PoetryLockSource source = pkg.getSource();
            ResolvedDependency entry = new ResolvedDependency(pkg.getName(), pkg.getVersion(),
                    source != null ? source.getUrl() : null, null);
            resolved.add(entry);
            byName.put(PythonResolutionResult.normalizeName(pkg.getName()), entry);
            if (pkg.getDependencies() != null) {
                List<String> names = new ArrayList<>();
                for (PoetryLockDependency dep : pkg.getDependencies()) {
                    names.add(dep.getName());
                }
                edges.put(PythonResolutionResult.normalizeName(pkg.getName()), names);
            }
        }

        List<ResolvedDependency> linked = new ArrayList<>(resolved.size());
        for (ResolvedDependency entry : resolved) {
            List<String> names = edges.get(PythonResolutionResult.normalizeName(entry.getName()));
            if (names == null) {
                linked.add(entry);
                continue;
            }
            List<ResolvedDependency> deps = new ArrayList<>();
            for (String name : names) {
                ResolvedDependency dep = byName.get(PythonResolutionResult.normalizeName(name));
                if (dep != null) {
                    deps.add(dep);
                }
            }
            linked.add(entry.withDependencies(deps.isEmpty() ? null : deps));
        }
        return linked;
    }
}
