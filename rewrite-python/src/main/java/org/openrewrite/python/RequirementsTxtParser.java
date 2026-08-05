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
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.InstalledEnvParser;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

/**
 * Parser for requirements.txt files that delegates to {@link PlainTextParser} and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata resolved via {@code uv pip freeze}.
 */
public class RequirementsTxtParser implements Parser {

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "requirements(-[\\w-]+)?\\.(txt|in)"
    );

    private final PlainTextParser plainTextParser = new PlainTextParser();
    private final Map<String, String> subprocessEnvironment;

    public RequirementsTxtParser() {
        this(emptyMap());
    }

    public RequirementsTxtParser(Map<String, String> subprocessEnvironment) {
        this.subprocessEnvironment = subprocessEnvironment;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return plainTextParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf.withMarkers(sf.getMarkers().add(ParseExceptionResult.build(this, new UnsupportedOperationException(), "Creating a PythonResolutionResult can only be done for PlainText LST elements.")));
            }
            PlainText text = (PlainText) sf;

            @Nullable Path originalFilePath = null;
            if (relativeTo != null) {
                originalFilePath = relativeTo.resolve(text.getSourcePath());
            }
            Path workspace = DependencyWorkspace.getOrCreateRequirementsWorkspace(
                    text.getText(), originalFilePath, subprocessEnvironment);
            if (workspace == null) {
                return sf.withMarkers(sf.getMarkers().add(ParseExceptionResult.build(this, new UnsupportedOperationException(),
                        "Failed to create the PythonResolutionResult due to a failure to install the requirement file. " +
                                "Perhaps you are missing `uv` in the environment or trying to build a requirement text file containing dependencies which are not available?")));
            }

            List<ResolvedDependency> resolvedDeps = parseFreezeOutput(workspace);
            if (resolvedDeps.isEmpty()) {
                return sf.withMarkers(sf.getMarkers().add(ParseExceptionResult.build(this, new UnsupportedOperationException(),
                        "Failed to create the PythonResolutionResult: no resolved dependencies.")));
            }

            List<Dependency> deps = dependenciesFromResolved(resolvedDeps,
                    parseDeclaredPackageNames(text.getText()));

            PythonResolutionResult marker = new PythonResolutionResult(
                    randomId(),
                    null,
                    null,
                    null,
                    null,
                    text.getSourcePath().toString(),
                    null,
                    null,
                    Collections.emptyList(),
                    deps,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    resolvedDeps,
                    PackageManager.Uv,
                    null
            );

            return text.withMarkers(text.getMarkers().addIfAbsent(marker));
        });
    }

    public static List<ResolvedDependency> parseFreezeOutput(Path workspace) {
        String freezeContent = DependencyWorkspace.readFreezeOutput(workspace);
        List<ResolvedDependency> resolved = parseFreezeLines(freezeContent);
        return linkDependenciesFromMetadata(resolved, workspace);
    }

    static List<ResolvedDependency> parseFreezeLines(String freezeContent) {
        List<ResolvedDependency> resolved = new ArrayList<>();
        for (String line : freezeContent.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eqIdx = trimmed.indexOf("==");
            if (eqIdx > 0) {
                String name = trimmed.substring(0, eqIdx).trim();
                String version = trimmed.substring(eqIdx + 2).trim();
                resolved.add(new ResolvedDependency(name, version, null, null));
            }
        }
        return resolved;
    }

    /**
     * Convert resolved dependencies into declared (direct) dependencies.
     * When the dependency graph has been linked (via {@link #linkDependenciesFromMetadata}),
     * only root packages (those not depended on by any other package) are included.
     * When the graph is unlinked (all {@code dependencies} fields are null), all packages
     * are treated as direct so that client code traversing {@code getDependencies()} finds every package.
     */
    public static List<Dependency> dependenciesFromResolved(List<ResolvedDependency> resolved) {
        return dependenciesFromResolved(resolved, Collections.emptySet());
    }

    public static List<Dependency> dependenciesFromResolved(List<ResolvedDependency> resolved,
                                                            Set<String> declaredPackageNames) {
        Set<String> transitive = new HashSet<>();
        for (ResolvedDependency r : resolved) {
            if (r.getDependencies() != null) {
                for (ResolvedDependency dep : r.getDependencies()) {
                    transitive.add(PythonResolutionResult.normalizeName(dep.getName()));
                }
            }
        }

        List<Dependency> deps = new ArrayList<>();
        for (ResolvedDependency r : resolved) {
            String normalizedName = PythonResolutionResult.normalizeName(r.getName());
            if (transitive.isEmpty() ||
                    !transitive.contains(normalizedName) ||
                    declaredPackageNames.contains(normalizedName)) {
                deps.add(new Dependency(r.getName(), "==" + r.getVersion(), null, null, r));
            }
        }
        return deps;
    }

    static Set<String> parseDeclaredPackageNames(String requirementsTxtContent) {
        Set<String> names = new HashSet<>();
        for (String line : requirementsTxtContent.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
                continue;
            }
            String name = PyProjectHelper.extractPackageName(trimmed);
            if (name != null) {
                names.add(PythonResolutionResult.normalizeName(name));
            }
        }
        return names;
    }

    /**
     * Link transitive dependencies from the workspace's {@code .venv} site-packages.
     *
     * @param resolved  flat list of resolved dependencies from freeze output
     * @param workspace workspace directory containing {@code .venv/}
     * @return new list with dependencies linked (or original list if site-packages not found)
     */
    static List<ResolvedDependency> linkDependenciesFromMetadata(List<ResolvedDependency> resolved, Path workspace) {
        Path sitePackages = InstalledEnvParser.findSitePackages(workspace.resolve(".venv"));
        if (sitePackages == null) {
            return resolved;
        }
        return InstalledEnvParser.linkDependenciesFromMetadata(resolved, sitePackages);
    }

    @Override
    public boolean accept(Path path) {
        String filename = path.getFileName().toString();
        return FILENAME_PATTERN.matcher(filename).matches();
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("requirements.txt");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(PlainText.class);
        }

        @Override
        public RequirementsTxtParser build() {
            return new RequirementsTxtParser();
        }

        @Override
        public String getDslName() {
            return "requirements.txt";
        }
    }
}
