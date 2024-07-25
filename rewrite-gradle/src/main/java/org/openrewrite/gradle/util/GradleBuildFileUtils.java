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
package org.openrewrite.gradle.util;

import org.openrewrite.Cursor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class GradleBuildFileUtils {
    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("RewriteGradleProject dependencies(groovy.lang.Closure)");
    private static final MethodMatcher DEPENDENCY_CONFIGURATION_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    public static boolean isLikelyDependencyConfiguration(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = cursor.getValue();
        if (DEPENDENCY_CONFIGURATION_MATCHER.matches(m)) {
            return true;
        }
        // If it's a configuration created by a plugin, we may not be able to type-attribute it
        // In the absence of type-attribution use its presence within a dependencies block to approximate
        if (m.getType() != null) {
            return false;
        }
        while (cursor != null) {
            if (cursor.getValue() instanceof J.MethodInvocation) {
                m = cursor.getValue();
                String methodName = m.getSimpleName();
                if ("constraints".equals(methodName) || "project".equals(methodName) || "modules".equals(methodName)
                        || "module".equals(methodName) || "file".equals(methodName) || "files".equals(methodName)) {
                    return false;
                }
                if (DEPENDENCY_DSL_MATCHER.matches(m)) {
                    return true;
                }
            }
            cursor = cursor.getParent();
        }
        return false;
    }

}
