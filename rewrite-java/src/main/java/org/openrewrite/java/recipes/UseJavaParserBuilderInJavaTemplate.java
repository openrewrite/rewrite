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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class UseJavaParserBuilderInJavaTemplate extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `JavaParser.Builder` when constructing `JavaTemplate`";
    }

    @Override
    public String getDescription() {
        return "Because we can now clone `JavaParser.Builder`, there is no need to fully build the parser inside a `Supplier<JavaParser>`. " +
               "This also makes room for `JavaTemplate` to add shared `JavaTypeCache` implementations to parsers used to compile templates.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher SUPPLIER_BASED_JAVA_PARSER = new MethodMatcher(
                "org.openrewrite.java.JavaTemplate$Builder javaParser(java.util.function.Supplier)");
        MethodMatcher JAVA_PARSER_BUILD = new MethodMatcher(
                "org.openrewrite.java.JavaParser$Builder build()");
        return Preconditions.check(new UsesMethod<>(SUPPLIER_BASED_JAVA_PARSER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (SUPPLIER_BASED_JAVA_PARSER.matches(m)) {
                    AtomicReference<J.MethodInvocation> builder = new AtomicReference<>();
                    new JavaIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                            if (JAVA_PARSER_BUILD.matches(method)) {
                                builder.set(requireNonNull(method.getSelect()).withPrefix(Space.EMPTY));
                            }
                            return super.visitMethodInvocation(method, p);
                        }
                    }.visit(m.getArguments().get(0), 0);
                    if (builder.get() != null) {
                        m = m.withArguments(Collections.singletonList(builder.get()));
                    }
                }
                return m;
            }
        });
    }
}
