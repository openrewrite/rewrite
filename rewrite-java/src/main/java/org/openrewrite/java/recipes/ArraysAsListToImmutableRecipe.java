/*
 * Copyright 2023 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

/**
 * This recipe modifies the invocation of a method so that it returns an unmodifiable list, if the
 * method returns a modifiable one.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ArraysAsListToImmutableRecipe extends Recipe {

    @JsonCreator
    public ArraysAsListToImmutableRecipe() {
    }

    @Override
    public String getDisplayName() {
        return "Wrap list to be unmodifiable";
    }

    @Override
    public String getDescription() {
        return "Return an unmodifiable list, if the method returns a modifiable list.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UnmodifiableListVisitor();
    }

    private static class UnmodifiableListVisitor extends JavaIsoVisitor<ExecutionContext> {

        final MethodMatcher asListMatcher = new MethodMatcher("java.util.Arrays asList(..)");
        final MethodMatcher unmodifiableListMatcher =
                new MethodMatcher("java.util.Collections unmodifiableList(..)");

        @Override
        public J.MethodInvocation visitMethodInvocation(
                final J.MethodInvocation method, final ExecutionContext executionContext) {
            J.MethodInvocation result = super.visitMethodInvocation(method, executionContext);
            if (asListMatcher.matches(method, true)) {
                final J.MethodInvocation parentInvocation =
                        getCursor().getParentOrThrow().firstEnclosing(J.MethodInvocation.class);
                if (unmodifiableListMatcher.matches(parentInvocation, true)) {
                    return super.visitMethodInvocation(method, executionContext);
                }
                result =
                        JavaTemplate.builder("Collections.unmodifiableList(#{any()})")
                                .imports("java.util.Collections", "java.util.Arrays")
                                .contextSensitive()
                                .build()
                                .apply(getCursor(), method.getCoordinates().replace(), method);
                maybeAddImport("java.util.Collections");
            }
            return result;
        }
    }
}
