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
import org.openrewrite.python.internal.DependencyManifest;
import org.openrewrite.python.internal.InstalledEnvParser;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

/**
 * Parser for requirements.txt files that delegates to {@link PlainTextParser} and attaches a
 * {@link PythonResolutionResult} marker with the dependencies the file declares, resolved against
 * the project's dependency environment.
 */
public class RequirementsTxtParser implements Parser {

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "requirements(-[\\w-]+)?\\.(txt|in)"
    );
    private static final Pattern INCLUDE = Pattern.compile("(?:-r|--requirement)(?:\\s*=\\s*|\\s+)?(\\S+)");

    private final PlainTextParser plainTextParser = new PlainTextParser();
    private final Map<String, String> subprocessEnvironment;

    @Nullable
    private final Path installedEnv;

    /**
     * Whether each file gets an environment installed from its own requirements.
     */
    private final boolean installsEnv;

    public RequirementsTxtParser() {
        this(emptyMap());
    }

    /**
     * @param subprocessEnvironment environment for the {@code uv} processes that install each file's requirements
     */
    public RequirementsTxtParser(Map<String, String> subprocessEnvironment) {
        this.subprocessEnvironment = subprocessEnvironment;
        this.installedEnv = null;
        this.installsEnv = true;
    }

    /**
     * @param installedEnv the project's dependency environment, which the markers resolve against, or null
     *                     to leave the files without markers
     */
    public RequirementsTxtParser(@Nullable Path installedEnv) {
        this.subprocessEnvironment = emptyMap();
        this.installedEnv = installedEnv;
        this.installsEnv = false;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return plainTextParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf.withMarkers(sf.getMarkers().add(ParseExceptionResult.build(this, new UnsupportedOperationException(), "Creating a PythonResolutionResult can only be done for PlainText LST elements.")));
            }
            PlainText text = (PlainText) sf;

            Path originalFilePath = relativeTo == null ? null : relativeTo.resolve(text.getSourcePath());
            Path env = installedEnv;
            if (installsEnv) {
                env = DependencyWorkspace.install(DependencyManifest.RequirementsFiles,
                        setupPy -> DependencyManifest.ofRequirementsFile(text.getText(), originalFilePath,
                                subprocessEnvironment, setupPy),
                        DependencyWorkspace.Options.builder().environment(subprocessEnvironment).build()).getEnv();
            } else if (env == null) {
                return sf;
            }
            if (env == null) {
                return sf.withMarkers(sf.getMarkers().add(ParseExceptionResult.build(this, new UnsupportedOperationException(),
                        "Failed to create the PythonResolutionResult due to a failure to install the requirement file. " +
                                "Perhaps you are missing `uv` in the environment or trying to build a requirement text file containing dependencies which are not available?")));
            }

            List<ResolvedDependency> resolvedDeps = InstalledEnvParser.parse(env);
            if (resolvedDeps.isEmpty()) {
                return sf;
            }

            List<Dependency> deps = declaredDependencies(resolvedDeps,
                    declaredPackageNames(text.getText(), originalFilePath, new HashSet<>()));

            PythonResolutionResult marker = new PythonResolutionResult(
                    randomId(),
                    null,
                    null,
                    null,
                    null,
                    text.getSourcePath().toString(),
                    null,
                    null,
                    emptyList(),
                    deps,
                    emptyMap(),
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    resolvedDeps,
                    PackageManager.Uv,
                    null
            );

            return text.withMarkers(text.getMarkers().addIfAbsent(marker));
        });
    }

    /**
     * Convert resolved dependencies into declared (direct) dependencies.
     * When the dependency graph is linked, only root packages (those not depended on by any
     * other package) are included.
     * When the graph is unlinked (all {@code dependencies} fields are null), all packages
     * are treated as direct so that client code traversing {@code getDependencies()} finds every package.
     */
    public static List<Dependency> dependenciesFromResolved(List<ResolvedDependency> resolved) {
        return dependenciesFromResolved(resolved, emptySet());
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

    /**
     * The declared packages the env holds, in the env's order. A file names only its own dependencies,
     * although the env, which several requirements files may share, holds those of all of them.
     */
    public static List<Dependency> declaredDependencies(List<ResolvedDependency> resolved,
                                                        Set<String> declaredPackageNames) {
        List<Dependency> deps = new ArrayList<>();
        for (ResolvedDependency r : resolved) {
            if (declaredPackageNames.contains(PythonResolutionResult.normalizeName(r.getName()))) {
                deps.add(new Dependency(r.getName(), "==" + r.getVersion(), null, null, r));
            }
        }
        return deps;
    }

    /**
     * The names a file declares, counting those of the files it includes with {@code -r}.
     */
    private static Set<String> declaredPackageNames(String content, @Nullable Path file, Set<Path> visited) {
        Set<String> names = parseDeclaredPackageNames(content);
        Path dir = file == null ? null : file.getParent();
        for (String line : content.split("\n")) {
            Matcher include = INCLUDE.matcher(line.trim());
            if (dir != null && include.matches()) {
                Path included = dir.resolve(include.group(1)).normalize();
                if (Files.isRegularFile(included) && visited.add(included)) {
                    try {
                        String includedContent = new String(Files.readAllBytes(included), StandardCharsets.UTF_8);
                        names.addAll(declaredPackageNames(includedContent, included, visited));
                    } catch (IOException ignored) {
                        // An unreadable include declares nothing
                    }
                }
            }
        }
        return names;
    }

    public static Set<String> parseDeclaredPackageNames(String requirementsTxtContent) {
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

    @Override
    public boolean accept(Path path) {
        return isRequirementsFile(path);
    }

    public static boolean isRequirementsFile(Path path) {
        return FILENAME_PATTERN.matcher(path.getFileName().toString()).matches();
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
