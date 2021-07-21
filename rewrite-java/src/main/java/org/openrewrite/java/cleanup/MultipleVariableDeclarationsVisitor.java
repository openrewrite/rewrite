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
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class MultipleVariableDeclarationsVisitor extends JavaIsoVisitor<ExecutionContext> {
    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
        J.Block b = super.visitBlock(block, ctx);
        boolean splitAtLeastOneVariable = false;
        List<Statement> statements = new ArrayList<>();
        for (Statement stmt : b.getStatements()) {
            if (stmt instanceof J.VariableDeclarations) {
                J.VariableDeclarations mv = (J.VariableDeclarations) stmt;
                if (mv.getVariables().size() > 1) {
                    splitAtLeastOneVariable = true;
                    for (int i = 0; i < mv.getVariables().size(); i++) {
                        J.VariableDeclarations.NamedVariable nv = mv.getVariables().get(i);
                        List<JLeftPadded<Space>> dimensions = ListUtils.concatAll(mv.getDimensionsBeforeName(), nv.getDimensionsAfterName());
                        nv = nv.withDimensionsAfterName(emptyList());
                        J.VariableDeclarations vd = new J.VariableDeclarations(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                mv.getLeadingAnnotations(),
                                mv.getModifiers(),
                                mv.getTypeExpression(),
                                mv.getVarargs(),
                                dimensions,
                                Collections.singletonList(JRightPadded.build(nv))
                        );
                        if (i == 0) {
                            vd = vd.withComments(mv.getComments()).withPrefix(mv.getPrefix());
                        }
                        vd = autoFormat(vd, ctx);
                        statements.add(vd);
                    }
                } else {
                    statements.add(stmt);
                }
            } else {
                statements.add(stmt);
            }
        }
        return splitAtLeastOneVariable ? b.withStatements(statements) : b;
    }
}
