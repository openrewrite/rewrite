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

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction3;

@Incubating(since = "8.30.0")
@Value
public class MethodAccess implements Trait<Expression> {
    Cursor cursor;

    @RequiredArgsConstructor
    public static class Matcher extends SimpleTraitMatcher<MethodAccess> {
        private final MethodMatcher methodMatcher;

        public Matcher(String methodPattern) {
            this(new MethodMatcher(methodPattern));
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction3<MethodAccess, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, getCursor(), p) :
                            super.visitMethodInvocation(method, p);
                }

                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, getCursor(), p) :
                            super.visitMethodDeclaration(method, p);
                }

                @Override
                public J visitNewClass(J.NewClass newClass, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, getCursor(), p) :
                            super.visitNewClass(newClass, p);
                }

                @Override
                public J visitMemberReference(J.MemberReference memberRef, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, getCursor(), p) :
                            super.visitMemberReference(memberRef, p);
                }
            };
        }

        @Override
        protected @Nullable MethodAccess test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof J.MethodInvocation ||
                value instanceof J.NewClass ||
                value instanceof J.MemberReference) {
                return methodMatcher.matches(((Expression) value)) ?
                        new MethodAccess(cursor) :
                        null;
            }
            if (value instanceof J.MethodDeclaration) {
                J newClassOrClassDecl = cursor
                        .dropParentUntil(t -> t instanceof J.ClassDeclaration ||
                                              t instanceof J.NewClass)
                        .getValue();
                if (newClassOrClassDecl instanceof J.ClassDeclaration) {
                    return methodMatcher.matches((J.MethodDeclaration) value,
                            (J.ClassDeclaration) newClassOrClassDecl) ?
                            new MethodAccess(cursor) :
                            null;
                } else if (newClassOrClassDecl instanceof J.NewClass) {
                    return methodMatcher.matches((J.MethodDeclaration) value,
                            (J.NewClass) newClassOrClassDecl) ?
                            new MethodAccess(cursor) :
                            null;
                }
            }
            return null;
        }
    }
}
