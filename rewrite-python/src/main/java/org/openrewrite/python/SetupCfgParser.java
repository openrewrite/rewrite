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
import org.openrewrite.python.internal.DependencyManifest;
import org.openrewrite.python.internal.InstalledEnvParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

/**
 * Parser for setup.cfg files that delegates to {@link PlainTextParser} and attaches a
 * {@link PythonResolutionResult} marker with the project's dependencies, resolved against its
 * dependency environment.
 */
public class SetupCfgParser implements Parser {

    private final PlainTextParser plainTextParser = new PlainTextParser();
    private final Map<String, String> subprocessEnvironment;

    @Nullable
    private final Path installedEnv;

    /**
     * Whether the project gets an environment installed from its setup.cfg or setup.py.
     */
    private final boolean installsEnv;

    public SetupCfgParser() {
        this(emptyMap());
    }

    /**
     * @param subprocessEnvironment environment for the {@code uv} processes that install the project's requirements
     */
    public SetupCfgParser(Map<String, String> subprocessEnvironment) {
        this.subprocessEnvironment = subprocessEnvironment;
        this.installedEnv = null;
        this.installsEnv = true;
    }

    /**
     * @param installedEnv the env installed from the project's setup.py or setup.cfg, which the marker
     *                     resolves against, or null to leave the file without a marker
     */
    public SetupCfgParser(@Nullable Path installedEnv) {
        this.subprocessEnvironment = emptyMap();
        this.installedEnv = installedEnv;
        this.installsEnv = false;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return plainTextParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf;
            }
            PlainText text = (PlainText) sf;

            @Nullable Path projectDir = null;
            if (relativeTo != null) {
                Path filePath = relativeTo.resolve(text.getSourcePath());
                projectDir = filePath.getParent();
            }
            Path env = installedEnv;
            if (installsEnv && projectDir != null) {
                Path dir = projectDir;
                DependencyManifest manifest = Files.isRegularFile(dir.resolve("setup.py")) ?
                        DependencyManifest.SetupPy : DependencyManifest.SetupCfg;
                env = DependencyWorkspace.install(manifest,
                        setupPy -> manifest.read(dir, relativeTo, subprocessEnvironment, setupPy),
                        DependencyWorkspace.Options.builder().environment(subprocessEnvironment).build()).getEnv();
            }
            if (env == null) {
                return sf;
            }

            List<ResolvedDependency> resolvedDeps = InstalledEnvParser.parse(env);
            if (resolvedDeps.isEmpty()) {
                return sf;
            }

            List<Dependency> deps = declaredDependencies(resolvedDeps, text.getText(), env);

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
     * The dependencies setup.cfg's {@code install_requires} declares. A setup.py may declare them in code
     * instead, and then they are the requirements the env was installed from. For an env made elsewhere,
     * they are its packages that no other package depends on.
     */
    private static List<Dependency> declaredDependencies(List<ResolvedDependency> resolved, String setupCfg, Path env) {
        List<String> declared = DependencyManifest.ofSetupCfg(setupCfg, null).getRoots();
        if (declared.isEmpty()) {
            List<String> installed = DependencyWorkspace.installedRequirements(env);
            if (installed == null) {
                return RequirementsTxtParser.dependenciesFromResolved(resolved);
            }
            declared = installed;
        }
        Set<String> names = RequirementsTxtParser.parseDeclaredPackageNames(String.join("\n", declared));
        return RequirementsTxtParser.declaredDependencies(resolved, names);
    }

    @Override
    public boolean accept(Path path) {
        return path.endsWith("setup.cfg");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("setup.cfg");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(PlainText.class);
        }

        @Override
        public SetupCfgParser build() {
            return new SetupCfgParser();
        }

        @Override
        public String getDslName() {
            return "setup.cfg";
        }
    }
}
