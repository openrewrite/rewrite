/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.visitor.refactor.op;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.Formatting;
import org.openrewrite.tree.J;
import org.openrewrite.tree.Tree;
import org.openrewrite.tree.Type;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.visitor.refactor.AstTransform;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.openrewrite.tree.J.randomId;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class AddField extends ScopedRefactorVisitor {
    private final List<J.Modifier> modifiers;
    private final String clazz;
    private final String name;

    @Nullable
    private final String init;

    public AddField(UUID scope, List<J.Modifier> modifiers, String clazz, String name, @Nullable String init) {
        super(scope);
        this.modifiers = modifiers;
        this.clazz = clazz;
        this.name = name;
        this.init = init;
    }

    @Override
    public String getRuleName() {
        return MessageFormatter.arrayFormat("core.AddField{classType={},name={}}",
                new String[]{clazz, name}).toString();
    }

    @Override
    public List<AstTransform> visitClassDecl(J.ClassDecl classDecl) {
        return maybeTransform(classDecl,
                classDecl.getId().equals(scope) && classDecl.getBody().getStatements()
                        .stream()
                        .filter(s -> s instanceof J.VariableDecls)
                        .map(J.VariableDecls.class::cast)
                        .noneMatch(mv -> mv.getVars().stream().anyMatch(var -> var.getSimpleName().equals(name))),
                super::visitClassDecl,
                J.ClassDecl::getBody,
                block -> {
                    var classType = Type.Class.build(clazz);
                    var newField = new J.VariableDecls(randomId(),
                            emptyList(),
                            modifiers,
                            J.Ident.build(randomId(), classType.getClassName(), classType, Formatting.EMPTY),
                            null,
                            emptyList(),
                            singletonList(new J.VariableDecls.NamedVar(randomId(),
                                    J.Ident.build(randomId(), name, null, Formatting.format("", init == null ? "" : " ")),
                                    emptyList(),
                                    init == null ? null : new J.UnparsedSource(randomId(), init, Formatting.format(" ")),
                                    classType,
                                    Formatting.format(" ")
                            )),
                            formatter().format(block)
                    );

                    List<Tree> statements = new ArrayList<>(block.getStatements().size() + 1);
                    statements.add(newField);
                    statements.addAll(block.getStatements());
                    return block.withStatements(statements);
                }
        );
    }
}
