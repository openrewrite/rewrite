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

import org.openrewrite.Cursor;
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
public class UpdateLastRead extends Recipe {
    @Override
    public String getDisplayName() {
        return "Last read data flow analysis";
    }

    @Override
    public String getDescription() {
        return "Update last read edges tracking uses of local variables and fields.";
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
                i = i.withMarkers(i.getMarkers().removeByType(LastRead.class));
                Map<String, UUID> variableIds = getCursor().getNearestMessage("variables");
                if (variableIds != null) {
                    UUID id = variableIds.get(identifier.getSimpleName());
                    if (id != null) {
                        Cursor parent = getCursor().dropParentUntil(t -> t instanceof J && !(t instanceof J.Parentheses));
                        if (cursorIsInstanceOf(parent, J.Unary.class, J.Binary.class, J.If.class, J.Synchronized.class,
                                J.ControlParentheses.class, J.ForLoop.Control.class, J.ForEachLoop.Control.class,
                                J.WhileLoop.class, J.DoWhileLoop.class, J.InstanceOf.class, J.AssignmentOperation.class)) {
                            i = i.withMarkers(i.getMarkers().setByType(new LastRead(randomId(), id)));
                        } else {
                            Cursor grandparent = parent.dropParentUntil(t -> t instanceof J && !(t instanceof J.Parentheses));
                            if (cursorIsInstanceOf(grandparent, J.Binary.class)) {
                                i = i.withMarkers(i.getMarkers().setByType(new LastRead(randomId(), id)));
                            }
                        }
                    }
                }
                return i;
            }

            @SafeVarargs
            private final boolean cursorIsInstanceOf(Cursor cursor, Class<? extends J>... types) {
                for (Class<? extends J> type : types) {
                    if (type.isInstance(cursor.getValue())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
