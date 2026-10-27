/*
 * Copyright 2026 the original author or authors.
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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.List;

import static java.util.Collections.emptyList;

public class UseDefaultPathSensitivity extends Recipe {

    private static final String PATH_SENSITIVE = "org.gradle.api.tasks.PathSensitive";
    private static final String PATH_SENSITIVITY = "org.gradle.api.tasks.PathSensitivity";

    private static final AnnotationMatcher INPUT_FILE = new AnnotationMatcher("@org.gradle.api.tasks.InputFile");
    private static final AnnotationMatcher INPUT_DIRECTORY = new AnnotationMatcher("@org.gradle.api.tasks.InputDirectory");
    private static final AnnotationMatcher PATH_SENSITIVE_MATCHER = new AnnotationMatcher("@" + PATH_SENSITIVE);

    @Getter
    final String displayName = "Declare path sensitivity on task file inputs";

    @Getter
    final String description = "Annotate an `@InputFile` with `@PathSensitive(PathSensitivity.NONE)` and an " +
            "`@InputDirectory` with `@PathSensitive(PathSensitivity.RELATIVE)`, and replace " +
            "`PathSensitivity.ABSOLUTE` with those values. Absolute paths are the default when no sensitivity is " +
            "declared, which puts the checkout location into the task's cache key: the same inputs then miss the " +
            "build cache on another machine, or in another directory on the same one. `@InputFiles` is left alone " +
            "because the right sensitivity for a collection depends on whether the task cares about the names " +
            "within it, which the annotation does not say. See the " +
            "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_tasks.html#default_path_sensitivities).";

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(5);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>("org.gradle.api.tasks.InputFile", false),
                        new UsesType<>("org.gradle.api.tasks.InputDirectory", false)),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);
                        if (!PATH_SENSITIVE_MATCHER.matches(a) || !"ABSOLUTE".equals(declaredSensitivity(a))) {
                            return a;
                        }
                        String sensitivity = wantedSensitivity(enclosingAnnotations());
                        if (sensitivity == null) {
                            return a;
                        }
                        // Written again rather than renamed in place: renaming the constant leaves the identifier's
                        // type attribution pointing at the one it replaced, which is an invalid tree.
                        return template().apply(getCursor(), a.getCoordinates().replace(), sensitivity);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                        String sensitivity = sensitivityToAdd(m.getLeadingAnnotations());
                        if (sensitivity == null) {
                            return m;
                        }
                        maybeAddImport(PATH_SENSITIVE);
                        maybeAddImport(PATH_SENSITIVITY);
                        return template().apply(getCursor(),
                                m.getCoordinates().addAnnotation((a, b) -> 0), sensitivity);
                    }

                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                        String sensitivity = sensitivityToAdd(v.getLeadingAnnotations());
                        if (sensitivity == null) {
                            return v;
                        }
                        maybeAddImport(PATH_SENSITIVE);
                        maybeAddImport(PATH_SENSITIVITY);
                        return template().apply(getCursor(),
                                v.getCoordinates().addAnnotation((a, b) -> 0), sensitivity);
                    }

                    private JavaTemplate template() {
                        return JavaTemplate.builder("@PathSensitive(PathSensitivity.#{})")
                                .imports(PATH_SENSITIVE, PATH_SENSITIVITY)
                                .javaParser(JavaParser.fromJavaVersion().classpath("gradle-api"))
                                .build();
                    }

                    private @Nullable String sensitivityToAdd(List<J.Annotation> annotations) {
                        return find(annotations, PATH_SENSITIVE_MATCHER) == null ?
                                wantedSensitivity(annotations) :
                                null;
                    }

                    private List<J.Annotation> enclosingAnnotations() {
                        J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                        if (method != null) {
                            return method.getLeadingAnnotations();
                        }
                        J.VariableDeclarations variable = getCursor().firstEnclosing(J.VariableDeclarations.class);
                        return variable == null ? emptyList() : variable.getLeadingAnnotations();
                    }
                });
    }


    /**
     * `NONE` for a single file, whose path should not reach the cache key at all, and `RELATIVE` for a directory,
     * where the layout inside it is part of the input but where it sits is not.
     */
    private static @Nullable String wantedSensitivity(List<J.Annotation> annotations) {
        if (find(annotations, INPUT_FILE) != null) {
            return "NONE";
        }
        if (find(annotations, INPUT_DIRECTORY) != null) {
            return "RELATIVE";
        }
        return null;
    }

    private static @Nullable String declaredSensitivity(J.Annotation pathSensitive) {
        if (pathSensitive.getArguments() == null || pathSensitive.getArguments().isEmpty()) {
            return null;
        }
        Object argument = pathSensitive.getArguments().get(0);
        if (argument instanceof J.FieldAccess) {
            return ((J.FieldAccess) argument).getSimpleName();
        }
        if (argument instanceof J.Identifier) {
            return ((J.Identifier) argument).getSimpleName();
        }
        return null;
    }

    private static J.@Nullable Annotation find(List<J.Annotation> annotations, AnnotationMatcher matcher) {
        for (J.Annotation annotation : annotations) {
            if (matcher.matches(annotation)) {
                return annotation;
            }
        }
        return null;
    }
}
