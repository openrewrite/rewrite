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

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;

public class RenameVariable<P> extends JavaIsoVisitor<P> {
    private final J.VariableDeclarations.NamedVariable variable;
    private final String toName;

    public RenameVariable(J.VariableDeclarations.NamedVariable variable, String toName) {
        this.variable = variable;
        this.toName = toName;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        if (variable.equals(this.variable)) {
            doAfterVisit(new RenameVariableByCursor(getCursor()));
            return variable;
        }
        return super.visitVariable(variable, p);
    }

    private class RenameVariableByCursor extends JavaIsoVisitor<P> {
        private final Cursor scope;

        public RenameVariableByCursor(Cursor scope) {
            this.scope = scope;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
            return super.visitCompilationUnit(cu, p);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, P p) {
            if (ident.getSimpleName().equals(variable.getSimpleName()) &&
                    isInSameNameScope(scope, getCursor()) &&
                    !(getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.FieldAccess)) {
                return ident.withName(toName);
            }
            return super.visitIdentifier(ident, p);
        }
    }
}
