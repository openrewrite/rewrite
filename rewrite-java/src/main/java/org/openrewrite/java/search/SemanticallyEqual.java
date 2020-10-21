package org.openrewrite.java.search;

import org.openrewrite.Tree;
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicInteger;

public class SemanticallyEqual extends AbstractJavaSourceVisitor<Boolean> {
    private final Tree tree;

    public SemanticallyEqual(Tree tree) {
        this.tree = tree;
    }

    @Override
    public Boolean defaultTo(Tree t) {
        return true;
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
        return r1 && r2;
    }

    @Override
    public Boolean visitAnnotation(J.Annotation otherAnnotation) {
        if(tree instanceof J.Annotation) {
            J.Annotation annotation = (J.Annotation) tree;
            AtomicInteger index = new AtomicInteger(0);

            return new SemanticallyEqual(annotation.getAnnotationType()).visit(otherAnnotation.getAnnotationType()) &&
                otherAnnotation.getArgs() != null ?
                annotation.getArgs().getArgs().stream().allMatch(arg ->
                    (otherAnnotation.getArgs().getArgs() != null &&
                        otherAnnotation.getArgs().getArgs().size() > index.get()) ?
                        new SemanticallyEqual(arg)
                                .visit(otherAnnotation.getArgs().getArgs().get(index.getAndIncrement())) :
                        false
                ) :
                annotation.getArgs() == null;
        }

        return false;
    }

    @Override
    public Boolean visitIdentifier(J.Ident otherIdent) {
        if(tree instanceof J.Ident) {
            J.Ident ident = (J.Ident) tree;

            return ident.getSimpleName().equals(otherIdent.getSimpleName()) &&
                (ident.getType() != null ?
                    ident.getType().equals(otherIdent.getType()) :
                    otherIdent.getType() == null);
        }

        return false;
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess otherFieldAccess) {
        if(tree instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) tree;

            // if the field access is a class literal
            if(fieldAccess.getSimpleName().equals("class")) {
                if(!otherFieldAccess.getSimpleName().equals("class")) {
                    return false;
                }
                else {
                    return fieldAccess.getTarget().getType() == null ||
                            fieldAccess.getTarget().getType().equals(otherFieldAccess.getTarget().getType());
                }
            }
        }

        return false;
    }

    @Override
    public Boolean visitAssign(J.Assign otherAssign) {
        if(tree instanceof J.Assign) {
            J.Assign assign = (J.Assign) tree;

            return new SemanticallyEqual(assign.getVariable()).visit(otherAssign.getVariable()) &&
                    new SemanticallyEqual(assign.getAssignment()).visit(otherAssign.getAssignment()) &&
                    assign.getType().equals(otherAssign.getType());
        }
        return false;
    }

    @Override
    public Boolean visitLiteral(J.Literal otherLiteral) {
        if(tree instanceof J.Literal) {
            J.Literal literal = (J.Literal) tree;

            if(otherLiteral.getValue() == null) {
                return literal.getValue() == null;
            }

            return otherLiteral.getValue().equals(((J.Literal) tree).getValue());
        }

        return false;
    }

}
