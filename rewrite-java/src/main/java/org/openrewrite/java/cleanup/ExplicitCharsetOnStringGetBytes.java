/*
 * Copyright 2022 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExplicitCharsetOnStringGetBytes extends Recipe {
    private static final MethodMatcher GET_BYTES = new MethodMatcher("java.lang.String getBytes()");

    @Option(displayName = "Default encoding",
            description = "The default encoding to supply to the `getBytes` call",
            example = "UTF_8",
            required = false)
    @Nullable
    String encoding;

    @Override
    public String getDisplayName() {
        return "Set charset encoding explicitly when calling `String#getBytes`";
    }

    @Override
    public String getDescription() {
        return "This makes the behavior of the code platform neutral. It will not override any " +
               "existing explicit encodings, even if they don't match the default encoding option.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GET_BYTES), new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate WITH_ENCODING = JavaTemplate
                    .builder(this::getCursor, "getBytes(StandardCharsets.#{})")
                    .imports("java.nio.charset.StandardCharsets")
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (GET_BYTES.matches(method)) {
                    maybeAddImport("java.nio.charset.StandardCharsets");
                    m = m.withTemplate(WITH_ENCODING, method.getCoordinates().replaceMethod(),
                            encoding == null ? "UTF_8" : encoding);
                }
                return m;
            }
        });
    }
}
