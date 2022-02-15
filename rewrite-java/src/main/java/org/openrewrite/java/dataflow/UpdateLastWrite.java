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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.20.0")
public class UpdateLastWrite extends Recipe {
    @Override
    public String getDisplayName() {
        return "Last write data flow analysis";
    }

    @Override
    public String getDescription() {
        return "Update last write edges tracking assignments to local variables and fields.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                getCursor().dropParentUntil(J.Block.class::isInstance)
                        .computeMessageIfAbsent("variables", v2 -> new HashMap<String, UUID>())
                        .put(variable.getSimpleName(), variable.getId());

                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
                v = v.withMarkers(v.getMarkers().removeByType(LastWrite.class));
                if (v.getInitializer() != null) {
                    v = v.withInitializer(v.getInitializer().withMarkers(v.getInitializer().getMarkers().setByType(new LastWrite(randomId(), v.getId()))));
                }

                return v;
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment a = super.visitAssignment(assignment, ctx);
                a = a.withMarkers(a.getMarkers().removeByType(LastWrite.class));
                if (a.getVariable() instanceof J.Identifier) {
                    Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");
                    if (variableIds != null) {
                        UUID id = variableIds.get(((J.Identifier) a.getVariable()).getSimpleName());
                        if (id != null) {
                            a = a.withAssignment(a.getAssignment().withMarkers(a.getAssignment().getMarkers().setByType(new LastWrite(randomId(), id))));
                        }
                    }
                }
                return a;
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, ExecutionContext ctx) {
                J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, ctx);
                a = a.withMarkers(a.getMarkers().removeByType(LastWrite.class));
                if (a.getVariable() instanceof J.Identifier) {
                    Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");
                    if (variableIds != null) {
                        UUID id = variableIds.get(((J.Identifier) a.getVariable()).getSimpleName());
                        if (id != null) {
                            a = a.withAssignment(a.getAssignment().withMarkers(a.getAssignment().getMarkers()
                                    .setByType(new LastWrite(randomId(), id))));
                        }
                    }
                }
                return a;
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
                J.Unary u = super.visitUnary(unary, ctx);
                u = u.withMarkers(u.getMarkers().removeByType(LastWrite.class));
                if (u.getExpression() instanceof J.Identifier) {
                    Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");
                    if (variableIds != null) {
                        UUID id = variableIds.get(((J.Identifier) u.getExpression()).getSimpleName());
                        if (id != null) {
                            u = u.withMarkers(u.getMarkers().setByType(new LastWrite(randomId(), id)));
                        }
                    }
                }
                return u;
            }
        };
    }
}
