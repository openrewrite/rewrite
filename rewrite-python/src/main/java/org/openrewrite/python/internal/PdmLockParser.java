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

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pdmlock.PdmLock;
import org.openrewrite.python.internal.pdmlock.PdmLockFormatException;
import org.openrewrite.python.internal.pdmlock.PdmLockPackage;
import org.openrewrite.python.internal.pdmlock.PdmLockReader;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
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
 * Extracts resolved-dependency information from pdm.lock for overlay onto the
 * {@link PythonResolutionResult} marker. Python resolution is flat, so each package name maps to
 * one {@link ResolvedDependency}; each package's {@code dependencies} PEP 508 strings link the graph.
 */
public class PdmLockParser {

    /**
     * Find and parse the pdm.lock beside (or above) the given pyproject directory.
     */
    public static List<ResolvedDependency> findAndParse(Path pyprojectDir, @Nullable Path boundary) {
        Path lockFile = UvLockParser.findLockFile(pyprojectDir, boundary, "pdm.lock");
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
        PdmLock lock;
        try {
            lock = PdmLockReader.parse(content);
        } catch (PdmLockFormatException e) {
            return Collections.emptyList();
        }

        List<ResolvedDependency> resolved = new ArrayList<>();
        Map<String, ResolvedDependency> byName = new LinkedHashMap<>();
        Map<String, List<String>> edges = new LinkedHashMap<>();

        for (PdmLockPackage pkg : lock.getPackages()) {
            String source = pkg.getVcsUrl() != null ? pkg.getVcsUrl() :
                    pkg.getUrl() != null ? pkg.getUrl() : pkg.getPath();
            ResolvedDependency entry = new ResolvedDependency(pkg.getName(), pkg.getVersion(), source, null);
            resolved.add(entry);
            byName.putIfAbsent(PythonResolutionResult.normalizeName(pkg.getName()), entry);
            if (pkg.getDependencies() != null) {
                List<String> names = new ArrayList<>();
                for (String dep : pkg.getDependencies()) {
                    Pep508Requirement req = Pep508Requirement.parse(dep);
                    if (req != null) {
                        names.add(req.getName());
                    }
                }
                edges.putIfAbsent(PythonResolutionResult.normalizeName(pkg.getName()), names);
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
