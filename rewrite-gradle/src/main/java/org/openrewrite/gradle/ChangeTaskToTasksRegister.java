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
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.tree.K;

import java.util.ArrayList;
import java.util.List;

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
        return Preconditions.check(new IsBuildGradle<>(), new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
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

    private static class GroovyVisitor extends GroovyIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!isTaskDeclaration(m, getCursor())) {
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

            StringBuilder template = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
            Expression select = m.getSelect();
            if (select != null) {
                template.append("#{any()}.");
                parameters.add(select.withType(JavaType.Primitive.Void)); // Bypass the type check for `project.tasks` not found
            }
            template.append("tasks.register(\"#{}\")");
            parameters.add(inner.getSimpleName());

            J.MethodInvocation taskRegistration = GroovyTemplate.apply(template.toString(), getCursor(), m.getCoordinates().replace(), parameters.toArray());
            List<Expression> appendArgs = new ArrayList<>();
            if (taskType != null) {
                appendArgs.add(taskType);
            }
            if (taskLambda != null) {
                appendArgs.add(taskLambda);
            }
            return taskRegistration.withArguments(ListUtils.concatAll(taskRegistration.getArguments(), appendArgs));
        }

        private boolean isTaskDeclaration(J.MethodInvocation method, Cursor cursor) {
            if (!"task".equals(method.getSimpleName())) {
                return false;
            }
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
    }

    private static class KotlinVisitor extends KotlinIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!isTaskDeclaration(m)) {
                return m;
            }

            List<Expression> args = m.getArguments();
            if (args.isEmpty() || !(args.get(0) instanceof J.Literal)) {
                return m;
            }

            J.Literal taskName = (J.Literal) args.get(0);
            if (taskName.getValue() == null || !TypeUtils.isString(taskName.getType())) {
                return m;
            }

            J.Lambda taskLambda = null;
            if (args.size() == 2 && args.get(1) instanceof J.Lambda) {
                taskLambda = (J.Lambda) args.get(1);
            }

            StringBuilder template = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
            Expression select = m.getSelect();
            if (select != null) {
                template.append("#{any()}.");
                parameters.add(select);
            }
            template.append("tasks.register(\"#{}\")");
            parameters.add(taskName.getValue());

            J.MethodInvocation taskRegistration = KotlinTemplate.apply(template.toString(), getCursor(), m.getCoordinates().replace(), parameters.toArray());

            return m.withSelect(taskRegistration.getSelect())
              .withName(taskRegistration.getName())
              .withArguments(ListUtils.concat((taskRegistration).getArguments(), taskLambda));
        }

        private boolean isTaskDeclaration(J.MethodInvocation method) {
            if (!"task".equals(method.getSimpleName())) {
                return false;
            }

            Expression select = method.getSelect();
            if (select == null) {
                return true;
            }
            return select instanceof J.Identifier && "project".equals(((J.Identifier) select).getSimpleName());
        }
    }
}
