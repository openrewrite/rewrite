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
import org.openrewrite.java.tree.TreeBuilder;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

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
    private JavaType.Class type;
    private String field;

    /**
     * @param enclosingClassType the fully qualified name of the class that the method should be added to
     */
    public void setType(String enclosingClassType) {
        this.type = JavaType.Class.build(enclosingClassType);
    }

    /**
     * @param field the name of the field to generate a getter for
     */
    public void setField(String field) {
        this.field = field;
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        if(TypeUtils.isOfClassType(classDecl.getType(), type.getFullyQualifiedName())) {
            classDecl.getFields().stream()
                    .filter(field -> field.getVars().stream()
                            .anyMatch(var -> this.field.equals(var.getSimpleName())))
                    .findAny()
                    .ifPresent(field -> {
                        // If there's already a getter method do nothing
                        MethodMatcher getterMatcher = new MethodMatcher(type.getFullyQualifiedName() + " get" + capitalize(this.field) + "()");
                        boolean getterAlreadyExists = classDecl.getMethods().stream().anyMatch(getterMatcher::matches);
                        if(!getterAlreadyExists) {
                            andThen(new Scoped(field));
                        }
                    });
        }
        return super.visitClassDecl(classDecl);
    }

    public static class Scoped extends JavaRefactorVisitor {
        private final J.VariableDecls field;

        public Scoped(J.VariableDecls field) {
            setCursoringOn();
            this.field = field;
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl cd = refactor(classDecl, super::visitClassDecl);

            for(int i = 0; i < cd.getFields().size(); i++) {
                if(field.isScope(cd.getFields().get(i))) {
                    J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                    assert cu != null;
                    JavaParser jp = JavaParser.fromJavaVersion()
                            .styles(cu.getStyles())
                            .build();
                    String fieldName = field.getVars().get(0).getSimpleName();
                    assert field.getTypeExpr() != null;
                    String type = field.getTypeExpr().print();

                    J.MethodDecl getMethod = TreeBuilder.buildMethodDeclaration(jp,
                            classDecl,
                            "public " + type + " get" + capitalize(fieldName) + "()" + " {\n" +
                                    "    return " + fieldName + ";\n" +
                                    "}\n",
                            field.getTypeAsClass());
                    andThen(new AutoFormat(getMethod));

                    J.Block<J> body = cd.getBody();
                    List<J> statements = new ArrayList<>(body.getStatements());
                    statements.add(i + 1, getMethod);
                    cd = cd.withBody(body.withStatements(statements));
                    break;
                }
            }
            return cd;
        }
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) +
                value.substring(1);
    }
}
