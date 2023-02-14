package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

public class ChainStringBuilderAppendCalls extends Recipe {
    // private static final MethodMatcher GET_BYTES = new MethodMatcher("java.lang.String getBytes()");
    private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");


    @Override
    public String getDisplayName() {
        return "Replace with chained 'StringBuilder.append()` Calls";
    }

    @Override
    public String getDescription() {
        return "Replace String concatenation in arguments of 'StringBuilder.append()' with chained append calls.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(STRING_BUILDER_APPEND);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                System.out.println(TreeVisitingPrinter.printTree(getCursor()));
                return super.visitCompilationUnit(cu, executionContext);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                // J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                if (STRING_BUILDER_APPEND.matches(method)) {

                    // collect all expressions if it's contains

                    List<Expression> expressionList = new ArrayList<>();


                }



                return method;
            }
        };
    }
}
