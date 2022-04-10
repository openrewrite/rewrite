package org.openrewrite.java.dataflow;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.function.Predicate;

/**
 * from Constructor fileReader, Call call, Expr src
 * where
 *   fileReader.getDeclaringType().hasQualifiedName("java.io", "FileReader") and
 *   call.getCallee() = fileReader and
 *   DataFlow::localFlow(DataFlow::exprNode(src), DataFlow::exprNode(call.getArgument(0)))
 * select src
 */
public class FileTempDirThing<P> extends JavaIsoVisitor<P> {
    private static final MethodMatcher FILE_MKDIRS = new MethodMatcher("java.io.File mkdirs()");
    private static final MethodMatcher FILE_DELETE = new MethodMatcher("java.io.File delete()");

    /**
     f1 = new File();
     f2 = f1.identity();

     try {
     File f = Files.createTempFile(null, null).toFile();

     // a = "http:"
     // b = a + "hi"

     // through the f, not through the delete
     //      f
     //      f.delete()
     f = new File();
     f.delete();

     // from the assignment (source) to the subject of delete (sink)
     // from the subject of delete (source) to the the subject of mkdirs (sink)

     boolean m = f.mkdirs();
     if (!m) {
     throw new RuntimeException("e");
     }
     } catch(Throwable t) {
     }
     */

    // Example of fixing at the sink
    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        if (FILE_MKDIRS.matches(m) && dataflow().anyLocalFlow(FILE_DELETE::matches, m)) {
        }
        return m;
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
