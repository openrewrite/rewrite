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
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.openrewrite.java.tree.J;

public class RenameVariable<P> extends JavaIsoProcessor<P> {
    private final Cursor scope;
    private final String toName;

    private String scopeVariableName;

    public RenameVariable(Cursor scope, String toName) {
        this.scope = scope;
        Validated validated = Validated.test("scope", "Must be a cursor to a J.VariableDecls.NamedVar",
                scope, s -> s.getValue() instanceof J.VariableDecls.NamedVar);
        if (validated.isInvalid()) {
            throw new ValidationException(validated);
        }
        this.toName = toName;
        setCursoringOn();
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        scopeVariableName = ((J.VariableDecls.NamedVar) scope.getValue()).getSimpleName();
        return super.visitCompilationUnit(cu, p);
    }

    @Override
    public J.Ident visitIdentifier(J.Ident ident, P p) {
        if (ident.getSimpleName().equals(scopeVariableName) &&
                isInSameNameScope(scope, getCursor()) &&
                !(getCursor().getParentOrThrow().getValue() instanceof J.FieldAccess)) {
            return ident.withName(toName);
        }

        return super.visitIdentifier(ident, p);
    }
}
