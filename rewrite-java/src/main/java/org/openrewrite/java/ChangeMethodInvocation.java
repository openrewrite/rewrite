package org.openrewrite.java;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ChangeMethodInvocation extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "java.lang.Thread run()")
    String methodPattern;

    @Option(displayName = "New called method name",
            description = "New name of the method to be called.",
            example = "start")
    String newName;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Change method name in method invocation";
    }

    @Override
    public String getDescription() {
        return "Change the name of the invoked method.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(new MethodMatcher(methodPattern, matchOverrides));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern, matchOverrides);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = super.visitMethodInvocation(method, context);
                if (matcher.matches(m)) {
                    return m.withName(m.getName().withSimpleName(newName));
                }
                return m;
            }
        };
    }
}
