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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;

import java.util.List;
import java.util.Objects;

/*
 * Recursively checks the equality of each element of two ASTs to determine if two trees are semantically equal.
 */
@Incubating(since = "6.0.0")
public class SemanticallyEqual {

    private SemanticallyEqual() {
    }

    public static boolean areEqual(J firstElem, J secondElem) {
        SemanticallyEqualVisitor sep = new SemanticallyEqualVisitor();
        sep.visit(firstElem, secondElem); // returns null, but changes value of class variable isEqual
        return sep.isEqual;
    }

    /**
     * Note: The following visit methods extend JavaVisitor in order to inherit access to the
     * visitor pattern set up there; however, the necessity to return a J did not fit the purposes of
     * SemanticallyEqualVisitor, so while the equality is tracked in isEqual, all the visitors return null.
     */
    @SuppressWarnings("ConstantConditions")
    private static class SemanticallyEqualVisitor extends JavaVisitor<J> {
        boolean isEqual;

        SemanticallyEqualVisitor() {
            isEqual = true;
        }

        @Override
        public J visitAnnotation(J.Annotation firstAnnotation, J second) {
            if (!(second instanceof J.Annotation)) {
                isEqual = false;
                return null;
            }
            J.Annotation secondAnnotation = (J.Annotation) second;

            if (firstAnnotation.getArguments() != null && secondAnnotation.getArguments() != null) {
                if (firstAnnotation.getArguments().size() == secondAnnotation.getArguments().size()) {

                    List<Expression> firstArgs = firstAnnotation.getArguments();
                    List<Expression> secondArgs = secondAnnotation.getArguments();

                    for (int i = 0; i < firstArgs.size(); i++) {
                        this.visit(firstArgs.get(i), secondArgs.get(i));
                    }
                } else {
                    isEqual = false;
                    return null;
                }
            }
            this.visitTypeName(firstAnnotation.getAnnotationType(), secondAnnotation.getAnnotationType());
            return null;
        }

        @Override
        public J visitIdentifier(J.Identifier firstIdent, J second) {
            if (!(second instanceof J.Identifier)) {
                isEqual = false;
                return null;
            }
            J.Identifier secondIdent = (J.Identifier) second;

            isEqual = isEqual && typeEquals(firstIdent.getType(), secondIdent.getType()) &&
                    firstIdent.getSimpleName().equals(secondIdent.getSimpleName());

            return null;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess firstFieldAccess, J second) {
            if (!(second instanceof J.FieldAccess)) {
                isEqual = false;
                return null;
            }
            J.FieldAccess secondFieldAccess = (J.FieldAccess) second;

            // Class literals are the only kind of FieldAccess which can appear within annotations
            // Functionality to correctly determine semantic equality of other kinds of field access will come later
            if (firstFieldAccess.getSimpleName().equals("class")) {
                if (!secondFieldAccess.getSimpleName().equals("class")) {
                    isEqual = false;
                    return null;
                } else {
                    isEqual = isEqual &&
                            typeEquals(firstFieldAccess.getType(), secondFieldAccess.getType()) &&
                            typeEquals(firstFieldAccess.getTarget().getType(), secondFieldAccess.getTarget().getType());
                }
            }

            return null;
        }

        @Override
        public J visitAssignment(J.Assignment firstAssignment, J second) {
            if (!(second instanceof J.Assignment)) {
                isEqual = false;
                return null;
            }
            J.Assignment secondAssignment = (J.Assignment) second;

            isEqual = isEqual &&
                    Objects.equals(firstAssignment.getType(), secondAssignment.getType()) &&
                    SemanticallyEqual.areEqual(firstAssignment.getVariable(), secondAssignment.getVariable()) &&
                    SemanticallyEqual.areEqual(firstAssignment.getAssignment(), secondAssignment.getAssignment());

            return null;
        }

        @Override
        public J visitLiteral(J.Literal firstLiteral, J second) {
            if (!(second instanceof J.Literal)) {
                isEqual = false;
                return null;
            }
            J.Literal secondLiteral = (J.Literal) second;

            isEqual = isEqual && Objects.equals(firstLiteral.getValue(), secondLiteral.getValue());

            return null;
        }

        @Override
        public <N extends NameTree> N visitTypeName(N firstTypeName, J second) {
            if (!(second instanceof NameTree)) {
                isEqual = false;
                return null;
            }
            isEqual = isEqual && typeEquals(firstTypeName.getType(), ((NameTree) second).getType());
            return null;
        }

        private static boolean typeEquals(JavaType thisType, JavaType otherType) {
            if (thisType == null) {
                return otherType == null;
            }
            if (thisType instanceof JavaType.FullyQualified) {
                if (!(otherType instanceof JavaType.FullyQualified)) {
                    return false;
                }
                return ((JavaType.FullyQualified) thisType).getFullyQualifiedName().equals(((JavaType.FullyQualified) otherType).getFullyQualifiedName());
            }

            return thisType.deepEquals(otherType);
        }
    }
}
