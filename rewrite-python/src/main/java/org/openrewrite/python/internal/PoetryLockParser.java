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
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        List<PythonResolutionLinker.UnlinkedPackage> packages = new ArrayList<>(lock.getPackages().size());
        for (PoetryLockPackage pkg : lock.getPackages()) {
            PoetryLockSource source = pkg.getSource();
            List<String> depNames = new ArrayList<>();
            if (pkg.getDependencies() != null) {
                for (PoetryLockDependency dep : pkg.getDependencies()) {
                    depNames.add(dep.getName());
                }
            }
            packages.add(new PythonResolutionLinker.UnlinkedPackage(
                    pkg.getName(), pkg.getVersion(),
                    source != null ? source.getUrl() : null, depNames));
        }
        return PythonResolutionLinker.buildGraph(packages);
    }
}
