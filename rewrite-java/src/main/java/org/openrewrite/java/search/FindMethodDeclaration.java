package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindMethodDeclaration extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find method declaration";
    }

    @Override
    public String getDescription() {
        return "Locates the declaration of a method.";
    }

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.List add(..)")
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new DeclaresMethod<>(methodPattern, matchOverrides), new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher m = new MethodMatcher(methodPattern, matchOverrides);
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if(cd == null) {
                    return md;
                }
                if(m.matches(md, cd)) {
                    md = SearchResult.found(md);
                }
                return md;
            }
        });
    }
}
