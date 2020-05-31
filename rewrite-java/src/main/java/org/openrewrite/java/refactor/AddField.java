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
package org.openrewrite.java.refactor;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class AddField extends JavaRefactorVisitor {
    private final J.ClassDecl scope;
    private final List<J.Modifier> modifiers;
    private final String clazz;
    private final String name;

    @Nullable
    private final String init;

    public AddField(J.ClassDecl scope, List<J.Modifier> modifiers, String clazz, String name, @Nullable String init) {
        super("java.AddField", "field.class", clazz, "field.name", name);
        this.scope = scope;
        this.modifiers = modifiers;
        this.clazz = clazz;
        this.name = name;
        this.init = init;
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        J.ClassDecl c = refactor(classDecl, super::visitClassDecl);

        if (scope.isScope(classDecl) && classDecl.getBody().getStatements().stream()
                .filter(s -> s instanceof J.VariableDecls)
                .map(J.VariableDecls.class::cast)
                .noneMatch(mv -> mv.getVars().stream().anyMatch(var -> var.getSimpleName().equals(name)))) {
            var body = c.getBody();
            var classType = JavaType.Class.build(clazz);

            maybeAddImport(classType);

            var newField = new J.VariableDecls(randomId(),
                    emptyList(),
                    modifiers,
                    J.Ident.build(randomId(), classType.getClassName(), classType, modifiers.isEmpty() ? EMPTY : format(" ")),
                    null,
                    emptyList(),
                    singletonList(new J.VariableDecls.NamedVar(randomId(),
                            J.Ident.build(randomId(), name, null, format("", init == null ? "" : " ")),
                            emptyList(),
                            init == null ? null : new J.UnparsedSource(randomId(), init, format(" ")),
                            classType,
                            format(" ")
                    )),
                    formatter.format(body)
            );

            List<J> statements = new ArrayList<>(body.getStatements().size() + 1);
            statements.add(newField);
            statements.addAll(body.getStatements());

            c = c.withBody(body.withStatements(statements));
        }

        return c;
    }
}
