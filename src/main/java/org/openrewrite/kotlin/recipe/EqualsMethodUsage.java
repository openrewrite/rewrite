package org.openrewrite.kotlin.recipe;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.tree.K;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = true)
public class EqualsMethodUsage extends Recipe {
    private static J.Binary equalsBinaryTemplate = null;

    @Override
    public String getDisplayName() {
        return "Structural equality tests should use \"==\" or \"!=\"";
    }

    @Override
    public String getDescription() {
        return "In Kotlin, == means structural equality and != structural inequality and both map to the left-side " +
               "term’s equals() function. It is, therefore, redundant to call equals() as a function. Also, == and !=" +
               " are more general than equals() and !equals() because it allows either of both operands to be null.\n" +
               "Developers using equals() instead of == or != is often the result of adapting styles from other " +
               "languages like Java, where == means reference equality and != means reference inequality.\n" +
               "The == and != operators are a more concise and elegant way to test structural equality than calling a function.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-6519");
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method,
                                           ExecutionContext executionContext) {
                method = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if ("equals".equals(method.getSimpleName()) &&
                    method.getArguments().size() == 1 &&
                    TypeUtils.isOfClassType(method.getMethodType().getReturnType(), "kotlin.Boolean") &&
                    method.getSelect() != null
                ) {
                    Expression lhs = method.getSelect();
                    Expression rhs = method.getArguments().get(0);
                    return buildEqualsBinary(lhs, rhs);
                }
                return method;
            }
        };
    }

    @SuppressWarnings("all")
    private static J.Binary buildEqualsBinary(Expression left, Expression right) {
        if (equalsBinaryTemplate == null) {
            K.CompilationUnit kcu = KotlinParser.builder().build()
                .parse("fun method(a : String, b : String) {val isSame = a == b}")
                .map(K.CompilationUnit.class::cast)
                .findFirst()
                .get();

            equalsBinaryTemplate = new KotlinVisitor<AtomicReference<J.Binary>>() {
                @Override
                public J visitBinary(J.Binary binary, AtomicReference<J.Binary> target) {
                    target.set(binary);
                    return binary;
                }
            }.reduce(kcu, new AtomicReference<J.Binary>()).get();
        }

        Space rhsPrefix = right.getPrefix();
        if (rhsPrefix.getWhitespace().isEmpty()) {
            rhsPrefix = rhsPrefix.withWhitespace(" ");
        }
        return equalsBinaryTemplate.withLeft(left.withPrefix(left.getPrefix())).withRight(right.withPrefix(rhsPrefix));
    }

}
