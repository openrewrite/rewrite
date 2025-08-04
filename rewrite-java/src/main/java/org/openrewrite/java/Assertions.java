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
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.test.UncheckedConsumer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.test.SourceSpecs.dir;

@SuppressWarnings("unused")
public class Assertions {
    private static final Map<Integer, JavaVersion> javaVersions = new ConcurrentHashMap<>();
    private static final Map<String, JavaProject> javaProjects = new ConcurrentHashMap<>();
    private static final Map<String, JavaSourceSet> javaSourceSets = new ConcurrentHashMap<>();

    private Assertions() {
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        if (ctx.getMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION) == null) {
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
        }
    }

    public static SourceFile validateTypes(SourceFile source, TypeValidation typeValidation) {
        if (source instanceof JavaSourceFile) {
            assertValidTypes(typeValidation, (JavaSourceFile) source);
            if (typeValidation.erroneous()) {
                List<J.Erroneous> allErroneous = new JavaIsoVisitor<List<J.Erroneous>>() {
                    @Override
                    public J.Erroneous visitErroneous(J.Erroneous erroneous, List<J.Erroneous> list) {
                        J.Erroneous err = super.visitErroneous(erroneous, list);
                        list.add(err);
                        return err;
                    }
                }.reduce(source, new ArrayList<>());
                if (!allErroneous.isEmpty()) {
                    throw new IllegalStateException("LST contains erroneous nodes\n" + allErroneous.stream()
                            .map(J.Erroneous::getText)
                            .collect(joining("\n\n")));
                }
            }
            if (typeValidation.unknown()) {
                List<J.Unknown> allUnknown = new JavaIsoVisitor<List<J.Unknown>>() {
                    @Override
                    public J.Unknown visitUnknown(J.Unknown unknown, List<J.Unknown> list) {
                        J.Unknown err = super.visitUnknown(unknown, list);
                        list.add(err);
                        return err;
                    }
                }.reduce(source, new ArrayList<>());
                if (!allUnknown.isEmpty()) {
                    throw new IllegalStateException("LST contains erroneous nodes\n" + allUnknown.stream()
                            .map(unknown -> unknown.getSource().getText())
                            .collect(joining("\n\n")));
                }
            }
        }
        return source;
    }

    private static void assertValidTypes(TypeValidation typeValidation, J sf) {
        if (typeValidation.identifiers() || typeValidation.methodInvocations() || typeValidation.methodDeclarations() || typeValidation.classDeclarations() ||
            typeValidation.constructorInvocations()) {
            List<FindMissingTypes.MissingTypeResult> missingTypeResults = FindMissingTypes.findMissingTypes(sf, true);
            missingTypeResults = missingTypeResults.stream()
                    .filter(missingType -> {
                        if (missingType.getJ() instanceof J.Identifier) {
                            return typeValidation.identifiers();
                        } else if (missingType.getJ() instanceof J.ClassDeclaration) {
                            return typeValidation.classDeclarations();
                        } else if (missingType.getJ() instanceof J.MethodInvocation || missingType.getJ() instanceof J.MemberReference) {
                            return typeValidation.methodInvocations();
                        } else if (missingType.getJ() instanceof J.NewClass) {
                            return typeValidation.constructorInvocations();
                        } else if (missingType.getJ() instanceof J.MethodDeclaration) {
                            return typeValidation.methodDeclarations();
                        } else if (missingType.getJ() instanceof J.VariableDeclarations.NamedVariable) {
                            return typeValidation.variableDeclarations();
                        } else {
                            return true;
                        }
                    })
                    .filter(missingType -> !typeValidation.allowMissingType().apply(missingType))
                    .collect(toList());
            if (!missingTypeResults.isEmpty()) {
                String missingTypes = missingTypeResults.stream()
                        .map(v -> v.getPath() + "\n" + v.getPrintedTree())
                        .collect(joining("\n\n"));
                throw new IllegalStateException(
                        "LST contains missing or invalid type information\n" + missingTypes +
                        "\nhttps://docs.openrewrite.org/reference/faq#im-seeing-lst-contains-missing-or-invalid-type-information-in-my-recipe-unit-tests-how-to-resolve");
            }
        }
    }

    public static SourceSpecs java(@Language("java") @Nullable String before) {
        return java(before, s -> {
        });
    }

    private static final Parser.Builder javaParser = JavaParser.fromJavaVersion()
            .classpath(JavaParser.runtimeClasspath())
            .logCompilationWarningsAndErrors(true);

    public static SourceSpecs java(@Language("java") @Nullable String before, Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(
                J.CompilationUnit.class, null, javaParser, before,
                Assertions::validateTypes,
                Assertions::customizeExecutionContext
        );
        acceptSpec(spec, java);
        return java;
    }

    public static SourceSpecs java(@Language("java") @Nullable String before, @Language("java") @Nullable String after) {
        return java(before, after, s -> {
        });
    }

    public static SourceSpecs java(@Language("java") @Nullable String before, @Language("java") @Nullable String after,
                                   Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, javaParser, before,
                Assertions::validateTypes,
                Assertions::customizeExecutionContext).after(s -> after);
        acceptSpec(spec, java);
        return java;
    }

    private static void acceptSpec(Consumer<SourceSpec<J.CompilationUnit>> spec, SourceSpec<J.CompilationUnit> java) {
        Consumer<J.CompilationUnit> userSuppliedAfterRecipe = java.getAfterRecipe();
        java.afterRecipe(userSuppliedAfterRecipe::accept);
        spec.accept(java);
    }

    public static SourceSpecs mavenProject(String project, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return dir(project, spec, sources);
    }

    public static SourceSpecs mavenProject(String project, SourceSpecs... sources) {
        return mavenProject(project, spec -> project(spec, project), sources);
    }

    public static SourceSpecs srcMainJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/main/java", spec, javaSources);
    }

    public static SourceSpecs srcMainJava(SourceSpecs... javaSources) {
        return srcMainJava(spec -> sourceSet(spec, "main"), javaSources);
    }

    public static SourceSpecs srcMainResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/main/resources", spec, resources);
    }

    public static SourceSpecs srcMainResources(SourceSpecs... resources) {
        return srcMainResources(spec -> sourceSet(spec, "main"), resources);
    }

    public static SourceSpecs srcTestJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/test/java", spec, javaSources);
    }

    public static SourceSpecs srcTestJava(SourceSpecs... javaSources) {
        return srcTestJava(spec -> sourceSet(spec, "test"), javaSources);
    }

    public static SourceSpecs srcTestResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/test/resources", spec, resources);
    }

    public static SourceSpecs srcTestResources(SourceSpecs... resources) {
        return srcTestResources(spec -> sourceSet(spec, "test"), resources);
    }

    public static SourceSpecs srcSmokeTestJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/smokeTest/java", spec, javaSources);
    }

    public static SourceSpecs srcSmokeTestJava(SourceSpecs... javaSources) {
        return srcSmokeTestJava(spec -> sourceSet(spec, "smokeTest"), javaSources);
    }

    public static SourceSpec<?> version(SourceSpec<?> sourceSpec, int version) {
        return sourceSpec.markers(javaVersion(version));
    }

    public static SourceSpecs version(SourceSpecs sourceSpec, int version) {
        for (SourceSpec<?> spec : sourceSpec) {
            spec.markers(javaVersion(version));
        }
        return sourceSpec;
    }

    public static SourceSpec<?> project(SourceSpec<?> sourceSpec, String projectName) {
        return sourceSpec.markers(javaProject(projectName));
    }

    public static SourceSpec<?> sourceSet(SourceSpec<?> sourceSpec, String sourceSet) {
        sourceSpec.markers(javaSourceSet(sourceSet));
        return sourceSpec;
    }

    public static UncheckedConsumer<List<SourceFile>> addTypesToSourceSet(String sourceSetName, List<String> extendsFrom, List<Path> classpath) {
        return sourceFiles -> {
            JavaSourceSet sourceSet = JavaSourceSet.build(sourceSetName, classpath, new JavaTypeCache(), false);

            for (int i = 0; i < sourceFiles.size(); i++) {
                SourceFile sourceFile = sourceFiles.get(i);
                Optional<JavaSourceSet> maybeCurrentSourceSet = sourceFile.getMarkers().findFirst(JavaSourceSet.class);
                if (!maybeCurrentSourceSet.isPresent() || !maybeCurrentSourceSet.get().getName().equals(sourceSetName)) {
                    continue;
                }

                sourceFiles.set(i, sourceFile.withMarkers(sourceFile.getMarkers().computeByType(sourceSet, (original, updated) -> updated)));
            }
        };
    }

    public static UncheckedConsumer<List<SourceFile>> addTypesToSourceSet(String sourceSetName, List<String> extendsFrom) {
        return addTypesToSourceSet(sourceSetName, extendsFrom, emptyList());
    }

    public static UncheckedConsumer<List<SourceFile>> addTypesToSourceSet(String sourceSetName) {
        return addTypesToSourceSet(sourceSetName, emptyList(), emptyList());
    }

    public static JavaVersion javaVersion(int version) {
        return javaVersions.computeIfAbsent(version, v ->
                new JavaVersion(Tree.randomId(), "openjdk", "adoptopenjdk",
                        Integer.toString(v), Integer.toString(v)));
    }

    private static JavaProject javaProject(String projectName) {
        return javaProjects.computeIfAbsent(projectName, name ->
                new JavaProject(Tree.randomId(), name, null));
    }

    private static JavaSourceSet javaSourceSet(String sourceSet) {
        return javaSourceSets.computeIfAbsent(sourceSet, name ->
                new JavaSourceSet(Tree.randomId(), name, emptyList(), emptyMap()));
    }
}
