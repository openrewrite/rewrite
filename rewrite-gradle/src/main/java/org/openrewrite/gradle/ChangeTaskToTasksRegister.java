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
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTaskToTasksRegister extends Recipe {

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
        return Preconditions.check(new IsBuildGradle<>(), new ChangeTaskToTasksRegisterVisitor());
    }

    private static class ChangeTaskToTasksRegisterVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!isTaskDeclaration(m, getCursor())) {
                return m;
            }

            Expression select = m.getSelect();
            Expression newSelect;
            J.Identifier tasks = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, "tasks", null, null);
            if (select == null) {
                newSelect = tasks.withPrefix(m.getPrefix());
                m = m.withPrefix(Space.EMPTY);
            } else {
                newSelect = new J.FieldAccess(
                  Tree.randomId(),
                  select.getPrefix(),
                  Markers.EMPTY,
                  select.withPrefix(Space.EMPTY),
                  JLeftPadded.build(tasks),
                  null
                );
            }

            J.Identifier register = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, "register", null, null);
            if (getCursor().firstEnclosing(G.CompilationUnit.class) != null) {
                List<Expression> args = m.getArguments();
                if (args.isEmpty() || !(args.get(0) instanceof J.MethodInvocation)) {
                    return m;
                }

                J.MethodInvocation inner = (J.MethodInvocation) args.get(0);
                String taskName = inner.getSimpleName();
                List<Expression> registerArgs = new ArrayList<>();
                registerArgs.add(new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, taskName, "\"" + taskName + "\"", null, JavaType.Primitive.String));
                List<Expression> innerArgs = inner.getArguments();
                if (!innerArgs.isEmpty()) {
                    if (innerArgs.get(0) instanceof G.MapEntry) {
                        G.MapEntry mapEntry = (G.MapEntry) innerArgs.get(0);
                        if ("type".equals(getMapEntryKey(mapEntry))) {
                            registerArgs.add(mapEntry.getValue().withPrefix(Space.SINGLE_SPACE));
                        }
                        if (innerArgs.size() > 1 && innerArgs.get(1) instanceof J.Lambda) {
                            registerArgs.add(((J.Lambda) innerArgs.get(1)).withPrefix(Space.SINGLE_SPACE));
                        }
                    } else if (innerArgs.get(0) instanceof J.Lambda) {
                        registerArgs.add(((J.Lambda) innerArgs.get(0)).withPrefix(Space.SINGLE_SPACE));
                    }
                }
                return m.withSelect(newSelect).withName(register).withArguments(registerArgs);
            } else {
                return m.withSelect(newSelect).withName(register);
            }
        }

        private boolean isTaskDeclaration(J.MethodInvocation method, Cursor cursor) {
            if (!"task".equals(method.getSimpleName())) {
                return false;
            }

            if (cursor.firstEnclosing(G.CompilationUnit.class) != null) {
                return isGroovyTaskDeclaration(method, cursor);
            } else if (cursor.firstEnclosing(K.CompilationUnit.class) != null) {
                return isKotlinTaskDeclaration(method);
            }
            return false;
        }

        private boolean isGroovyTaskDeclaration(J.MethodInvocation method, Cursor cursor) {
            Expression select = method.getSelect();
            if (select == null) {
                return true;
            }

            if (select instanceof J.Identifier) {
                String selectName = ((J.Identifier) select).getSimpleName();
                if ("project".equals(selectName) || "it".equals(selectName)) {
                    return true;
                }

                J.Lambda enclosingLambda = cursor.firstEnclosing(J.Lambda.class);
                if (enclosingLambda != null) {
                    for (J param : enclosingLambda.getParameters().getParameters()) {
                        if (param instanceof J.VariableDeclarations) {
                            J.VariableDeclarations varDecls = (J.VariableDeclarations) param;
                            if (!varDecls.getVariables().isEmpty() &&
                              varDecls.getVariables().get(0).getName().getSimpleName().equals(selectName)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        private boolean isKotlinTaskDeclaration(J.MethodInvocation method) {
            Expression select = method.getSelect();
            if (select == null) {
                return true;
            }
            return select instanceof J.Identifier && "project".equals(((J.Identifier) select).getSimpleName());
        }

        private String getMapEntryKey(G.MapEntry mapEntry) {
            Expression expression = mapEntry.getKey();
            if (expression instanceof J.Literal) {
                Object key = ((J.Literal) expression).getValue();
                if (key instanceof String) {
                    return (String) key;
                }
            }
            return null;
        }
    }
}
