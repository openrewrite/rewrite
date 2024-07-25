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
