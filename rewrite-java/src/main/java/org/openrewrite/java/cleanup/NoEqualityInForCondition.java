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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class NoEqualityInForCondition extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use comparison rather than equality checks in for conditions";
    }

    @Override
    public String getDescription() {
        return "Testing for loop termination using an equality operator (`==` and `!=`) is dangerous, because it could set up an infinite loop. Using a relational operator instead makes it harder to accidentally write an infinite loop.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-888");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitForControl(J.ForLoop.Control control, ExecutionContext ctx) {
                if (control.getCondition() instanceof J.Binary) {
                    J.Binary condition = (J.Binary) control.getCondition();
                    if (condition.getRight() instanceof J.Literal && condition.getRight().getType() == JavaType.Primitive.Null) {
                        return super.visitForControl(control, ctx);
                    }

                    if (control.getUpdate().size() == 1 && control.getUpdate().get(0) instanceof J.Unary) {
                        J.Unary update = (J.Unary) control.getUpdate().get(0);

                        if (condition.getOperator() == J.Binary.Type.NotEqual) {
                            switch (update.getOperator()) {
                                case PreIncrement:
                                case PostIncrement:
                                    return control.withCondition(condition.withOperator(J.Binary.Type.LessThan));
                                case PreDecrement:
                                case PostDecrement:
                                    return control.withCondition(condition.withOperator(J.Binary.Type.GreaterThan));
                            }
                        }
                    }
                }

                return super.visitForControl(control, ctx);
            }
        };
    }
}
