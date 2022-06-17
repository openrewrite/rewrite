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
package org.openrewrite.java.controlflow;

import lombok.AllArgsConstructor;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Set;
import java.util.stream.Collectors;

@Incubating(since = "7.25.0")
@AllArgsConstructor
public class ControlFlowBasicBlockVisitor<P> extends JavaIsoVisitor<P> {
    private final String methodName;

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.MethodDeclaration methodDeclaration = getCursor().firstEnclosing(J.MethodDeclaration.class);
        if (block == methodDeclaration.getBody() &&
                methodDeclaration.getName().getSimpleName().equals(methodName)) {
            ControlFlowSummary summary = ControlFlow.startingAt(getCursor()).findControlFlow();
            Set<J> inBasicBlock = summary.getBasicBlocks().stream().flatMap(b ->
                    b.getNodeValues().stream()).collect(Collectors.toSet());
            doAfterVisit(new JavaIsoVisitor<P>() {

                @Override
                public Statement visitStatement(Statement statement, P p) {
                    return inBasicBlock.contains(statement) ?
                            statement.withMarkers(statement.getMarkers().searchResult()) :
                            statement;
                }

                @Override
                public Expression visitExpression(Expression expression, P p) {
                    return inBasicBlock.contains(expression) ?
                            expression.withMarkers(expression.getMarkers().searchResult()) :
                            expression;
                }
            });
        }
        return super.visitBlock(block, p);
    }
}
