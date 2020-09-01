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
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.*;

/**
 * Generates a 'get' method for a field. For a field like:
 *
 * String foo;
 *
 * This will add the method:
 *
 * public String getFoo() {
 *     return foo;
 * }
 *
 * If the specified field does not exist no change will be made.
 * If a getter already exists no change will be made.
 *
 */
public class GenerateGetter extends JavaRefactorVisitor {
    public static class Scoped extends JavaRefactorVisitor {
        private final JavaType.Class clazz;
        private final String fieldName;

        public Scoped(JavaType.Class clazz, String fieldName) {
            setCursoringOn();
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

            assert field.getTypeExpr() != null;
            String simpleFieldName = field.getVars().get(0).getSimpleName();
            MethodMatcher getterMatcher = new MethodMatcher(type.getFullyQualifiedName() + " get" + capitalize(simpleFieldName) + "()");
            boolean getterAlreadyExists = classDecl.getMethods().stream().anyMatch(it -> getterMatcher.matches(it, classDecl));
            if (getterAlreadyExists) {
                return cd;
            }
            boolean isMissingTargetField = !cd.getFields().stream().filter(field::isScope).findAny().isPresent();
            if (isMissingTargetField) {
                return cd;
            }

            J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
            assert cu != null;
            JavaParser jp = JavaParser.fromJavaVersion()
                    .styles(cu.getStyles())
                    .build();

            J.VariableDecls.NamedVar fieldVar = field.getVars().get(0);
            String fieldName = fieldVar.getSimpleName();

            J.MethodDecl getMethod = TreeBuilder.buildMethodDeclaration(jp,
                    classDecl,
                    "public " + field.getTypeExpr().print().trim() + " get" + capitalize(fieldName) + "()" + " {\n" +
                            "    return " + fieldName + ";\n" +
                            "}\n",
                    field.getTypeAsClass());

            andThen(new AutoFormat(getMethod));

            J.Block<J> body = cd.getBody();
            List<J> statements = new ArrayList<>(body.getStatements());
            statements.add(getMethod);
            cd = cd.withBody(body.withStatements(statements));

            return cd;
        }
    }
}
