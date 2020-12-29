package org.openrewrite.java.search;

import org.openrewrite.EvalContext;
import org.openrewrite.java.JavaEvalVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Paint;

public class FindMethodInvocations extends JavaEvalVisitor {
    private MethodMatcher methodMatcher;

    public void setMethodMatcher(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, EvalContext ctx) {
        if(methodMatcher.matches(method)) {
            return method.withMarkers(method.getMarkers()
                    .addOrUpdate(new Paint()));
        }
        return super.visitMethodInvocation(method, ctx);
    }
}

class AddTestAnnotation extends JavaEvalVisitor {
    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, EvalContext ctx) {
        doOnNext(new FindMethodInvocations(), getCursor());
        doOnNext(new MaybeAddAnnotation());
        return cu;
    }

    class MaybeAddAnnotation extends JavaEvalVisitor {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, EvalContext ctx) {
            if(method.getMarkers().findFirst(Paint.class)) {

            }
            return super.visitMethodInvocation(method, ctx);
        }
    }
}
