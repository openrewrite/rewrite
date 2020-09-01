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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TreeBuilder;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.capitalize;

/**
 * Generates a 'set' method for a field. For a field like:
 *
 * String foo;
 *
 * This will add the method:
 *
 * public void setFoo(String value) {
 *     foo = value;
 * }
 *
 * If the specified field does not exist no change will be made.
 * If a getter already exists no change will be made.
 *
 */
public class GenerateSetter extends JavaRefactorVisitor {
    public static class Scoped extends JavaRefactorVisitor {
        private final JavaType.Class clazz;
        private final String fieldName;

        public Scoped(JavaType.Class clazz, String fieldName) {
            this.fieldName = fieldName;
            this.clazz = clazz;
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl cd = refactor(classDecl, super::visitClassDecl);
            JavaType.Class type = classDecl.getType();
            if(type == null || !clazz.getFullyQualifiedName().equals(type.getFullyQualifiedName())) {
                return cd;
            }

            J.VariableDecls field = cd.getFields().stream()
                    .filter(it -> it.getVars().get(0).getSimpleName().equals(fieldName))
                    .findAny()
                    .orElse(null);
            if (field == null) {
                return cd;
            }
            assert field.getTypeAsClass() != null;
            assert field.getTypeExpr() != null;

            MethodMatcher setterMatcher = new MethodMatcher(
                    type.getFullyQualifiedName() + " set" + capitalize(fieldName) + "(" + field.getTypeAsClass().getFullyQualifiedName() + ")");

            boolean setterAlreadyExists = cd.getMethods().stream().anyMatch(it -> setterMatcher.matches(it, classDecl));
            if (setterAlreadyExists) {
                return cd;
            }

            J.VariableDecls.NamedVar fieldVar = field.getVars().get(0);
            JavaType fieldType = fieldVar.getType();

            // TreeBuilder.buildMethodDeclaration() doesn't currently type-attribute inner classes correctly
            // Manually construct the AST to get the types juuuust right
            J.Ident valueArgument = J.Ident.build(randomId(), "value", fieldType, Formatting.format(" "));
            Expression assignmentExp;
            if(fieldVar.getSimpleName().equals("value")) {
                assignmentExp = new J.FieldAccess(randomId(),
                        J.Ident.build(randomId(),
                                "this",
                                cd.getType(),
                                Formatting.EMPTY),
                        fieldVar.getName(),
                        fieldType,
                        Formatting.format("", " "));
            } else {
                assignmentExp = fieldVar.getName().withFormatting(Formatting.format("", " "));
            }
            J.MethodDecl setMethod = new J.MethodDecl(randomId(),
                    Collections.emptyList(),
                    Collections.singletonList( new J.Modifier.Public(randomId(), Formatting.EMPTY)),
                    null,
                    JavaType.Primitive.Void.toTypeTree().withFormatting(Formatting.format(" ")),
                    J.Ident.build(randomId(), "set" + capitalize(fieldName), null, Formatting.format(" ")),
                    new J.MethodDecl.Parameters(randomId(),
                            Collections.singletonList(new J.VariableDecls(randomId(),
                                    Collections.emptyList(),
                                    Collections.emptyList(),
                                    field.getTypeExpr(),
                                    null,
                                    Collections.emptyList(),
                                    Collections.singletonList(new J.VariableDecls.NamedVar(randomId(),
                                            valueArgument,
                                            Collections.emptyList(),
                                            null,
                                            fieldType,
                                            Formatting.EMPTY)),
                                    Formatting.EMPTY)),
                            Formatting.EMPTY),
                    null,
                    new J.Block<>(randomId(),
                            null,
                            Collections.singletonList(new J.Assign(
                                    randomId(),
                                    assignmentExp,
                                    valueArgument,
                                    null,
                                    Formatting.format("\n    ")
                            )),
                            Formatting.format(" "),
                            new J.Block.End(randomId(), Formatting.format("\n"))),
                    null,
                    Formatting.format("\n\n")
            );
            andThen(new AutoFormat(setMethod));

            J.Block<J> body = cd.getBody();
            List<J> statements = new ArrayList<>(body.getStatements());
            statements.add(setMethod);
            cd = cd.withBody(body.withStatements(statements));

            return cd;
        }
    }
}
