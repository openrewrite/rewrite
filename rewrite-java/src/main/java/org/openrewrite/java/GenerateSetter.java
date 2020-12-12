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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Formatting.format;
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
public class GenerateSetter {
    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.ClassDecl scope;
        private final String fieldName;

        public Scoped(J.ClassDecl scope, String fieldName) {
            this.scope = scope;
            this.fieldName = fieldName;
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl cd = super.visitClassDecl(classDecl);

            if(!classDecl.isScope(scope)) {
                return cd;
            }

            J.VariableDecls field = cd.getFields().stream()
                    .filter(it -> it.getVars().get(0).getSimpleName().equals(fieldName))
                    .findAny()
                    .orElse(null);
            if (field == null) {
                return cd;
            }

            JavaType.Class type = TypeUtils.asClass(classDecl.getType());
            if (type != null) {
                String fieldTypeString;
                JavaType.Class classType = field.getTypeAsClass();
                if(classType != null) {
                    fieldTypeString = classType.getFullyQualifiedName();
                } else {
                    assert field.getTypeExpr() != null;
                    fieldTypeString = field.getTypeExpr().print();
                }

                MethodMatcher setterMatcher = new MethodMatcher(
                        type.getFullyQualifiedName() + " set" + capitalize(fieldName) + "(" + fieldTypeString + ")");

                boolean setterAlreadyExists = cd.getMethods().stream().anyMatch(it -> setterMatcher.matches(it, classDecl));
                if (setterAlreadyExists) {
                    return cd;
                }

                J.VariableDecls.NamedVar fieldVar = field.getVars().get(0);
                JavaType fieldType = fieldVar.getType();

                // TreeBuilder.buildMethodDeclaration() doesn't currently type-attribute inner classes correctly
                // Manually construct the AST to get the types juuuust right
                J.Ident valueArgument = J.Ident.build(randomId(), "value", fieldType, emptyList(), format(" "), Markers.EMPTY);
                Expression assignmentExp;
                if (fieldVar.getSimpleName().equals("value")) {
                    assignmentExp = new J.FieldAccess(randomId(),
                            J.Ident.build(randomId(),
                                    "this",
                                    cd.getType(),
                                    emptyList(),
                                    Formatting.EMPTY,
                                    Markers.EMPTY),
                            fieldVar.getName(),
                            fieldType,
                            emptyList(),
                            format("", " "),
                            Markers.EMPTY);
                } else {
                    assignmentExp = fieldVar.getName().withFormatting(format("", " "));
                }
                J.MethodDecl setMethod = new J.MethodDecl(randomId(),
                        emptyList(),
                        Collections.singletonList(new J.Modifier.Public(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY)),
                        null,
                        JavaType.Primitive.Void.toTypeTree().withFormatting(format(" ")),
                        J.Ident.build(randomId(), "set" + capitalize(fieldName), null, emptyList(), format(" "), Markers.EMPTY),
                        new J.MethodDecl.Parameters(randomId(),
                                Collections.singletonList(new J.VariableDecls(randomId(),
                                        emptyList(),
                                        emptyList(),
                                        field.getTypeExpr(),
                                        null,
                                        emptyList(),
                                        Collections.singletonList(new J.VariableDecls.NamedVar(
                                                randomId(),
                                                valueArgument,
                                                new J.Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                                                emptyList(),
                                                null,
                                                new J.Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                                                fieldType,
                                                emptyList(),
                                                Formatting.EMPTY,
                                                Markers.EMPTY)),
                                        emptyList(),
                                        Formatting.EMPTY,
                                        Markers.EMPTY)),
                                emptyList(),
                                Formatting.EMPTY,
                                Markers.EMPTY),
                        null,
                        new J.Block<>(randomId(),
                                null,
                                Collections.singletonList(new J.Assign(
                                        randomId(),
                                        assignmentExp,
                                        valueArgument,
                                        null,
                                        emptyList(),
                                        format("\n    "),
                                        Markers.EMPTY
                                )),
                                emptyList(),
                                format(" "),
                                Markers.EMPTY,
                                new J.Block.End(randomId(), emptyList(), format("\n"), Markers.EMPTY)),
                        null,
                        emptyList(),
                        format("\n\n"),
                        Markers.EMPTY
                );
                andThen(new AutoFormat(setMethod));

                J.Block<J> body = cd.getBody();
                List<J> statements = new ArrayList<>(body.getStatements());
                statements.add(setMethod);
                cd = cd.withBody(body.withStatements(statements));
            }

            return cd;
        }
    }
}
