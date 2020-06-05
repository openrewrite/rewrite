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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;

public class RenameVariable extends JavaRefactorVisitor {
    private final J.VariableDecls.NamedVar scope;
    private final String toName;

    private Cursor scopeCursor;
    private String scopeVariableName;

    public RenameVariable(J.VariableDecls.NamedVar scope, String toName) {
        this.scope = scope;
        this.toName = toName;
        setCursoringOn();
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("to", toName);
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        scopeCursor = new RetrieveCursor(scope).visit(cu);
        scopeVariableName = ((J.VariableDecls.NamedVar) scopeCursor.getTree()).getSimpleName();

        return super.visitCompilationUnit(cu);
    }

    @Override
    public J visitIdentifier(J.Ident ident) {
        if (ident.getSimpleName().equals(scopeVariableName) &&
                isInSameNameScope(scopeCursor, getCursor()) &&
                !(getCursor().getParentOrThrow().getTree() instanceof J.FieldAccess)) {
            return ident.withName(toName);
        }

        return super.visitIdentifier(ident);
    }
}
