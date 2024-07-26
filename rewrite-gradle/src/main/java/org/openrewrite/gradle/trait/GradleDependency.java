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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.Trait;

@Value
public class GradleDependency implements Trait<J.MethodInvocation> {
    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("* dependencies(groovy.lang.Closure)");
    private static final MethodMatcher DEPENDENCY_CONFIGURATION_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    GradleProject gradleProject;
    Cursor cursor;

    public GradleDependency(GradleProject gradleProject, Cursor cursor) {
        this.gradleProject = gradleProject;
        this.cursor = cursor;

        debug();
    }

    private boolean isLikelyDependencyConfiguration(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation methodInvocation = cursor.getValue();
        if (DEPENDENCY_CONFIGURATION_MATCHER.matches(methodInvocation)) {
            return true;
        }
        // If it's a configuration created by a plugin, we may not be able to type-attribute it
        // In the absence of type-attribution use its presence within a dependencies block to approximate
        if (methodInvocation.getType() != null) {
            return false;
        }

        boolean isLikelyDependencyConfiguration = false;
        while (cursor != null) {
            if (cursor.getValue() instanceof J.MethodInvocation) {
                J.MethodInvocation m = cursor.getValue();
                String methodName = m.getSimpleName();
                if ("constraints".equals(methodName) || "project".equals(methodName) || "modules".equals(methodName)
                        || "module".equals(methodName) || "file".equals(methodName) || "files".equals(methodName)) {
                    cursor = null;
                    isLikelyDependencyConfiguration = false;
                    break;
                }
                if (DEPENDENCY_DSL_MATCHER.matches(m)) {
                    isLikelyDependencyConfiguration = true;
                    cursor = null;
                    break;
                }
            }
            cursor = cursor.getParent();
        }

        // There are cases where the configurations don't populate correctly.
        if (gradleProject.getNameToConfiguration().isEmpty()) {
            return isLikelyDependencyConfiguration;
        }

        if (isLikelyDependencyConfiguration) {
            return gradleProject.getNameToConfiguration().keySet().stream()
                    .anyMatch(configurationName -> methodInvocation.toString().startsWith(configurationName));
        }

        return false;
    }

    public void debug() {
        gradleProject.getNameToConfiguration().forEach((name, configuration) -> {
            System.err.println("Configuration: " + name);
        });
        System.err.println();
    }

    public boolean isDependency() {
        return isLikelyDependencyConfiguration(cursor);
    }

    public boolean isNotDependency() {
        return !isDependency();
    }
}
