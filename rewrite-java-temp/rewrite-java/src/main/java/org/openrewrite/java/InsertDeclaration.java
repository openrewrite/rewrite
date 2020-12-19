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

import java.util.*;

public class InsertDeclaration {
    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.ClassDecl enclosing;
        private final J declaration;
        private DeclarationOrderStyle.Layout layout;

        public Scoped(J.ClassDecl enclosing, J declaration) {
            this.enclosing = enclosing;
            this.declaration = declaration;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
            this.layout = Optional.ofNullable(cu.getStyle(DeclarationOrderStyle.class))
                    .map(DeclarationOrderStyle::getLayout)
                    .orElse(DeclarationOrderStyle.Layout.DEFAULT);

            return super.visitCompilationUnit(cu);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = super.visitClassDecl(classDecl);

            if (c.isScope(enclosing)) {
                if (c.getBody().getStatements().stream().anyMatch(s -> s.isScope(declaration))) {
                    return c;
                }

                List<J> declarations = new ArrayList<>(c.getBody().getStatements());
                declarations.add(declaration);
                layout.reset();
                declarations.forEach(layout::accept);

                Set<J> before = new HashSet<>();
                J formattedDeclaration = declaration;

                List<J> orderedDeclarations = layout.orderedDeclarations();
                for (J orderedDeclaration : orderedDeclarations) {
                    if (orderedDeclaration.isScope(declaration)) {
                        formattedDeclaration = orderedDeclaration;
                        break;
                    }
                    before.add(orderedDeclaration);
                }

                List<J> declarationsWithInsert = new ArrayList<>();
                if (before.isEmpty()) {
                    declarationsWithInsert.add(formattedDeclaration);
                }

                for (J statement : c.getBody().getStatements()) {
                    int size = before.size();
                    before.remove(statement);
                    declarationsWithInsert.add(statement);
                    if(size == 1) {
                        declarationsWithInsert.add(formattedDeclaration);
                    }
                }

                c = c.withBody(c.getBody().withStatements(declarationsWithInsert));
                andThen(new AutoFormat(formattedDeclaration));
            }

            return c;
        }
    }
}
