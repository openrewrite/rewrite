/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRepository extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove repository";
    }

    @Override
    public String getDescription() {
        return "Removes a repository from Gradle build scripts. Named repositories include \"jcenter\", \"mavenCentral\", \"mavenLocal\", and \"google\".";
    }

    @Option(displayName = "Repository",
            description = "The name of the repository to remove",
            example = "jcenter")
    String repository;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (repository.equals(method.getSimpleName())) {
                    try {
                        Cursor cursor = getCursor().dropParentUntil(e -> e instanceof J.MethodInvocation);
                        if ("repositories".equals(((J.MethodInvocation) cursor.getValue()).getSimpleName())) {
                            return null;
                        }
                    } catch (Exception ignored) {}
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
