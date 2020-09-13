/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.java.style.DeclarationOrderStyle;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

public class InsertDeclaration {
    public static class Scoped extends JavaRefactorVisitor {
        private final J.ClassDecl enclosing;
        private final J declaration;
        private DeclarationOrderStyle.Layout layout;

        public Scoped(J.ClassDecl enclosing, J declaration) {
            this.enclosing = enclosing;
            this.declaration = declaration;
        }

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu) {
            this.layout = cu.getStyle(DeclarationOrderStyle.class)
                    .map(DeclarationOrderStyle::getLayout)
                    .orElse(DeclarationOrderStyle.Layout.DEFAULT);

            return super.visitCompilationUnit(cu);
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = refactor(classDecl, super::visitClassDecl);

            if (c.isScope(enclosing)) {
                if (c.getBody().getStatements().stream().anyMatch(s -> s.isScope(declaration))) {
                    return c;
                }

                List<J> declarations = new ArrayList<>(c.getBody().getStatements());
                declarations.add(declaration);
                layout.reset();
                declarations.forEach(layout::accept);

                J firstBefore = null;
                J firstAfter = null;
                J formattedDeclaration = declaration;

                List<J> orderedDeclarations = layout.orderedDeclarations();
                for (int i = 0; i < orderedDeclarations.size(); i++) {
                    if (orderedDeclarations.get(i).isScope(declaration)) {
                        firstBefore = i > 0 ? orderedDeclarations.get(i - 1) : null;
                        firstAfter = i < orderedDeclarations.size() - 1 ? orderedDeclarations.get(i + 1) : null;
                        formattedDeclaration = orderedDeclarations.get(i);
                        break;
                    }
                }

                List<J> declarationsWithInsert = new ArrayList<>();
                if (firstBefore == null) {
                    declarationsWithInsert.add(formattedDeclaration);
                }

                for (J statement : c.getBody().getStatements()) {
                    if (statement.isScope(firstAfter)) {
                        declarationsWithInsert.add(firstAfter);
                    } else {
                        declarationsWithInsert.add(statement);
                        if (statement.isScope(firstBefore)) {
                            declarationsWithInsert.add(formattedDeclaration);
                        }
                    }
                }

                c = c.withBody(c.getBody().withStatements(declarationsWithInsert));
                andThen(new AutoFormat(formattedDeclaration));
            }

            return c;
        }
    }
}
