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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTaskToTasksRegister extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change `task` to `tasks.register`";
    }

    @Override
    public String getDescription() {
        return "Changes eager task creation `task myTask(...)` to lazy registration `tasks.register(\"myTask\", ...)` in Gradle build scripts. " +
                "This aligns with modern Gradle best practices for improved build performance.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            private final JavaTemplate registerNoClosureTemplate = JavaTemplate
                    .builder("tasks.register(#{any(java.lang.String)}, #{any(org.openrewrite.java.tree.Expression)})")
                    .build();

            private final JavaTemplate registerWithClosureTemplate = JavaTemplate
                    .builder("tasks.register(#{any(java.lang.String)}, #{any(org.openrewrite.java.tree.Expression)}, #{any(org.openrewrite.java.tree.J.Lambda)})")
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!"task".equals(m.getSimpleName())) {
                    return m;
                }
                List<Expression> taskCallArgs = m.getArguments();
                if (taskCallArgs.size() != 1 || !(taskCallArgs.get(0) instanceof J.MethodInvocation)) {
                    return m;
                }

                J.MethodInvocation taskDefinitionInvocation = (J.MethodInvocation) taskCallArgs.get(0);
                List<Expression> taskDefArgs = taskDefinitionInvocation.getArguments();
                Expression taskTypeExpression = null;
                J.Lambda taskConfigurationLambda = null;
                for (Expression arg : taskDefArgs) {
                    if (arg instanceof G.MapEntry) {
                        G.MapEntry mapEntry = (G.MapEntry) arg;
                        if (mapEntry.getKey() instanceof J.Literal && "type".equals(((J.Literal) mapEntry.getKey()).getValue())) {
                            taskTypeExpression = mapEntry.getValue();
                        }
                    } else if (arg instanceof J.Lambda) {
                        taskConfigurationLambda = (J.Lambda) arg;
                    }
                }
                if (taskTypeExpression == null) {
                    return m;
                }

                J.Literal literalTaskName = getLiteralTaskName(taskDefinitionInvocation);
                if (taskConfigurationLambda == null) {
                    m = registerNoClosureTemplate.apply(
                            getCursor(),
                            m.getCoordinates().replace(),
                            literalTaskName,
                            taskTypeExpression
                    );
                } else {
                    m = registerWithClosureTemplate.apply(
                            getCursor(),
                            m.getCoordinates().replace(),
                            literalTaskName,
                            taskTypeExpression,
                            taskConfigurationLambda
                    );
                }

                return m;
            }
        });
    }

    private J.Literal getLiteralTaskName(J.MethodInvocation taskDefinitionInvocation) {
        String taskName = taskDefinitionInvocation.getSimpleName();
        if (taskName.startsWith("\"") && taskName.endsWith("\"") && taskName.length() > 1) {
            taskName = taskName.substring(1, taskName.length() - 1);
        }
        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                taskName,
                "\"" + taskName + "\"",
                null,
                JavaType.Primitive.String
        );
    }
}
