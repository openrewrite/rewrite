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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        Set<String> knownNames = new HashSet<>();
        for (PoetryLockPackage pkg : lock.getPackages()) {
            knownNames.add(PythonResolutionResult.normalizeName(pkg.getName()));
        }

        // Pass 1: create all entries, each with a dependencies list to fill in
        // pass 2 (or null when no edge resolves to a locked package).
        List<ResolvedDependency> resolved = new ArrayList<>(lock.getPackages().size());
        Map<String, ResolvedDependency> byName = new LinkedHashMap<>();
        for (PoetryLockPackage pkg : lock.getPackages()) {
            PoetryLockSource source = pkg.getSource();
            boolean anyResolvable = pkg.getDependencies() != null && pkg.getDependencies().stream()
                    .anyMatch(dep -> knownNames.contains(PythonResolutionResult.normalizeName(dep.getName())));
            ResolvedDependency entry = new ResolvedDependency(pkg.getName(), pkg.getVersion(),
                    source != null ? source.getUrl() : null,
                    anyResolvable ? new ArrayList<>() : null);
            resolved.add(entry);
            byName.put(PythonResolutionResult.normalizeName(pkg.getName()), entry);
        }

        // Pass 2: fill each entry's list in place with the shared instances, per
        // the linkage contract on ResolvedDependency#getDependencies().
        for (int i = 0; i < resolved.size(); i++) {
            ResolvedDependency entry = resolved.get(i);
            if (entry.getDependencies() == null) {
                continue;
            }
            for (PoetryLockDependency dep : lock.getPackages().get(i).getDependencies()) {
                ResolvedDependency child = byName.get(PythonResolutionResult.normalizeName(dep.getName()));
                if (child != null) {
                    entry.getDependencies().add(child);
                }
            }
        }
        return resolved;
    }
}
