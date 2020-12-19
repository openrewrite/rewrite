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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.DeclarationOrderStyle;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Optional;

public class OrderDeclarations extends JavaIsoRefactorVisitor {
    @Nullable
    private DeclarationOrderStyle.Layout layout;

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
        andThen(new Scoped(classDecl, layout));
        return classDecl; // don't recurse intentionally
    }

    public void setLayout(@Nullable DeclarationOrderStyle.Layout layout) {
        this.layout = layout;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.ClassDecl scope;

        @Nullable
        private DeclarationOrderStyle.Layout layout;

        public Scoped(J.ClassDecl scope, @Nullable DeclarationOrderStyle.Layout layout) {
            this.scope = scope;
            this.layout = layout;
            setCursoringOn();
        }

        public Scoped(J.ClassDecl scope) {
            this(scope, null);
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
            if (layout == null) {
                // infer from compilation unit or default
                this.layout = Optional.ofNullable(cu.getStyle(DeclarationOrderStyle.class))
                        .map(style -> this.layout = style.getLayout())
                        .orElse(DeclarationOrderStyle.Layout.DEFAULT);
            }
            return super.visitCompilationUnit(cu);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = super.visitClassDecl(classDecl);

            if (getCursor().isScopeInPath(scope)) {
                assert layout != null;

                layout.reset();
                c.getBody().getStatements().forEach(layout::accept);

                List<J> orderedDeclarations = layout.orderedDeclarations();

                for (int i = 0, orderedDeclarationsSize = orderedDeclarations.size(); i < orderedDeclarationsSize; i++) {
                    J orderedDeclaration = orderedDeclarations.get(i);

                    if(orderedDeclaration instanceof J.MethodDecl) {
                        orderedDeclarations.set(i, orderedDeclaration.withFormatting(orderedDeclaration.getFormatting().withMinimumBlankLines(1)));
                    }
                }

                andThen(new AutoFormat(orderedDeclarations.toArray(new J[0])));

                c = c.withBody(c.getBody().withStatements(orderedDeclarations));
            }

            return c;
        }
    }
}
