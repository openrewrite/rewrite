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
package org.openrewrite.java.recipes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseTreeRandomId extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `Tree.randomId()` in LST constructors";
    }

    @Override
    public String getDescription() {
        return "Replaces occurrences of `UUID.randomUUID()` with `Tree.randomId()` when passed as an argument to a constructor call for LST elements.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final TypeMatcher treeMatcher = new TypeMatcher("org.openrewrite.Tree", true);
        final MethodMatcher randomUUIDMatcher = new MethodMatcher("java.util.UUID randomUUID()");
        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("org.openrewrite.Tree", true),
                        new UsesMethod<>(randomUUIDMatcher)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = super.visitNewClass(newClass, ctx);
                        if (!treeMatcher.matches(n.getType())) {
                            return n;
                        }
                        return n.withArguments(ListUtils.mapFirst(n.getArguments(), this::maybeReplace));
                    }

                    private Expression maybeReplace(Expression a) {
                        if (!randomUUIDMatcher.matches((J.MethodInvocation) a)) {
                            return a;
                        }
                        maybeAddImport("org.openrewrite.Tree");
                        maybeRemoveImport("java.util.UUID");

                        J.MethodInvocation m = (J.MethodInvocation) a;
                        JavaType.Class classType = JavaType.ShallowClass.build("org.openrewrite.Tree");
                        JavaType.Method methodType = m.getMethodType().withName("randomId").withDeclaringType(classType);
                        m = m.withName(m.getName().withSimpleName("randomId").withType(methodType));
                        if (m.getSelect() instanceof J.Identifier) {
                            return m.withSelect(((J.Identifier) m.getSelect()).withSimpleName("Tree").withType(classType));
                        }
                        return m.withSelect(new J.Identifier(
                                Tree.randomId(), m.getPrefix(), m.getMarkers(), Collections.emptyList(), "Tree", classType, null));
                    }
                });
    }
}
