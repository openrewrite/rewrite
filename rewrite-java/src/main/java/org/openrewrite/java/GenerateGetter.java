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

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.internal.StringUtils.capitalize;

/**
 * Generates a 'get' method for a field. For a field like:
 * <p>
 * String foo;
 * <p>
 * This will add the method:
 * <p>
 * public String getFoo() {
 * return foo;
 * }
 * <p>
 * If the specified field does not exist no change will be made.
 * If a getter already exists no change will be made.
 */
public class GenerateGetter extends JavaRefactorVisitor {
    public static class Scoped extends JavaRefactorVisitor {
        private final J.ClassDecl scope;
        private final String fieldName;

        public Scoped(J.ClassDecl scope, String fieldName) {
            this.scope = scope;
            this.fieldName = fieldName;
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            if (classDecl.isScope(scope)) {
                J.VariableDecls field = classDecl.getFields().stream()
                        .filter(it -> it.getVars().get(0).getSimpleName().equals(fieldName))
                        .findAny()
                        .orElse(null);
                if (field == null) {
                    return classDecl;
                }

                assert field.getTypeExpr() != null;
                String simpleFieldName = field.getVars().get(0).getSimpleName();

                JavaType.Class classType = TypeUtils.asClass(classDecl.getType());
                if (classType != null) {
                    MethodMatcher getterMatcher = new MethodMatcher(classType.getFullyQualifiedName() +
                            " get" + capitalize(simpleFieldName) + "()");

                    boolean getterAlreadyExists = classDecl.getMethods().stream().anyMatch(it -> getterMatcher.matches(it, classDecl));
                    if (getterAlreadyExists) {
                        return classDecl;
                    }

                    boolean isMissingTargetField = classDecl.getFields().stream().noneMatch(field::isScope);
                    if (isMissingTargetField) {
                        return classDecl;
                    }

                    J.VariableDecls.NamedVar fieldVar = field.getVars().get(0);
                    String fieldName = fieldVar.getSimpleName();

                    J.MethodDecl getMethod = treeBuilder.buildMethodDeclaration(
                            classDecl,
                            "public " + field.getTypeExpr().print().trim() + " get" + capitalize(fieldName) + "()" + " {\n" +
                                    "    return " + fieldName + ";\n" +
                                    "}\n",
                            field.getTypeAsClass()
                    );

                    andThen(new InsertDeclaration.Scoped(classDecl, getMethod));
                }
            }

            return super.visitClassDecl(classDecl);
        }
    }
}
