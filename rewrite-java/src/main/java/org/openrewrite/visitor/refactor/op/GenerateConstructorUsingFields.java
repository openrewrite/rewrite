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

import org.openrewrite.tree.*;
import org.openrewrite.tree.*;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;
import org.openrewrite.visitor.refactor.AstTransform;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.openrewrite.tree.Formatting.*;
import static org.openrewrite.tree.J.randomId;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

public class GenerateConstructorUsingFields extends ScopedRefactorVisitor {
    private final List<J.VariableDecls> fields;

    public GenerateConstructorUsingFields(UUID scope, List<J.VariableDecls> fields) {
        super(scope);
        this.fields = fields;
    }

    @Override
    public List<AstTransform> visitClassDecl(J.ClassDecl classDecl) {
        return maybeTransform(classDecl,
                isScope(classDecl) && !hasRequiredArgsConstructor(classDecl),
                super::visitClassDecl,
                (cd, cursor) -> {
                    List<Tree> statements = cd.getBody().getStatements();

                    int lastField = 0;
                    for (int i = 0; i < statements.size(); i++) {
                        if (statements.get(i) instanceof J.VariableDecls) {
                            lastField = i;
                        }
                    }

                    List<Statement> constructorParams = fields.stream()
                            .map(mv -> new J.VariableDecls(randomId(),
                                    emptyList(),
                                    emptyList(),
                                    mv.getTypeExpr() != null ? mv.getTypeExpr().withFormatting(EMPTY) : null,
                                    null,
                                    formatFirstPrefix(mv.getDimensionsBeforeName(), ""),
                                    formatFirstPrefix(mv.getVars(), " "),
                                    EMPTY))
                            .collect(toList());

                    for (int i = 1; i < constructorParams.size(); i++) {
                        constructorParams.set(i, constructorParams.get(i).withFormatting(format(" ")));
                    }

                    Formatting constructorFormatting = formatter().format(cd.getBody());
                    J.MethodDecl constructor = new J.MethodDecl(randomId(), emptyList(),
                            singletonList(new J.Modifier.Public(randomId(), EMPTY)),
                            null,
                            null,
                            J.Ident.build(randomId(), cd.getSimpleName(), cd.getType(), format(" ")),
                            new J.MethodDecl.Parameters(randomId(), constructorParams, EMPTY),
                            null,
                            new J.Block<>(randomId(), null, emptyList(), format(" "),
                                    formatter().findIndent(cd.getBody().getIndent(), cd.getBody().getStatements().toArray(Tree[]::new)).getPrefix()),
                            null,
                            constructorFormatting.withPrefix("\n" + constructorFormatting.getPrefix()));

                    // add assignment statements to constructor
                    andThen(new ScopedRefactorVisitor(constructor.getId()) {
                        @Override
                        public List<AstTransform> visitMethod(J.MethodDecl method) {
                            return maybeTransform(method,
                                    isScope(method),
                                    super::visitMethod,
                                    J.MethodDecl::getBody,
                                    (body, cursor) -> body.withStatements(
                                            TreeBuilder.buildSnippet(cursor.enclosingCompilationUnit(),
                                                    cursor,
                                                    fields.stream().map(mv -> {
                                                        String name = mv.getVars().get(0).getSimpleName();
                                                        return "this." + name + " = " + name + ";";
                                                    }).collect(joining("\n", "", "\n"))
                                            ))
                            );
                        }
                    });

                    statements.add(lastField + 1, constructor);

                    return cd.withBody(cd.getBody().withStatements(statements));
                });
    }

    private boolean hasRequiredArgsConstructor(J.ClassDecl cd) {
        Set<String> injectedFieldNames = fields.stream().map(f -> f.getVars().get(0).getSimpleName()).collect(toSet());

        return cd.getBody().getStatements().stream().anyMatch(stat -> stat.whenType(J.MethodDecl.class)
                .filter(J.MethodDecl::isConstructor)
                .map(md -> md.getParams().getParams().stream()
                        .map(p -> p.whenType(J.VariableDecls.class)
                                .map(mv -> mv.getVars().get(0).getSimpleName())
                                .orElseThrow(() -> new RuntimeException("not possible to get here")))
                        .allMatch(injectedFieldNames::contains))
                .orElse(false));
    }
}
