package org.openrewrite.gradle.util;

import org.openrewrite.Cursor;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.function.Predicate;

public class DependencyMatchPredicate implements Predicate<Cursor> {

    @Override
    public boolean test(Cursor cursor) {
        GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher();
        /*
        We use the MethodMatcher approach if the Trait matcher doesn't match as currently the Trait
        matcher does not match dependencies defined in the buildScript
         */
        return gradleDependencyMatcher.get(cursor).isPresent() || methodMatches(cursor);
    }

    private boolean methodMatches(Cursor cursor) {
        Object object = cursor.getValue();
        if (object instanceof J.MethodInvocation) {
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");
            J.MethodInvocation methodInvocation = (J.MethodInvocation) object;
            return dependencyDsl.matches(methodInvocation);
        }

        return false;
    }
}