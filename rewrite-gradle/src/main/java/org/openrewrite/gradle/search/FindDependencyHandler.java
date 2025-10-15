package org.openrewrite.gradle.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.trait.GradleDependencies;
import org.openrewrite.marker.SearchResult;

public class FindDependencyHandler extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find Gradle `dependencies` blocks";
    }

    @Override
    public String getDescription() {
        return "Find the dependency handler containing any number of dependency definitions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GradleDependencies.Matcher()
                .asVisitor(gd -> SearchResult.found(gd.getTree()));
    }
}
