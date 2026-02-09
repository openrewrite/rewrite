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
package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.PythonDependencyParser;
import org.openrewrite.python.internal.UvLockParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parser for pyproject.toml files that delegates to TomlParser and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata.
 */
public class PyProjectTomlParser implements Parser {

    private final TomlParser tomlParser = new TomlParser();

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return tomlParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof Toml.Document)) {
                return sf;
            }
            Toml.Document doc = (Toml.Document) sf;
            PythonResolutionResult marker = PythonDependencyParser.createMarker(doc, relativeTo);
            if (marker == null) {
                return sf;
            }

            // Try to resolve dependencies from uv.lock
            marker = resolveFromLockFile(marker, doc, relativeTo);

            return doc.withMarkers(doc.getMarkers().addIfAbsent(marker));
        });
    }

    private PythonResolutionResult resolveFromLockFile(PythonResolutionResult marker,
                                                       Toml.Document doc,
                                                       @Nullable Path relativeTo) {
        Path sourcePath = doc.getSourcePath();
        Path pyprojectDir;
        if (relativeTo != null) {
            pyprojectDir = relativeTo.resolve(sourcePath).getParent();
        } else {
            pyprojectDir = sourcePath.getParent();
        }

        if (pyprojectDir == null) {
            return marker;
        }

        List<ResolvedDependency> resolvedDeps = UvLockParser.findAndParse(pyprojectDir, relativeTo);
        if (resolvedDeps.isEmpty()) {
            return marker;
        }

        marker = marker.withResolvedDependencies(resolvedDeps);

        // Link declared dependencies to their resolved versions
        marker = marker.withDependencies(linkResolved(marker.getDependencies(), resolvedDeps));
        marker = marker.withBuildRequires(linkResolved(marker.getBuildRequires(), resolvedDeps));

        return marker;
    }

    private List<Dependency> linkResolved(List<Dependency> deps, List<ResolvedDependency> resolved) {
        return deps.stream().map(dep -> {
            String normalizedName = PythonResolutionResult.normalizeName(dep.getName());
            ResolvedDependency found = resolved.stream()
                    .filter(r -> PythonResolutionResult.normalizeName(r.getName()).equals(normalizedName))
                    .findFirst()
                    .orElse(null);
            return found != null ? dep.withResolved(found) : dep;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean accept(Path path) {
        return "pyproject.toml".equals(path.getFileName().toString());
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("pyproject.toml");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(Toml.Document.class);
        }

        @Override
        public PyProjectTomlParser build() {
            return new PyProjectTomlParser();
        }

        @Override
        public String getDslName() {
            return "pyproject.toml";
        }
    }
}
