/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * TODO: Check the following criteria:
 * - Lombok in dependencies +
 * - All methods of class are static +
 * - No instances of given class +
 * - All static attributes are final +
 * <p>
 * TODO: Perform the transformation:
 * - Add the annotation +
 * - Remove static from all attributes and methods +
 * <p>
 * TODO: Features to consider:
 * - Transformation: Add Lombok config if not present + supported configuration options for utility class
 * - Transformation: Replace instantiation with static calls to methods --> na
 * - Anonymous classes ???
 * - Reflection ???
 */
public class LombokUtilityClass extends Recipe {

    @Override
    public String getDisplayName() {
        return "Lombok UtilityClass";
    }

    @Override
    public String getDescription() {
        return "This recipe will check if any class is transformable (only static methods in class)" +
                " into the Lombok UtilityClass and will perform the change if applicable.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeVisitor();
    }


    private static class ChangeVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final ExecutionContext executionContext
        ) {
            if (!CheckVisitor.shouldPerformChanges(classDecl)) {
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            maybeAddImport("lombok.experimental.UtilityClass", false);
            return super.visitClassDeclaration(
                    JavaTemplate
                            .builder("@UtilityClass")
                            .imports("lombok.experimental.UtilityClass")
                            .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                            .build()
                            .apply(
                                    getCursor(),
                                    classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName))
                            ),
                    executionContext
            );
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final ExecutionContext executionContext
        ) {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            if (!CheckVisitor.shouldPerformChanges(classDecl)) {
                return super.visitMethodDeclaration(method, executionContext);
            }

            return super.visitMethodDeclaration(
                    method.withModifiers(method.getModifiers().stream()
                            .filter(m -> m.getType() != J.Modifier.Type.Static)
                            .collect(Collectors.toList())),
                    executionContext
            );
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                final J.VariableDeclarations multiVariable,
                final ExecutionContext executionContext
        ) {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            if (!CheckVisitor.shouldPerformChanges(classDecl)) {
                return super.visitVariableDeclarations(multiVariable, executionContext);
            }

            return super.visitVariableDeclarations(
                    multiVariable
                            .withModifiers(multiVariable.getModifiers().stream()
                                    .filter(m -> m.getType() != J.Modifier.Type.Static)
                                    .collect(Collectors.toList())
                            )
                            .withVariables(multiVariable.getVariables().stream()
                                    .map(v -> v.withName(v.getName().withSimpleName(v.getName().getSimpleName().toLowerCase())))
                                    .collect(Collectors.toList())
                            ),
                    executionContext
            );
        }
    }

    private static class CheckVisitor extends JavaIsoVisitor<AtomicBoolean> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final AtomicBoolean shouldPerformChanges
        ) {
            if (classDecl.hasModifier(J.Modifier.Type.Abstract)) {
                shouldPerformChanges.set(false);
            }
            if (classDecl.getLeadingAnnotations().stream().anyMatch(a -> "UtilityClass".equals(a.getSimpleName()))) {
                shouldPerformChanges.set(false);
            }
            return super.visitClassDeclaration(classDecl, shouldPerformChanges);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final AtomicBoolean shouldPerformChanges
        ) {
            if (!method.hasModifier(J.Modifier.Type.Static)) {
                shouldPerformChanges.set(false);
            }

            if (method.getSimpleName().equalsIgnoreCase("main")) {
                shouldPerformChanges.set(false);
            }
            return super.visitMethodDeclaration(method, shouldPerformChanges);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(
                final J.VariableDeclarations.NamedVariable variable,
                final AtomicBoolean shouldPerformChanges
        ) {
            if (variable.isField(getCursor())
                    && variable.getVariableType() != null
                    && !variable.getVariableType().hasFlags(Flag.Static, Flag.Final)) {
                shouldPerformChanges.set(false);
            }
            return super.visitVariable(variable, shouldPerformChanges);
        }

        private static boolean shouldPerformChanges(final J.ClassDeclaration classDecl) {
            return new CheckVisitor().reduce(classDecl, new AtomicBoolean(true)).get();
        }
    }
}
