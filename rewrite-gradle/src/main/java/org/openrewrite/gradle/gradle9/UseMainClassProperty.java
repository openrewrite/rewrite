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
package org.openrewrite.gradle.gradle9;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseMainClassProperty extends Recipe {

    private static final String IN_JAVA_EXEC = "IN_JAVA_EXEC";

    @Override
    public String getDisplayName() {
        return "Use `mainClass` instead of `main` for `JavaExec` tasks";
    }

    @Override
    public String getDescription() {
        return "The `main` property on `JavaExec` tasks was deprecated in Gradle 7.1 and removed in Gradle 9.0. " +
                "Use the `mainClass` property instead. " +
                "See the [Gradle upgrade guide](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html) for more information.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (hasJavaExecType(method)) {
                    getCursor().putMessage(IN_JAVA_EXEC, true);
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                Expression variable = assignment.getVariable();
                if (variable instanceof J.Identifier) {
                    J.Identifier id = (J.Identifier) variable;
                    if ("main".equals(id.getSimpleName()) && getCursor().getNearestMessage(IN_JAVA_EXEC) != null) {
                        return assignment.withVariable(id.withSimpleName("mainClass"));
                    }
                } else if (variable instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) variable;
                    if ("main".equals(fieldAccess.getSimpleName()) && getCursor().getNearestMessage(IN_JAVA_EXEC) != null) {
                        return assignment.withVariable(
                                fieldAccess.withName(fieldAccess.getName().withSimpleName("mainClass"))
                        );
                    }
                }
                return assignment;
            }

            private boolean hasJavaExecType(J.MethodInvocation method) {
                // Groovy DSL: task foo(type: JavaExec) { ... }
                for (Expression arg : method.getArguments()) {
                    if (arg instanceof G.MapEntry) {
                        G.MapEntry entry = (G.MapEntry) arg;
                        if (isTypeKey(entry.getKey()) && isJavaExecValue(entry.getValue())) {
                            return true;
                        }
                    } else if (isJavaExecValue(arg)) {
                        return true;
                    }
                }
                // Kotlin DSL: tasks.register<JavaExec>("foo") { ... }
                if (method.getTypeParameters() != null) {
                    for (Expression typeParameter : method.getTypeParameters()) {
                        if (isJavaExecValue(typeParameter)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean isTypeKey(Expression key) {
                return key instanceof G.Literal && "type".equals(((G.Literal) key).getValue());
            }

            private boolean isJavaExecValue(Expression value) {
                return value instanceof J.Identifier && "JavaExec".equals(((J.Identifier) value).getSimpleName());
            }
        });
    }
}
