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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class WhileInsteadOfFor extends Recipe {

    @Override
    public String getDisplayName() {
        return "Prefer `while` over `for` loops";
    }

    @Override
    public String getDescription() {
        return "When only the condition expression is defined in a for loop, and the initialization and increment expressions are missing, a while loop should be used instead to increase readability.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1264");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            final JavaTemplate whileLoop = JavaTemplate.builder(this::getCursor,
                    "while(#{any(boolean)}) {}")
                    .build();

            @Override
            public J visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                if (forLoop.getControl().getInit().get(0) instanceof J.Empty &&
                    forLoop.getControl().getUpdate().get(0) instanceof J.Empty &&
                    !(forLoop.getControl().getCondition() instanceof J.Empty)
                ) {
                    J.WhileLoop w = forLoop.withTemplate(whileLoop, forLoop.getCoordinates().replace(),
                        forLoop.getControl().getCondition());
                    w = w.withBody(forLoop.getBody());
                    return w;
                }
                return super.visitForLoop(forLoop, ctx);
            }
        };
    }
}
