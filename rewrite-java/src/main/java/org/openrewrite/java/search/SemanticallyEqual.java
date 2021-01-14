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

import org.openrewrite.*;
import org.openrewrite.java.JavaProcessor;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.Objects;

/*
 * Semantically Equal recursively checks the equality of each element of two ASTs to determine if the two trees are semantically equal.
 * This is necessary because ASTs are so frequently recreated that merely comparing the IDs of two ASTs is ineffective.
 * SemanticallyEqual has only been implemented for annotations for now.
 */
@Incubating(since = "6.0.0")
public class SemanticallyEqual {

    private SemanticallyEqual() {}

    public static boolean areEqual(J firstElem, J secondElem) {
        SemanticallyEqualProcessor sep = new SemanticallyEqualProcessor();
        sep.visit(firstElem, secondElem); // returns null, but changes value of class variable isEqual
        return sep.isEqual;
    }

    /**
     * Note: The following visit methods extend JavaProcessor in order to inherit access to the
     * visitor pattern set up there; however, the necessity to return a J did not fit the purposes of
     * SemanticallyEqualProcessor, so while the equality is tracked in isEqual, all the visitors return null.
     */
    private static class SemanticallyEqualProcessor extends JavaProcessor<J> {
        boolean isEqual;

        SemanticallyEqualProcessor() {
            isEqual = true;
        }

        @Override
        public J visitAnnotation(J.Annotation firstAnnotation, J second) {
            if (!(second instanceof J.Annotation)) {
                isEqual = false;
                return null;
            }
            J.Annotation secondAnnotation = (J.Annotation) second;

            if (firstAnnotation.getArgs() != null && secondAnnotation.getArgs() != null) {
                if (firstAnnotation.getArgs().getElem() != null &&
                        secondAnnotation.getArgs().getElem() != null &&
                        firstAnnotation.getArgs().getElem().size() == secondAnnotation.getArgs().getElem().size()) {

                    List<JRightPadded<Expression>> firstArgs = firstAnnotation.getArgs().getElem();
                    List<JRightPadded<Expression>> secondArgs = secondAnnotation.getArgs().getElem();

                    for (int i = 0; i < firstArgs.size(); i++) {
                        this.visit(firstArgs.get(i).getElem(), secondArgs.get(i).getElem());
                    }
                }
                else {
                    isEqual = false;
                    return null;
                }

                this.visitTypeName(firstAnnotation.getAnnotationType(), secondAnnotation.getAnnotationType());
            }
            return null;
        }

        @Override
        public J visitIdentifier(J.Ident firstIdent, J second) {
            if (!(second instanceof J.Ident)) {
                isEqual = false;
                return null;
            }
            J.Ident secondIdent = (J.Ident) second;

            isEqual = isEqual && Objects.equals(firstIdent.getType(), secondIdent.getType()) &&
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
                }
                else {
                    isEqual = isEqual &&
                            typeEquals(firstFieldAccess.getType(), secondFieldAccess.getType()) &&
                            typeEquals(firstFieldAccess.getTarget().getType(), secondFieldAccess.getTarget().getType());
                }
            }

            return null;
        }

        @Override
        public J visitAssign(J.Assign firstAssign, J second) {
            if (!(second instanceof J.Assign)) {
                isEqual = false;
                return null;
            }
            J.Assign secondAssign = (J.Assign) second;

            isEqual = isEqual &&
                    Objects.equals(firstAssign.getType(), secondAssign.getType()) &&
                    SemanticallyEqual.areEqual(firstAssign.getVariable(), secondAssign.getVariable()) &&
                    SemanticallyEqual.areEqual(firstAssign.getAssignment().getElem(), secondAssign.getAssignment().getElem());

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
        public NameTree visitTypeName(NameTree firstTypeName, J second) {
            if (!(second instanceof NameTree)) {
                isEqual = false;
                return null;
            }
            NameTree secondTypeName = (NameTree) second;

            isEqual = isEqual && identEquals((J.Ident) firstTypeName, (J.Ident) secondTypeName);

            return null;
        }

        private static boolean identEquals(J.Ident thisIdent, J.Ident otherIdent) {
            return Objects.equals(thisIdent.getSimpleName(), otherIdent.getSimpleName()) && typeEquals(thisIdent.getType(), otherIdent.getType());
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
