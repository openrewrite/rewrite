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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.dataflow.FindLocalFlowPaths;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class ReplaceStackWithDeque extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace `java.util.Stack` with `java.util.Deque`.";
    }

    @Override
    public String getDescription() {
        return "From the Javadoc of `Stack`:\n" +
                "> A more complete and consistent set of LIFO stack operations is provided by the Deque interface and its implementations, which should be used in preference to this class.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.util.Stack");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);

                LocalFlowSpec<Expression, J> returned = new LocalFlowSpec<Expression, J>() {
                    @Override
                    public boolean isSource(Expression expression, Cursor cursor) {
                        return variable.getInitializer() == expression;
                    }

                    @Override
                    public boolean isSink(J j, Cursor cursor) {
                        return cursor.firstEnclosing(J.Return.class) != null;
                    }
                };

                if (v.getInitializer() != null && FindLocalFlowPaths.noneMatch(getCursor(), returned)) {
                    v = (J.VariableDeclarations.NamedVariable) new ChangeType("java.util.Stack", "java.util.ArrayDeque", false)
                            .getVisitor().visitNonNull(v, ctx, getCursor().getParentOrThrow());
                    getCursor().putMessageOnFirstEnclosing(J.VariableDeclarations.class, "replace", true);
                }

                return v;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if(getCursor().getMessage("replace", false)) {
                    v = (J.VariableDeclarations) new ChangeType("java.util.Stack", "java.util.Deque", false)
                            .getVisitor().visitNonNull(v, ctx, getCursor().getParentOrThrow());
                    maybeAddImport("java.util.ArrayDeque");
                    maybeAddImport("java.util.Deque");
                }
                return v;
            }
        };
    }
}
