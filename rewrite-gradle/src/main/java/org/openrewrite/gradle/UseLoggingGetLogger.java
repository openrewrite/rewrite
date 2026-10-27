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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public class UseLoggingGetLogger extends Recipe {

    private static final String PLUGIN_TYPE = "org.gradle.api.Plugin";
    private static final String LOGGER_TYPE = "org.gradle.api.logging.Logger";
    private static final String LOGGING_TYPE = "org.gradle.api.logging.Logging";
    private static final String FIELD_NAME = "LOGGER";

    private static final MethodMatcher PROJECT_GET_LOGGER =
            new MethodMatcher("org.gradle.api.Project getLogger()");
    private static final MethodMatcher LOGGING_GET_LOGGER =
            new MethodMatcher("org.gradle.api.logging.Logging getLogger(..)");

    @Getter
    final String displayName = "Obtain a plugin's logger from `Logging.getLogger`";

    @Getter
    final String description = "Replace `project.getLogger()` in a `Plugin` with a `private static final Logger` " +
            "obtained from `Logging.getLogger(TheClass.class)`. A logger taken from the project is attributed to the " +
            "build rather than to the plugin, so its output carries nothing to say where it came from, and reaching " +
            "through the project for it is one more use of the project to remove before build logic can be " +
            "configuration-cache safe. A logger the class already declares that way is used as it is, rather than a " +
            "second one being added. A task's own `getLogger()` is left alone: Gradle attributes that one to the " +
            "task already. See the " +
            "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_general.html#obtain_loggers_via_logging_get_logger).";

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(5);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // A JavaVisitor rather than the Iso form: replacing `project.getLogger()` with a field reference
        // returns a different kind of node than it was given.
        return Preconditions.check(new UsesType<>(PLUGIN_TYPE, false), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
                if (!implementsPlugin(c) || loggerField(c) != null || !usesProjectLogger(c) ||
                    declaresFieldNamed(c, FIELD_NAME)) {
                    // A field of that name that is not a logger: adding another would not compile.
                    return c;
                }

                maybeAddImport(LOGGER_TYPE);
                maybeAddImport(LOGGING_TYPE);
                // Declared on this pass; the calls are pointed at it on the next, once the field exists to take its
                // type from.
                return JavaTemplate.builder("private static final Logger " + FIELD_NAME +
                                            " = Logging.getLogger(#{}.class);")
                        .imports(LOGGER_TYPE, LOGGING_TYPE)
                        // Parsed against the surrounding file so the `TheClass.class` reference resolves to it
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion().classpath("gradle-api"))
                        .build()
                        .apply(getCursor(), c.getBody().getCoordinates().firstStatement(), c.getSimpleName());
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!PROJECT_GET_LOGGER.matches(m)) {
                    return m;
                }
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null || !implementsPlugin(enclosing)) {
                    return m;
                }
                J.VariableDeclarations.NamedVariable logger = loggerField(enclosing);
                if (logger == null) {
                    return m;
                }
                // Built rather than templated so the identifier carries the field's own type attribution
                JavaType.Variable fieldType = logger.getVariableType();
                return new J.Identifier(randomId(), m.getPrefix(), m.getMarkers(),
                        emptyList(), logger.getSimpleName(),
                        fieldType == null ? null : fieldType.getType(), fieldType);
            }
        });
    }

    private static boolean implementsPlugin(J.ClassDeclaration classDecl) {
        if (classDecl.getImplements() == null) {
            return false;
        }
        for (TypeTree implemented : classDecl.getImplements()) {
            if (TypeUtils.isAssignableTo(PLUGIN_TYPE, implemented.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The field this class already initializes from `Logging.getLogger`, whatever it is named, or null when there is
     * none to reuse.
     */
    private static J.VariableDeclarations.@Nullable NamedVariable loggerField(J.ClassDeclaration classDecl) {
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (!(statement instanceof J.VariableDeclarations)) {
                continue;
            }
            for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) statement).getVariables()) {
                if (variable.getInitializer() instanceof J.MethodInvocation &&
                    LOGGING_GET_LOGGER.matches((J.MethodInvocation) variable.getInitializer())) {
                    return variable;
                }
            }
        }
        return null;
    }

    private static boolean declaresFieldNamed(J.ClassDeclaration classDecl, String name) {
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations) {
                for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) statement).getVariables()) {
                    if (name.equals(variable.getSimpleName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean usesProjectLogger(J.ClassDeclaration classDecl) {
        return new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if (PROJECT_GET_LOGGER.matches(method)) {
                    found.set(true);
                }
                return super.visitMethodInvocation(method, found);
            }
        }.reduce(classDecl, new AtomicBoolean()).get();
    }
}
