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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import static org.openrewrite.Preconditions.or;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveExtension extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove build extension by name";
    }

    @Override
    public String getDescription() {
        return "Remove a Gradle build extension from `settings.gradle(.kts)` or `build.gradle(.kts)` files.";
    }

    @Option(displayName = "Method name",
            description = "The name of the build extension to remove, e.g., `buildCache`.",
            example = "buildCache")
    String methodName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (methodName.equals(method.getSimpleName())) {
                            return null;
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
        );
    }
}
