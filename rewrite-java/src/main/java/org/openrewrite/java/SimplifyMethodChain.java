/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
public class SimplifyMethodChain extends Recipe {
    @Option(displayName = "Method pattern chain",
            description = "A list of method patterns that are called in sequence")
    List<String> methodPatternChain;

    @Option(displayName = "New method name",
            description = "The method name that will replace the existing name. The new method name target is assumed to have the same arguments as the last method in the chain.")
    String newMethodName;

    @Override
    public String getDisplayName() {
        return "Simplify a call chain";
    }

    @Override
    public String getDescription() {
        return "Simplify `a.b().c()` to `a.d()`.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("methodPatternChain",
                "Requires more than one pattern",
                methodPatternChain, c -> c.size() > 1));
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        List<MethodMatcher> matchers = methodPatternChain.stream()
                .map(MethodMatcher::new)
                .collect(Collectors.toList());
        Collections.reverse(matchers);

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                Expression select = m;
                for (MethodMatcher matcher : matchers) {
                    if (!(select instanceof J.MethodInvocation) || !matcher.matches((J.MethodInvocation) select)) {
                        return m;
                    }
                    select = m.getSelect();
                }

                if (select instanceof J.MethodInvocation) {
                    assert m.getType() != null;
                    return m.withSelect(((J.MethodInvocation) select).getSelect())
                            .withName(m.getName().withName(newMethodName))
                            .withType(m.getType().withName(newMethodName));
                }

                return m;
            }
        };
    }
}
