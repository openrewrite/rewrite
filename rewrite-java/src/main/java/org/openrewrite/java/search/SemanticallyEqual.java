/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.search;

import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicInteger;

@Incubating(since = "6.0.0")
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
            boolean areArgsEqual;

            if(annotation.getArgs() != null && otherAnnotation.getArgs() != null) {
                if(annotation.getArgs().getArgs() != null &&
                    otherAnnotation.getArgs().getArgs() != null &&
                    annotation.getArgs().getArgs().size() == otherAnnotation.getArgs().getArgs().size()) {

                    AtomicInteger index = new AtomicInteger(0);
                    areArgsEqual = annotation.getArgs().getArgs().stream()
                        .allMatch(arg ->
                                new SemanticallyEqual(arg)
                                        .visit(otherAnnotation.getArgs().getArgs().get(index.getAndIncrement()))
                        );
                } else {
                    return false;
                }
            } else if(annotation.getArgs() == null && otherAnnotation.getArgs() == null) {
                areArgsEqual = true;
            } else {
                return false;
            }

            return areArgsEqual && new SemanticallyEqual(annotation.getAnnotationType()).visit(otherAnnotation.getAnnotationType());
        }

        return false;
    }

    @Override
    public Boolean visitIdentifier(J.Ident otherIdent) {
        if(tree instanceof J.Ident) {
            J.Ident ident = (J.Ident) tree;
            boolean isTypeEqual;

            if(ident.getType() != null && otherIdent.getType() != null) {
                isTypeEqual = ident.getType().equals(otherIdent.getType());
            } else if(ident.getType() == null && otherIdent.getType() == null) {
                isTypeEqual = true;
            } else {
                return false;
            }
            return isTypeEqual && ident.getSimpleName().equals(otherIdent.getSimpleName());
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
