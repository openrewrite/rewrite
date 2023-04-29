/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Iterator;

@Incubating(since = "7.0.0")
public class NoFinalizedLocalVariables extends Recipe {

    @Override
    public String getDisplayName() {
        return "Don't use final on local variables";
    }

    @Override
    public String getDescription() {
        return "Remove the `final` modifier keyword from local variables regardless of whether they are used within a local class or an anonymous class.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);

                // if this doesn't have "final", we don't need to bother going any further; we're done
                if (!mv.hasModifier(J.Modifier.Type.Final)) {
                    return mv;
                }

                Tree parent = getCursor().getParentTreeCursor().getValue();
                if (parent instanceof J.MethodDeclaration || parent instanceof J.Lambda) {
                    // this variable is a method parameter or lambda parameter
                    return removeFinal(mv);
                }

                Iterator<Object> cursorPath = getCursor().getPath();
                while (cursorPath.hasNext()) {
                    Object next = cursorPath.next();
                    if (next instanceof J.Block) {
                        while (cursorPath.hasNext()) {
                            next = cursorPath.next();
                            if (next instanceof J.ClassDeclaration || next instanceof J.NewClass) {
                                // this variable is a field
                                return mv;
                            } else if (next instanceof J.MethodDeclaration) {
                                return removeFinal(mv);
                            }
                        }
                        break;
                    }
                }

                return mv;
            }

            private J.VariableDeclarations removeFinal(J.VariableDeclarations mv) {
                J.VariableDeclarations v = mv.withModifiers(ListUtils.map(mv.getModifiers(),
                        m -> m.getType() == J.Modifier.Type.Final ? null : m));
                if (v.getModifiers().isEmpty() && v.getTypeExpression() != null) {
                    v = v.withTypeExpression(v.getTypeExpression().withPrefix(v.getTypeExpression().getPrefix()
                            .withWhitespace("")));
                }
                return v;
            }
        };
    }
}
