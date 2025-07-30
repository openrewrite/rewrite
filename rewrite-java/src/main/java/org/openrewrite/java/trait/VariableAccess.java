/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.trait.internal.MaybeParenthesesPair;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Incubating(since = "8.30.0")
@Value
public class VariableAccess implements Trait<J.Identifier> {
    Cursor cursor;

    /**
     * A write access to a variable occurs as the destination of an assignment.
     */
    public boolean isWriteAccess() {
        MaybeParenthesesPair pair = MaybeParenthesesPair.from(cursor);
        if (pair.getParent() instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) pair.getParent();
            return assignment.getVariable() == pair.getTree();
        }
        if (pair.getParent() instanceof J.Unary) {
            J.Unary unary = (J.Unary) pair.getParent();
            return unary.getExpression() == pair.getTree() &&
                   unary.getOperator().isModifying();
        }
        return false;
    }

    /**
     * A read access to a variable. In other words, it is a variable access that does
     * _not_ occur as the destination of a simple assignment, but it may occur as
     * the destination of a compound assignment or a unary assignment.
     */
    public boolean isReadAccess() {
        if (isWriteAccess()) {
            return false;
        }
        MaybeParenthesesPair pair = MaybeParenthesesPair.from(cursor);
        if (pair.getParent() instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) pair.getParent();
            return assignment.getVariable() != pair.getTree();
        }
        if (pair.getParent() instanceof J.Unary) {
            J.Unary unary = (J.Unary) pair.getParent();
            return unary.getExpression() == pair.getTree() &&
                   !unary.getOperator().isModifying();
        }
        return true;
    }

    public static class Matcher extends SimpleTraitMatcher<VariableAccess> {
        private Predicate<JavaType> typeTest = t -> true;

        public Matcher isOfType(JavaType type) {
            this.typeTest = t -> TypeUtils.isOfType(t, type);
            return this;
        }

        public Matcher isOfClassType(String fullyQualifiedName) {
            this.typeTest = t -> TypeUtils.isOfClassType(t, fullyQualifiedName);
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<VariableAccess, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitIdentifier(J.Identifier ident, P p) {
                    VariableAccess varAccess = test(getCursor());
                    return varAccess != null ?
                            (J) visitor.visit(varAccess, p) :
                            super.visitIdentifier(ident, p);
                }
            };
        }

        /**
         * Since variable access is a trait of a leaf LST element (J.Identifier),
         * there are no higher elements in the tree that could potentially match.
         */
        @Override
        public Stream<VariableAccess> higher(Cursor cursor) {
            return Stream.empty();
        }

        @Override
        protected @Nullable VariableAccess test(Cursor cursor) {
            if (!(cursor.getValue() instanceof J.Identifier)) {
                return null;
            }

            J.Identifier ident = cursor.getValue();
            String name = ident.getSimpleName();
            if ("this".equals(name) || "super".equals(name) ||
                checkNamePart(cursor, J.VariableDeclarations.NamedVariable.class, J.VariableDeclarations.NamedVariable::getName)) {
                return null;
            }

            if (ident.getFieldType() != null) {
                return checkFilters(cursor);
            }

            if (checkNamePart(cursor, J.ClassDeclaration.class, J.ClassDeclaration::getName) ||
                checkNamePart(cursor, J.MethodDeclaration.class, J.MethodDeclaration::getName) ||
                checkNamePart(cursor, J.MethodInvocation.class, J.MethodInvocation::getName) ||
                checkNamePart(cursor, J.NewClass.class, J.NewClass::getClazz) ||
                checkNamePart(cursor, J.FieldAccess.class, J.FieldAccess::getName) ||
                checkNamePart(cursor, J.MethodInvocation.class, J.MethodInvocation::getName) ||
                checkNamePart(cursor, J.ParameterizedType.class, J.ParameterizedType::getClazz) ||
                cursor.firstEnclosing(Javadoc.class) != null) {
                return null;
            }

            Cursor parent = cursor.getParentTreeCursor();
            if (checkParent(parent, J.TypeCast.class, j -> j.getClazz().getTree() == ident)) {
                return null;
            }

            // Special case for annotations, where the left side of the assignment is the annotation field name,
            // and not a variable access.
            if (checkNamePart(cursor, J.Assignment.class, J.Assignment::getVariable) &&
                checkParent(parent, J.Annotation.class, j -> j.getArguments() != null && j.getArguments().contains(ident))) {
                return null;
            }

            if (checkNamePart(cursor, J.ArrayAccess.class, J.ArrayAccess::getIndexed) ||
                checkNamePart(cursor, J.ArrayDimension.class, J.ArrayDimension::getIndex) ||
                checkNamePart(cursor, J.ControlParentheses.class, J.ControlParentheses::getTree) ||
                checkNamePart(cursor, J.ForEachLoop.Control.class, J.ForEachLoop.Control::getIterable) ||
                checkNamePart(cursor, J.ForLoop.Control.class, J.ForLoop.Control::getCondition) ||
                checkNamePart(cursor, J.Parentheses.class, J.Parentheses::getTree) ||
                checkNamePart(cursor, J.TypeCast.class, J.TypeCast::getExpression) ||
                checkNamePart(cursor, J.Unary.class, J.Unary::getExpression) ||
                checkNamePart(cursor, J.VariableDeclarations.NamedVariable.class, J.VariableDeclarations.NamedVariable::getInitializer) ||
                checkParent(cursor, J.Assignment.class, a -> a.getVariable() == ident || a.getAssignment() == ident) ||
                checkParent(cursor, J.Binary.class, b -> b.getLeft() == ident || b.getRight() == ident) ||
                checkParent(cursor, J.MethodInvocation.class, m -> m.getSelect() == ident || m.getArguments().contains(ident)) ||
                checkParent(cursor, J.NewClass.class, n -> n.getEnclosing() == ident || n.getArguments().contains(ident)) ||
                checkParent(cursor, J.NewArray.class, n -> n.getInitializer() != null && n.getInitializer().contains(ident)) ||
                checkParent(cursor, J.Ternary.class, t -> t.getCondition() == ident ||
                                                          t.getTruePart() == ident ||
                                                          t.getFalsePart() == ident) ||
                checkParent(cursor, J.Annotation.class, parentAnnotation -> parentAnnotation.getArguments() != null && parentAnnotation.getArguments().contains(ident))) {
                return checkFilters(cursor);
            }

            // Catchall. Useful point for setting a breakpoint when debugging.
            return null;
        }

        private @Nullable VariableAccess checkFilters(Cursor cursor) {
            return typeTest.test(cursor.<J.Identifier>getValue().getType()) ?
                    new VariableAccess(cursor) :
                    null;
        }

        private <J2> boolean checkParent(Cursor cursor, Class<J2> parentType, Predicate<J2> test) {
            if (cursor.getValue() instanceof SourceFile) {
                return false;
            }
            Object parent = cursor.getParentTreeCursor().getValue();
            return parentType.isInstance(parent) && test.test(parentType.cast(parent));
        }

        private <J2 extends J> boolean checkNamePart(Cursor cursor, Class<J2> parentType, Function<J2, J> nameExtractor) {
            return checkParent(cursor, parentType, j -> nameExtractor.apply(j) == cursor.getValue());
        }
    }
}
