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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseMainClassPropertyForApplication extends Recipe {

    private static final String IN_APPLICATION = "IN_APPLICATION";

    String displayName = "Use `mainClass` instead of `mainClassName` in the `application` block";

    String description = "The `mainClassName` property on the `application` extension was deprecated in Gradle 6.4 and removed in Gradle 9.0. " +
            "Use the `mainClass` property instead. " +
            "See the [Gradle upgrade guide](https://docs.gradle.org/9.0.0/userguide/upgrading_major_version_9.html) for more information.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if ("application".equals(method.getSimpleName())) {
                    getCursor().putMessage(IN_APPLICATION, true);
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                if (getCursor().getNearestMessage(IN_APPLICATION) == null) {
                    return assignment;
                }
                Expression variable = assignment.getVariable();
                if (variable instanceof J.Identifier) {
                    J.Identifier id = (J.Identifier) variable;
                    if ("mainClassName".equals(id.getSimpleName())) {
                        return assignment.withVariable(id.withSimpleName("mainClass"));
                    }
                } else if (variable instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) variable;
                    if ("mainClassName".equals(fieldAccess.getSimpleName())) {
                        return assignment.withVariable(
                                fieldAccess.withName(fieldAccess.getName().withSimpleName("mainClass"))
                        );
                    }
                }
                return assignment;
            }
        });
    }
}
