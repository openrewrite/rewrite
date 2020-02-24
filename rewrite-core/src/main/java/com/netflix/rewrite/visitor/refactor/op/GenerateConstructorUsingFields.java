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
package com.netflix.rewrite.visitor.refactor.op;

import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.netflix.rewrite.tree.Formatting.*;
import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

public class GenerateConstructorUsingFields extends ScopedRefactorVisitor {
    private final List<Tr.VariableDecls> fields;

    public GenerateConstructorUsingFields(UUID scope, List<Tr.VariableDecls> fields) {
        super(scope);
        this.fields = fields;
    }

    @Override
    public List<AstTransform> visitClassDecl(Tr.ClassDecl classDecl) {
        return maybeTransform(classDecl,
                isScope(classDecl) && !hasRequiredArgsConstructor(classDecl),
                super::visitClassDecl,
                (cd, cursor) -> {
                    List<Tree> statements = cd.getBody().getStatements();

                    int lastField = 0;
                    for (int i = 0; i < statements.size(); i++) {
                        if (statements.get(i) instanceof Tr.VariableDecls) {
                            lastField = i;
                        }
                    }

                    List<Statement> constructorParams = fields.stream()
                            .map(mv -> new Tr.VariableDecls(randomId(),
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
                    Tr.MethodDecl constructor = new Tr.MethodDecl(randomId(), emptyList(),
                            singletonList(new Tr.Modifier.Public(randomId(), EMPTY)),
                            null,
                            null,
                            Tr.Ident.build(randomId(), cd.getSimpleName(), cd.getType(), format(" ")),
                            new Tr.MethodDecl.Parameters(randomId(), constructorParams, EMPTY),
                            null,
                            new Tr.Block<>(randomId(), null, emptyList(), format(" "),
                                    formatter().findIndent(cd.getBody().getIndent(), cd.getBody().getStatements().toArray(Tree[]::new)).getPrefix()),
                            null,
                            constructorFormatting.withPrefix("\n" + constructorFormatting.getPrefix()));

                    // add assignment statements to constructor
                    andThen(new ScopedRefactorVisitor(constructor.getId()) {
                        @Override
                        public List<AstTransform> visitMethod(Tr.MethodDecl method) {
                            return maybeTransform(method,
                                    isScope(method),
                                    super::visitMethod,
                                    Tr.MethodDecl::getBody,
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

    private boolean hasRequiredArgsConstructor(Tr.ClassDecl cd) {
        Set<String> injectedFieldNames = fields.stream().map(f -> f.getVars().get(0).getSimpleName()).collect(toSet());

        return cd.getBody().getStatements().stream().anyMatch(stat -> stat.whenType(Tr.MethodDecl.class)
                .filter(Tr.MethodDecl::isConstructor)
                .map(md -> md.getParams().getParams().stream()
                        .map(p -> p.whenType(Tr.VariableDecls.class)
                                .map(mv -> mv.getVars().get(0).getSimpleName())
                                .orElseThrow(() -> new RuntimeException("not possible to get here")))
                        .allMatch(injectedFieldNames::contains))
                .orElse(false));
    }
}
