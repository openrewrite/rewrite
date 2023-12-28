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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.internal.lang.NonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class ReplaceUUIDRandomId extends Recipe {
    /**
     * A method pattern that is used to find matching LST constructor invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "LST Constructor pattern",
            description = "A method pattern that is used to find matching LST constructor invocations.",
            example = "org.openrewrite.java.tree.J.Literal <constructor>(..)")
    String constructorPattern;

    @Override
    public String getDisplayName() {
        return "Replace `UUID.randomUUID()` with `Tree.randomId()` in LST constructor call";
    }

    @Override
    public String getDescription() {
        return "Replaces occurrences of `UUID.randomUUID()` with `Tree.randomId()` when passed as an argument inside a constructor call for an LST class.";
    }

    @Override
    public @NotNull TreeVisitor<J, ExecutionContext> getVisitor() {
        MethodMatcher lstConstructorMatcher = new MethodMatcher(constructorPattern);
        return new ReplaceUUIDRandomIdVisitor(lstConstructorMatcher);
    }

    private static class ReplaceUUIDRandomIdVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public ReplaceUUIDRandomIdVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            if (methodMatcher.matches(n)) {
                List<Expression> arguments = n.getArguments();
                if (!arguments.isEmpty()) {
                    Expression firstArgument = arguments.get(0);
                    if (firstArgument instanceof J.MethodInvocation &&
                            ((J.MethodInvocation) firstArgument).getSelect() instanceof J.Identifier &&
                            "UUID".equals(((J.Identifier) ((J.MethodInvocation) firstArgument).getSelect()).getSimpleName()) &&
                            "randomUUID".equals(((J.MethodInvocation) firstArgument).getSimpleName())) {

                        Expression replacement = JavaTemplate.builder("Tree.randomId()")
                                .imports("org.openrewrite.java.tree.Tree")
                                .build()
                                .matcher(new Cursor(getCursor(), arguments.get(0)))
                                .find()
                                ? (Expression) JavaTemplate.builder("ListUtils.mapFirst(arguments, __ -> Tree.randomId())")
                                .imports("org.openrewrite.java.tree.Tree", "org.openrewrite.java.tree.ListUtils")
                                .build()
                                : null;

                        n = n.withArguments(ListUtils.mapFirst(arguments, a -> replacement));
                    }
                }
            }
            return n;
        }
    }
}
