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
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultipleVariableDeclarationsVisitor extends JavaIsoVisitor<ExecutionContext> {
    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
        J.Block b = super.visitBlock(block, ctx);
        AtomicBoolean splitAtLeastOneVariable = new AtomicBoolean(false);
        List<Statement> statements = new ArrayList<>();
        for (Statement stmt : b.getStatements()) {
            if (stmt instanceof J.VariableDeclarations) {
                J.VariableDeclarations mv = (J.VariableDeclarations) stmt;
                if (mv.getVariables().size() > 1 && getCursor().getValue() instanceof J.Block) {
                    splitAtLeastOneVariable.set(true);
                    for (J.VariableDeclarations.NamedVariable nv : mv.getVariables()) {
                        List<JLeftPadded<Space>> dimensions = ListUtils.concatAll(mv.getDimensionsBeforeName(), nv.getDimensionsAfterName());
                        J.VariableDeclarations vd = mv.withId(Tree.randomId())
                                .withVariables(Collections.singletonList(nv.withDimensionsAfterName(dimensions)))
                                .withDimensionsBeforeName(Collections.emptyList());
                        statements.add(vd);
                    }
                } else {
                    statements.add(stmt);
                }
            } else {
                statements.add(stmt);
            }
        }
        return splitAtLeastOneVariable.get() ? b.withStatements(statements) : b;
    }

}
