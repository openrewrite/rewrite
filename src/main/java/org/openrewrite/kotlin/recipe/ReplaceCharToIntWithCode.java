package org.openrewrite.kotlin.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.tree.K;


public class ReplaceCharToIntWithCode extends Recipe {
    private static final MethodMatcher CHAR_TO_INT_METHOD_MATCHER = new MethodMatcher("kotlin.Char toInt()");
    private static J.FieldAccess charCodeTemplate = null;

    @Override
    public String getDisplayName() {
        return "Replace `Char#toInt()` with `Char#code`";
    }

    @Override
    public String getDescription() {
        return "Replace the usage of the deprecated `Char#toInt()` with `Char#code`. "
               + "Please ensure that your Kotlin version is 1.5 or later to support the `Char#code` property. "
               + "Note that the current implementation does not perform a Kotlin version check.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                return (K.CompilationUnit) super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method,
                                                            ExecutionContext executionContext) {
                if (CHAR_TO_INT_METHOD_MATCHER.matches(method)) {
                    J.FieldAccess codeTemplate = getCharCodeTemplate();
                    return codeTemplate.withTarget(method.getSelect()).withPrefix(method.getPrefix());
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }


    @SuppressWarnings("all")
    private static J.FieldAccess getCharCodeTemplate() {
        if (charCodeTemplate == null) {
            K.CompilationUnit kcu = KotlinParser.builder().build()
                .parse("fun method(c : Char) {c.code}")
                .map(K.CompilationUnit.class::cast)
                .findFirst()
                .get();

            charCodeTemplate = (J.FieldAccess) ((J.MethodDeclaration) kcu.getStatements().get(0)).getBody().getStatements().get(0);
        }

        return charCodeTemplate;
    }
}
