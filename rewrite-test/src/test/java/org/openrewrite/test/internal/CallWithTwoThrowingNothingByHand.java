/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test.internal;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NullMarked;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
public class CallWithTwoThrowingNothingByHand extends Recipe {

    String displayName = "Call with `2`, throwing nothing";

    String description = "Replaces the `1` that a call passes by `2`, and leaves out the exceptions the called method throws.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (m.getMethodType() == null || !(m.getArguments().get(0) instanceof J.Literal) ||
                    !Integer.valueOf(1).equals(((J.Literal) m.getArguments().get(0)).getValue())) {
                    return m;
                }
                J.Literal one = (J.Literal) m.getArguments().get(0);
                JavaType.Method throwingNothing = m.getMethodType().withThrownExceptions(List.of());
                return m.withArguments(List.of(one.withValue(2).withValueSource("2")))
                  .withMethodType(throwingNothing)
                  .withName(m.getName().withType(throwingNothing));
            }
        };
    }
}
