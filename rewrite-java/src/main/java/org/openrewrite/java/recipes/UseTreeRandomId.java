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

                    private Expression maybeReplace(Expression expression) {
                        if (!randomUUIDMatcher.matches(expression)) {
                            return expression;
                        }
                        maybeAddImport("org.openrewrite.Tree");
                        maybeRemoveImport("java.util.UUID");

                        J.MethodInvocation mi = (J.MethodInvocation) expression;
                        JavaType.Class classType = JavaType.ShallowClass.build("org.openrewrite.Tree");
                        JavaType.Method methodType = mi.getMethodType().withName("randomId").withDeclaringType(classType);
                        mi = mi.withName(mi.getName().withSimpleName("randomId").withType(methodType));
                        if (mi.getSelect() instanceof J.Identifier) {
                            return mi.withSelect(((J.Identifier) mi.getSelect()).withSimpleName("Tree").withType(classType));
                        }
                        return mi.withSelect(new J.Identifier(
                                Tree.randomId(), mi.getPrefix(), mi.getMarkers(), Collections.emptyList(), "Tree", classType, null));
                    }
                });
    }
}
