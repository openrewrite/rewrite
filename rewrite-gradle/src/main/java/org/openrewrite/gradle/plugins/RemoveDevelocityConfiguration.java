/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import static org.openrewrite.Preconditions.or;

public class RemoveDevelocityConfiguration extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove Develocity configuration";
    }

    @Override
    public String getDescription() {
        return "Remove Develocity configuration from a Gradle build.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                or(new IsBuildGradle<>(), new IsSettingsGradle<>()),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if ("develocity".equals(method.getSimpleName()) ||
                                "gradleEnterprise".equals(method.getSimpleName())) {
                            return null;
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
        );
    }
}
