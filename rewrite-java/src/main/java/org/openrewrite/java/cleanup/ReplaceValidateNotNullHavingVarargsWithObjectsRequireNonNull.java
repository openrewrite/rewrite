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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplaceValidateNotNullHavingVarargsWithObjectsRequireNonNull extends Recipe {
    private static final MethodMatcher VALIDATE_NOTNULL = new MethodMatcher("org.apache.commons.lang3.Validate notNull(Object, String, Object[])");

    @Override
    public String getDisplayName() {
        return "Replace `org.apache.commons.lang3.Validate#notNull` with `Objects#requireNonNull`";
    }

    @Override
    public String getDescription() {
        return "Replace `org.apache.commons.lang3.Validate.notNull(Object, String, Object[])` with `Objects.requireNonNull(Object, String)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(VALIDATE_NOTNULL), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);
                if (!VALIDATE_NOTNULL.matches(mi)) {
                    return mi;
                }

                List<Expression> arguments = mi.getArguments();
                String template = arguments.size() == 2
                        ? "Objects.requireNonNull(#{any()}, #{any(java.lang.String)})"
                        : String.format("Objects.requireNonNull(#{any()}, () -> String.format(#{any(java.lang.String)}, %s))",
                        String.join(", ", Collections.nCopies(arguments.size() - 2, "#{any()}")));


                maybeRemoveImport("org.apache.commons.lang3.Validate");
                maybeAddImport("java.util.Objects");

                mi = mi.withTemplate(
                        JavaTemplate.builder(this::getCursor, template)
                                .imports("java.util.Objects")
                                .build(),
                        mi.getCoordinates().replace(),
                        arguments.toArray());

                if (arguments.size() == 2) {
                    return maybeAutoFormat(mi, mi.withArguments(
                            ListUtils.map(mi.getArguments(), (a, b) -> b.withPrefix(arguments.get(a).getPrefix()))), p);
                }

                // Retain comments and whitespace around lambda arguments
                Expression arg0 = arguments.get(0);
                arguments.remove(0);
                J.Lambda lambda = (J.Lambda) mi.getArguments().get(1);
                J.MethodInvocation stringFormatMi = (J.MethodInvocation) lambda.getBody();

                stringFormatMi = stringFormatMi.withArguments(
                        ListUtils.map(stringFormatMi.getArguments(), (a, b) -> b.withPrefix(arguments.get(a).getPrefix())));

                lambda = maybeAutoFormat(lambda, lambda.withBody(stringFormatMi), p);
                return maybeAutoFormat(mi, mi.withArguments(Stream.of(arg0, lambda).collect(Collectors.toList())), p);
            }
        });
    }
}
