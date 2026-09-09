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
import org.openrewrite.python.internal.InstalledEnvParser;
import org.openrewrite.python.internal.PythonDependencyParser;
import org.openrewrite.python.internal.PdmLockParser;
import org.openrewrite.python.internal.PoetryLockParser;
import org.openrewrite.python.internal.PythonResolutionLinker;
import org.openrewrite.python.internal.UvLockParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Parser for pyproject.toml files that delegates to TomlParser and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata.
 */
public class PyProjectTomlParser implements Parser {

    private final TomlParser tomlParser = new TomlParser();
    private final Map<String, String> subprocessEnvironment;

    @Nullable
    private final Path installedEnv;

    public PyProjectTomlParser() {
        this(emptyMap(), null);
    }

    /**
     * @param subprocessEnvironment environment for the {@code uv} resolution subprocess
     * @param installedEnv          an already-installed virtual env for this project's dependencies,
     *                              read in preference to running {@code uv} when no lock file exists
     */
    public PyProjectTomlParser(Map<String, String> subprocessEnvironment, @Nullable Path installedEnv) {
        this.subprocessEnvironment = subprocessEnvironment;
        this.installedEnv = installedEnv;
    }

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

            marker = resolveDependencies(marker, doc, relativeTo);

            return doc.withMarkers(doc.getMarkers().addIfAbsent(marker));
        });
    }

    private PythonResolutionResult resolveDependencies(PythonResolutionResult marker,
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

        PythonResolutionResult.PackageManager pm = marker.getPackageManager();
        List<ResolvedDependency> resolvedDeps;
        boolean lockPresent;
        boolean uvManaged = false;
        if (pm == PythonResolutionResult.PackageManager.Poetry) {
            lockPresent = Files.exists(pyprojectDir.resolve("poetry.lock"));
            resolvedDeps = PoetryLockParser.findAndParse(pyprojectDir, relativeTo);
        } else if (pm == PythonResolutionResult.PackageManager.Pdm) {
            lockPresent = Files.exists(pyprojectDir.resolve("pdm.lock"));
            resolvedDeps = PdmLockParser.findAndParse(pyprojectDir, relativeTo);
        } else {
            lockPresent = Files.exists(pyprojectDir.resolve("uv.lock"));
            resolvedDeps = UvLockParser.findAndParse(pyprojectDir, relativeTo);
            pm = PythonResolutionResult.PackageManager.Uv;
            uvManaged = true;
        }
        if (!resolvedDeps.isEmpty()) {
            return PythonResolutionLinker.applyPyproject(marker, resolvedDeps, pm);
        }
        if (lockPresent) {
            // A lock file exists but yielded nothing — it stays authoritative; don't re-resolve.
            return marker;
        }

        // No lock file: read the caller-installed env if one was provided. Installed dists are
        // ground truth for any package manager.
        if (installedEnv != null) {
            List<ResolvedDependency> installed = InstalledEnvParser.parse(installedEnv);
            if (!installed.isEmpty()) {
                return PythonResolutionLinker.applyPyproject(marker, installed, marker.getPackageManager());
            }
        }
        // Else resolve into a content-hash-cached workspace, the same way requirements.txt and
        // setup.cfg already do — only for uv-managed projects (a Poetry/Pdm pyproject is not
        // valid `uv pip install` input).
        if (uvManaged) {
            try {
                Path workspace = DependencyWorkspace.getOrCreateWorkspace(doc.printAll(), subprocessEnvironment);
                List<ResolvedDependency> frozen = RequirementsTxtParser.parseFreezeOutput(workspace);
                if (!frozen.isEmpty()) {
                    return PythonResolutionLinker.applyPyproject(marker, frozen, pm);
                }
            } catch (RuntimeException e) {
                // uv unavailable or the install failed — leave the marker unresolved, as before.
            }
        }
        return marker;
    }

    @Override
    public boolean accept(Path path) {
        return path.endsWith("pyproject.toml");
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
