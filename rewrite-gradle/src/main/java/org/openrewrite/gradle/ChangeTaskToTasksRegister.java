/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.SearchResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTaskToTasksRegister extends Recipe {

    private static final GradleParser GRADLE_PARSER = GradleParser.builder().build();

    @Override
    public String getDisplayName() {
        return "Change Gradle task eager creation to lazy registration";
    }

    @Override
    public String getDescription() {
        return "Changes eager task creation `task exampleName(type: ExampleType)` to lazy registration `tasks.register(\"exampleName\", ExampleType)`. " +
                "Also supports Kotlin DSL: `task<ExampleType>(\"exampleName\")` to `tasks.register<ExampleType>(\"exampleName\")`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new IsBuildGradle<>(), new IsBuildGradleKts()),
                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (tree == null) {
                            return null;
                        }
                        if (tree instanceof G.CompilationUnit) {
                            return new GroovyVisitor().visit(tree, ctx);
                        }
                        if (tree instanceof K.CompilationUnit) {
                            return new KotlinVisitor().visit(tree, ctx);
                        }
                        return tree;
                    }
                }
        );
    }

    private static class IsBuildGradle<P> extends TreeVisitor<Tree, P> {
        @Override
        public Tree visit(@Nullable Tree tree, P p) {
            if (tree instanceof SourceFile) {
                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getSourcePath().toString().endsWith(".gradle")) {
                    return SearchResult.found(tree);
                }
            }
            return tree;
        }
    }

    private static class IsBuildGradleKts extends TreeVisitor<Tree, ExecutionContext> {
        @Override
        public Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
            if (tree instanceof SourceFile) {
                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) {
                    return SearchResult.found(tree);
                }
            }
            return tree;
        }
    }

    private static class GroovyVisitor extends GroovyIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!"task".equals(m.getSimpleName()) || m.getSelect() != null) {
                return m;
            }

            List<Expression> args = m.getArguments();
            if (args.isEmpty() || !(args.get(0) instanceof J.MethodInvocation)) {
                return m;
            }

            J.MethodInvocation inner = (J.MethodInvocation) args.get(0);
            List<Expression> innerArgs = inner.getArguments();
            if(innerArgs.isEmpty()) {
                return m;
            }

            Expression taskType = null;
            J.Lambda taskLambda = null;
            if (innerArgs.get(0) instanceof G.MapEntry) {
                G.MapEntry mapEntry = (G.MapEntry) innerArgs.get(0);
                Expression key = mapEntry.getKey();
                String keyName = null;
                if (key instanceof G.Literal) {
                    Object value = ((G.Literal) key).getValue();
                    if (value instanceof String) {
                        keyName = (String) value;
                    }
                }

                if ("type".equals(keyName)) {
                    taskType = mapEntry.getValue();
                }

                if (innerArgs.size() > 1 && innerArgs.get(1) instanceof J.Lambda) {
                    taskLambda = (J.Lambda) innerArgs.get(1);
                }
            } else if (innerArgs.get(0) instanceof J.Lambda) {
                taskLambda = (J.Lambda) innerArgs.get(0);
            }

            String template;
            String name = inner.getSimpleName();
            String lambda = taskLambda != null ? taskLambda.print(getCursor().getParentOrThrow()) : "";
            if (taskType != null) {
                String type = taskType.withPrefix(Space.EMPTY).print(getCursor().getParentOrThrow());
                template = "tasks.register(\"" + name + "\", " + type + ")" + lambda;
            } else {
                template = "tasks.register(\"" + name + "\")" + lambda;
            }

            SourceFile parsed = GRADLE_PARSER.parse(ctx, template)
                    .findFirst()
                    .orElse(null);
            if (!(parsed instanceof G.CompilationUnit)) {
                return m;
            }

            G.CompilationUnit cu = (G.CompilationUnit) parsed;
            if (cu.getStatements().isEmpty() || !(cu.getStatements().get(0) instanceof J.MethodInvocation)) {
                return m;
            }
            return cu.getStatements().get(0).withPrefix(m.getPrefix()).withMarkers(m.getMarkers());
        }
    }

    private static class KotlinVisitor extends KotlinIsoVisitor<ExecutionContext> {

        @Override
        public K.MethodInvocation visitMethodInvocation(K.MethodInvocation method, ExecutionContext ctx) {
            K.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!m.getSimpleName().equals("task") || m.getSelect() != null) {
                return m;
            }

            List<Expression> args = m.getArguments();
            if (args.isEmpty() || !(args.get(0) instanceof J.Literal)) {
                return m;
            }

            J.Literal taskName = (J.Literal) args.get(0);
            if (!TypeUtils.isString(taskName.getType())) {
                return m;
            }

            Expression taskType = null;
            if(m.getTypeParameters() != null && !m.getTypeParameters().isEmpty()) {
                taskType = m.getTypeParameters().get(0);
            }

            J.Lambda taskLambda = null;
            if (args.size() == 2 && args.get(1) instanceof J.Lambda) {
                taskLambda = (J.Lambda) args.get(1);
            }

            String template;
            String name = (String) taskName.getValue();
            String lambda = taskLambda != null ? taskLambda.print(getCursor().getParentOrThrow()) : "";
            if (taskType != null) {
                String type = taskType.print(getCursor().getParentOrThrow());
                template = "tasks.register<" + type + ">(\"" + name + "\")" + lambda;
            } else {
                template = "tasks.register(\"" + name + "\")" + lambda;
            }

            GradleParser.Input input = new GradleParser.Input(
                    Paths.get("build.gradle.kts"),
                    () -> new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8))
            );

            SourceFile parsed = GRADLE_PARSER.parseInputs(Collections.singletonList(input), null, ctx)
                    .findFirst()
                    .orElse(null);
            if (!(parsed instanceof K.CompilationUnit)) {
                return m;
            }

            K.CompilationUnit cu = (K.CompilationUnit) parsed;
            if (cu.getStatements().isEmpty() || !(cu.getStatements().get(0) instanceof J.Block)) {
                return m;
            }

            J.Block block = (J.Block) cu.getStatements().get(0);
            return block.getStatements().get(0).withPrefix(m.getPrefix()).withMarkers(m.getMarkers());
        }
    }
}
