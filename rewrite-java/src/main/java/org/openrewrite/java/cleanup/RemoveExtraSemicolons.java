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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class RemoveExtraSemicolons extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove extra semicolons";
    }

    @Override
    public String getDescription() {
        return "Optional semicolons at the end of try-with-resources are also removed.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("RSPEC-1116", "RSPEC-2959"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Empty visitEmpty(J.Empty empty, ExecutionContext ctx) {
                if (getCursor().getParentTreeCursor().getValue() instanceof J.Block) {
                    return null;
                }
                return empty;
            }

            @Override
            public J.Try.Resource visitTryResource(J.Try.Resource tr, ExecutionContext executionContext) {
                J.Try _try = getCursor().dropParentUntil(is -> is instanceof J.Try).getValue();
                if (_try.getResources().isEmpty() ||
                    _try.getResources().get(_try.getResources().size() - 1) != tr ||
                    !_try.getResources().get(_try.getResources().size() - 1).isTerminatedWithSemicolon()) {
                    return tr;
                }
                return tr.withTerminatedWithSemicolon(false);
            }

            @Override
            public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums, ExecutionContext executionContext) {
                J.EnumValueSet e = super.visitEnumValueSet(enums, executionContext);
                if (getCursor().firstEnclosing(J.Block.class).getStatements().size() == 1) {
                    e = e.withTerminatedWithSemicolon(false);
                }
                return e;
            }
        };
    }
}
