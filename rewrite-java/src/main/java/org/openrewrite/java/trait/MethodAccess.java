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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.function.Predicate;

@Incubating(since = "8.30.0")
@Value
public class MethodAccess implements Trait<MethodCall> {
    Cursor cursor;

    @RequiredArgsConstructor
    public static class Matcher extends SimpleTraitMatcher<MethodAccess> {
        private final MethodMatcher methodMatcher;
        private Predicate<@Nullable JavaType> returnsTest = m -> true;

        public Matcher(String methodPattern) {
            this(new MethodMatcher(methodPattern));
        }

        public Matcher returns(Predicate<@Nullable JavaType> returnsTest) {
            this.returnsTest = returnsTest;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<MethodAccess, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, p) :
                            super.visitMethodInvocation(method, p);
                }

                @Override
                public J visitNewClass(J.NewClass newClass, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, p) :
                            super.visitNewClass(newClass, p);
                }

                @Override
                public J visitMemberReference(J.MemberReference memberRef, P p) {
                    MethodAccess methodAccess = test(getCursor());
                    return methodAccess != null ?
                            (J) visitor.visit(methodAccess, p) :
                            super.visitMemberReference(memberRef, p);
                }
            };
        }

        @Override
        protected @Nullable MethodAccess test(Cursor cursor) {
            Object value = cursor.getValue();
            JavaType.Method methodType = ((MethodCall) value).getMethodType();
            JavaType returnType = methodType == null ? null : methodType.getReturnType();

            return methodMatcher.matches(((Expression) value)) &&
                   returnsTest.test(returnType) ?
                    new MethodAccess(cursor) :
                    null;
        }
    }
}
