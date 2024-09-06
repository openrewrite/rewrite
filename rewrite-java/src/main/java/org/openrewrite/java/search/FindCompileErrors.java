package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

public class FindCompileErrors extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find compile errors";
    }

    @Override
    public String getDescription() {
        return "Compile errors result in a particular LST structure that can be searched for.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Erroneous visitErroneous(J.Erroneous erroneous, ExecutionContext ctx) {
                return SearchResult.found(erroneous);
            }
        };
    }
}