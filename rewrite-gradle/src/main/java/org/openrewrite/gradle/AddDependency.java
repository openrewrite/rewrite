/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.semver.Semver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddDependency extends ScanningRecipe<AddDependency.Scanned> {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example, " +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Configuration",
            description = "A configuration to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                          "is used when adding a new as of yet unused dependency.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Option(displayName = "Only if using",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.junit.jupiter.api.*")
    String onlyIfUsing;

    @Option(displayName = "Classifier",
            description = "A classifier to add. Commonly used to select variants of a library.",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Extension",
            description = "The extension of the dependency to add. If omitted Gradle defaults to assuming the type is \"jar\".",
            example = "jar",
            required = false)
    @Nullable
    String extension;

    @Option(displayName = "Family pattern",
            description = "A pattern, applied to groupIds, used to determine which other dependencies should have aligned version numbers. " +
                          "Accepts '*' as a wildcard character.",
            example = "com.fasterxml.jackson*",
            required = false)
    @Nullable
    String familyPattern;

    static final String DEPENDENCY_PRESENT = "org.openrewrite.gradle.AddDependency.DEPENDENCY_PRESENT";

    @Override
    public String getDisplayName() {
        return "Add Gradle dependency";
    }

    @Override
    public String getDescription() {
        return "Add a gradle dependency to a `build.gradle` file in the correct configuration based on where it is used.";
    }

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (version != null) {
            validated = validated.or(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    static class Scanned {
        boolean usingType;
        Map<JavaProject, String> configurationByProject = new HashMap<>();
    }

    @Override
    public Scanned getInitialValue() {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject ->
                        sourceFile.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet -> {
                            if (sourceFile != new UsesType<>(onlyIfUsing, true).visit(sourceFile, ctx)) {
                                acc.usingType = true;
                                acc.configurationByProject.compute(javaProject, (jp, configuration) -> "implementation".equals(configuration) ?
                                        configuration :
                                        "test".equals(sourceSet.getName()) ? "testImplementation" : "implementation"
                                );
                            }
                        }));
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.usingType && !acc.configurationByProject.isEmpty(),
                Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
                    final MethodMatcher dependencyDslMatcher = new MethodMatcher("DependencyHandlerSpec *(..)");
                    final Pattern familyPatternCompiled = StringUtils.isBlank(familyPattern) ? null : Pattern.compile(familyPattern.replace("*", ".*"));

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (dependencyDslMatcher.matches(m) && (StringUtils.isBlank(configuration) || configuration.equals(m.getSimpleName()))) {
                            if (m.getArguments().get(0) instanceof J.Literal) {
                                //noinspection ConstantConditions
                                Dependency dependency = DependencyStringNotationConverter.parse((String) ((J.Literal) m.getArguments().get(0)).getValue());
                                if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, DEPENDENCY_PRESENT, true);
                                }
                            } else if (m.getArguments().get(0) instanceof G.GString) {
                                List<J> strings = ((G.GString) m.getArguments().get(0)).getStrings();
                                if (strings.size() >= 2 &&
                                    strings.get(0) instanceof J.Literal) {
                                    String dep = (String) ((J.Literal) strings.get(0)).getValue();
                                    if (dep != null) {
                                        Dependency dependency = DependencyStringNotationConverter.parse(dep);
                                        if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                                            getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, DEPENDENCY_PRESENT, true);
                                        }
                                    }
                                }
                            } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                                G.MapEntry groupEntry = null;
                                G.MapEntry artifactEntry = null;

                                for (Expression e : m.getArguments()) {
                                    if (!(e instanceof G.MapEntry)) {
                                        continue;
                                    }
                                    G.MapEntry arg = (G.MapEntry) e;
                                    if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                                        continue;
                                    }
                                    J.Literal key = (J.Literal) arg.getKey();
                                    J.Literal value = (J.Literal) arg.getValue();
                                    if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                                        continue;
                                    }
                                    if ("group".equals(key.getValue())) {
                                        groupEntry = arg;
                                    } else if ("name".equals(key.getValue())) {
                                        artifactEntry = arg;
                                    }
                                }

                                if (groupEntry == null || artifactEntry == null) {
                                    return m;
                                }

                                if (groupId.equals(((J.Literal) groupEntry.getValue()).getValue())
                                    && artifactId.equals(((J.Literal) artifactEntry.getValue()).getValue())) {
                                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, DEPENDENCY_PRESENT, true);
                                }
                            }
                        }

                        return m;
                    }

                    @Override
                    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                        if (!cu.getSourcePath().toString().endsWith(".gradle") || cu.getSourcePath().getFileName().toString().equals("settings.gradle")) {
                            return cu;
                        }

                        JavaProject javaProject = getCursor().firstEnclosingOrThrow(JavaSourceFile.class)
                                .getMarkers().findFirst(JavaProject.class).orElse(null);
                        String maybeConfiguration = javaProject == null ? null :
                                acc.configurationByProject.get(javaProject);
                        if (maybeConfiguration == null) {
                            return cu;
                        }

                        G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);

                        if (getCursor().getMessage(DEPENDENCY_PRESENT, false)) {
                            return g;
                        }

                        String resolvedConfiguration = StringUtils.isBlank(configuration) ? maybeConfiguration : configuration;

                        return (G.CompilationUnit) new AddDependencyVisitor(groupId, artifactId, version, StringUtils.isBlank(versionPattern) ? null : versionPattern, resolvedConfiguration,
                                StringUtils.isBlank(classifier) ? null : classifier, StringUtils.isBlank(extension) ? null : extension, familyPatternCompiled).visitNonNull(g, ctx);
                    }
                })
        );
    }
}
