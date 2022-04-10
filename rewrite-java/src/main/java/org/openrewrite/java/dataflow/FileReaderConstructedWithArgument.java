package org.openrewrite.java.dataflow;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.function.Predicate;

import static org.apache.commons.lang.StringUtils.startsWith;

/**
 * from Constructor fileReader, Call call, Expr src
 * where
 *   fileReader.getDeclaringType().hasQualifiedName("java.io", "FileReader") and
 *   call.getCallee() = fileReader and
 *   DataFlow::localFlow(DataFlow::exprNode(src), DataFlow::exprNode(call.getArgument(0)))
 * select src
 */
public class FileReaderConstructedWithArgument<P> extends JavaIsoVisitor<P> {
    private static final MethodMatcher NEW_FILE_READER = new MethodMatcher("java.io.FileReader <constructor>(..)");

    // Example of fixing at the sink
    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        if (NEW_FILE_READER.matches(method)) {
            // test whether the input to this FileReader contained "http"
            if (dataflow().anyLocalFlow((J.Literal src) -> startsWith(src.getValueSource(), "http://"),
                    method.getArguments().get(0))) {
                // modify the method invocation, maybe to further sanitize the input
            }
        }
        return super.visitMethodInvocation(method, p);
    }

    // Example of fixing at the source
    @Override
    public J.Literal visitLiteral(J.Literal literal, P p) {
        // test whether this literal winds up getting used to construct a FileReader
        if (startsWith(literal.getValueSource(), "http://") &&
                dataflow().anyLocalFlow(literal, NEW_FILE_READER::matches)) {
            // modify the input itself to sanitize it
        }


        return super.visitLiteral(literal, p);
    }

    // API -----------------------------------------------

    public Dataflow dataflow() {
        return new Dataflow();
    }

    class Dataflow {
        public boolean anyLocalFlow(Predicate<? extends Expression> sourceMatches, Expression sink) {
            return false;
        }

        public boolean anyLocalFlow(Expression source, Predicate<? extends Expression> sinkMatches) {
            return false;
        }
    }
}
