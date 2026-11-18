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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Optional;
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

    /**
     * The variable this access reads or writes, found by its position in the tree rather than by its type
     * attribution, so it tells apart two locals of one name, type and method declared in sibling blocks.
     */
    @Incubating(since = "8.112.0")
    public Optional<Scope.Binding> getBinding() {
        String name = getTree().getSimpleName();
        Object parent = cursor.getParentTreeCursor().getValue();
        if (!(parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == getTree())) {
            return Scope.at(cursor).lookup(name);
        }
        // A field selected from `this` or `super` belongs to the innermost class, and one selected from the name of an
        // enclosing class belongs to that class. Another instance's is not bound here.
        Expression target = ((J.FieldAccess) parent).getTarget();
        if (!(target instanceof J.Identifier)) {
            return Optional.empty();
        }
        String qualifier = ((J.Identifier) target).getSimpleName();
        boolean self = "this".equals(qualifier) || "super".equals(qualifier);
        for (Scope scope = Scope.at(cursor); scope != null; scope = scope.enclosing().orElse(null)) {
            J tree = scope.getTree();
            if (self && (tree instanceof J.ClassDeclaration || tree instanceof J.NewClass) ||
                tree instanceof J.ClassDeclaration && ((J.ClassDeclaration) tree).getSimpleName().equals(qualifier)) {
                for (Scope.Binding binding : scope.getBindings()) {
                    if (binding.getNamespace() == Scope.Namespace.VARIABLE && binding.getName().equals(name) && !binding.isLocal()) {
                        return Optional.of(binding);
                    }
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
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

            // Without attribution, the identifier's position decides, under the source file's own language rules.
            return Scope.service(cursor).namespaceOf(cursor) == Scope.Namespace.VARIABLE ? checkFilters(cursor) : null;
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
