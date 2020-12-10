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

import org.openrewrite.Formatting;
import org.openrewrite.marker.Markers;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Formatting.formatFirstPrefix;
import static org.openrewrite.Tree.randomId;

public class GenerateConstructorUsingFields {
    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.ClassDecl scope;
        private final List<J.VariableDecls> fields;

        public Scoped(J.ClassDecl scope,
                      List<J.VariableDecls> fields) {
            this.scope = scope;
            this.fields = fields;
            setCursoringOn();
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            if (scope.isScope(classDecl) && !hasRequiredArgsConstructor(classDecl)) {
                List<J> statements = new ArrayList<>(classDecl.getBody().getStatements());

                int lastField = -1;
                for (int i = 0; i < statements.size(); i++) {
                    if (statements.get(i) instanceof J.VariableDecls) {
                        lastField = i;
                    }
                }

                List<Statement> constructorParams = fields.stream()
                        .map(mv -> new J.VariableDecls(randomId(),
                                emptyList(),
                                emptyList(),
                                mv.getTypeExpr() != null ? mv.getTypeExpr().withFormatting(Formatting.EMPTY) : null,
                                null,
                                formatFirstPrefix(mv.getDimensionsBeforeName(), ""),
                                formatFirstPrefix(mv.getVars(), " "),
                                Formatting.EMPTY,
                                Markers.EMPTY))
                        .collect(toList());

                for (int i = 1; i < constructorParams.size(); i++) {
                    constructorParams.set(i, constructorParams.get(i).withFormatting(format(" ")));
                }

                Formatting constructorFormatting = formatter.format(classDecl.getBody());
                J.MethodDecl constructor = new J.MethodDecl(
                        randomId(),
                        emptyList(),
                        singletonList(new J.Modifier.Public(randomId(), Formatting.EMPTY, Markers.EMPTY)),
                        null,
                        null,
                        J.Ident.build(randomId(), classDecl.getSimpleName(), classDecl.getType(), format(" "), Markers.EMPTY),
                        new J.MethodDecl.Parameters(randomId(), constructorParams, Formatting.EMPTY, Markers.EMPTY),
                        null,
                        new J.Block<>(randomId(), null, emptyList(), format(" "),
                                Markers.EMPTY,
                                new J.Block.End(randomId(), format(formatter.findIndent(classDecl.getBody().getIndent(),
                                        classDecl.getBody().getStatements().toArray(new Tree[0])).getPrefix()), Markers.EMPTY)),
                        null,
                        constructorFormatting.withPrefix("\n" + constructorFormatting.getPrefix()),
                        Markers.EMPTY
                );

                if (!fields.isEmpty()) {
                    // add assignment statements to constructor
                    andThen(new AddAssignmentsToConstructor(constructor));
                }

                statements.add(lastField + 1, constructor);

                return classDecl.withBody(classDecl.getBody().withStatements(statements));
            }

            return super.visitClassDecl(classDecl);
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

        private class AddAssignmentsToConstructor extends JavaIsoRefactorVisitor {
            private final J.MethodDecl scope;

            private AddAssignmentsToConstructor(J.MethodDecl scope) {
                this.scope = scope;
                setCursoringOn();
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public J.MethodDecl visitMethod(J.MethodDecl method) {
                if (scope.isScope(method)) {
                    return method.withBody(method.getBody().withStatements(
                            getTreeBuilder().buildSnippet(
                                    getCursor(),
                                    fields.stream().map(mv -> {
                                        String name = mv.getVars().get(0).getSimpleName();
                                        return "this." + name + " = " + name + ";";
                                    }).collect(joining("\n", "", "\n"))
                            ))
                    );
                }

                return super.visitMethod(method);
            }
        }
    }
}
