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
package org.openrewrite.java.dataflow;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.20.0")
public class UpdateGuardedBy extends Recipe {
    @Override
    public String getDisplayName() {
        return "Guarded-by data flow analysis";
    }

    @Override
    public String getDescription() {
        return "Update guarded-by edges tracking places where access to a variable has been guarded by a condition or the negation of a condition.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                getCursor().dropParentUntil(J.Block.class::isInstance)
                        .computeMessageIfAbsent("variables", v -> new HashMap<String, UUID>())
                        .put(variable.getSimpleName(), variable.getId());
                return super.visitVariable(variable, ctx);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier i = super.visitIdentifier(identifier, ctx);
                i = i.withMarkers(i.getMarkers().removeByType(GuardedBy.class));

                Object parent = getCursor().dropParentUntil(t -> t instanceof J && !(t instanceof J.Parentheses)).getValue();
                if(parent instanceof J.Binary || parent instanceof J.Unary) {
                    return i;
                }

                Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");
                if (variableIds != null) {
                    UUID id = variableIds.get(identifier.getSimpleName());
                    if (id != null) {
                        GuardedBy guardedBy = guardedBy(id, null);
                        if (guardedBy != null) {
                            i = i.withMarkers(i.getMarkers().setByType(guardedBy));
                        }
                    }
                }
                return i;
            }

            @Nullable
            private GuardedBy guardedBy(UUID variableId, @Nullable J skip) {
                AtomicBoolean skipped = new AtomicBoolean(skip == null);
                J guard = (J) getCursor().getPathAsStream()
                        .filter(t -> {
                            if (!skipped.get()) {
                                if (t == skip) {
                                    skipped.set(true);
                                }
                                return false;
                            }
                            return t instanceof J.If || t instanceof J.If.Else || t instanceof J.ForLoop ||
                                    t instanceof J.WhileLoop || t instanceof J.DoWhileLoop;
                        })
                        .findFirst()
                        .orElse(null);

                if (guard != null) {
                    boolean negated = false;
                    J guardCondition = null;
                    if (guard instanceof J.If) {
                        guardCondition = ((J.If) guard).getIfCondition().getTree();
                    } else if (guard instanceof J.If.Else) {
                        guardCondition = getCursor().firstEnclosingOrThrow(J.If.class).getIfCondition().getTree();
                        negated = true;
                    } else if (guard instanceof J.ForLoop) {
                        guardCondition = ((J.ForLoop) guard).getControl().getCondition();
                    } else if (guard instanceof J.WhileLoop) {
                        guardCondition = ((J.WhileLoop) guard).getCondition().getTree();
                    } else if (guard instanceof J.DoWhileLoop) {
                        guardCondition = ((J.DoWhileLoop) guard).getWhileCondition().getTree();
                    }

                    if (!(guardCondition instanceof J.Empty)) {
                        assert guardCondition != null;

                        Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");

                        AtomicBoolean conditionUsesVariable = new AtomicBoolean(false);
                        new JavaIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");
                                assert variableIds != null;
                                if (variableId.equals(variableIds.get(identifier.getSimpleName()))) {
                                    conditionUsesVariable.set(true);
                                }
                                return identifier;
                            }
                        }.visit(guardCondition, 0, getCursor());

                        if(conditionUsesVariable.get()) {
                            return new GuardedBy(randomId(), guardCondition.getId(), negated);
                        }
                    } else {
                        // look for a guard in the next conditional statement above this
                        return guardedBy(variableId, guardCondition);
                    }
                }
                return null;
            }
        };
    }
}
