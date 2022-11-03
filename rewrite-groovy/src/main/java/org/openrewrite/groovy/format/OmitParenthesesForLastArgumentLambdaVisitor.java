package org.openrewrite.groovy.format;

import org.openrewrite.Tree;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import static org.openrewrite.Tree.randomId;

public class OmitParenthesesForLastArgumentLambdaVisitor<P> extends GroovyIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public OmitParenthesesForLastArgumentLambdaVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        return method.withArguments(ListUtils.mapLast(m.getArguments(), last -> {
            if (last instanceof J.Lambda) {
                J l = last.getPrefix().getWhitespace().isEmpty() ?
                        ((J.Lambda) last).withPrefix(last.getPrefix().withWhitespace(" ")) :
                        last;
                return l.withMarkers(l.getMarkers().computeByType(new OmitParentheses(randomId()), (s1, s2) -> s1));
            }
            return last;
        }));
    }

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }
}
