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

import org.openrewrite.Validated;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

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
    public Validated validate() {
        return Validated.required("type", type).and(Validated.required("field", field));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        if(TypeUtils.isOfClassType(classDecl.getType(), type.getFullyQualifiedName())) {
            classDecl.getFields().stream()
                    .filter(field -> field.getVars().stream()
                            .anyMatch(var -> this.field.equals(var.getSimpleName())))
                    .findAny()
                    .ifPresent(field -> andThen(new Scoped(classDecl, field)));
        }
        return super.visitClassDecl(classDecl);
    }

    public static class Scoped extends JavaRefactorVisitor {
        private final J.ClassDecl clazz;
        private final J.VariableDecls field;

        public Scoped(J.ClassDecl clazz, J.VariableDecls field) {
            setCursoringOn();
            this.field = field;
            this.clazz = clazz;
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl cd = refactor(classDecl, super::visitClassDecl);
            if(!clazz.isScope(cd)) {
                return cd;
            }

            assert field.getTypeExpr() != null;
            assert clazz.getType() != null;
            String simpleFieldName = field.getVars().get(0).getSimpleName();
            MethodMatcher getterMatcher = new MethodMatcher(clazz.getType().getFullyQualifiedName() + " get" + capitalize(simpleFieldName) + "()");
            boolean getterAlreadyExists = classDecl.getMethods().stream().anyMatch(it -> getterMatcher.matches(it, classDecl));
            if(getterAlreadyExists) {
                return cd;
            }
            boolean isMissingTargetField = !cd.getFields().stream().filter(field::isScope).findAny().isPresent();
            if(isMissingTargetField) {
                return cd;
            }

            J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
            assert cu != null;
            JavaParser jp = JavaParser.fromJavaVersion()
                    .styles(cu.getStyles())
                    .build();

            J.VariableDecls.NamedVar fieldVar = field.getVars().get(0);
            String fieldName = fieldVar.getSimpleName();

            assert fieldVar.getType() != null;
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(fieldVar.getType());

            J.MethodDecl getMethod = TreeBuilder.buildMethodDeclaration(jp,
                    classDecl,
                    "public " + field.getTypeExpr().print().trim() + " get" + capitalize(fieldName) + "()" + " {\n" +
                            "    return " + fieldName + ";\n" +
                            "}\n",
                    type);
            andThen(new AutoFormat(getMethod));

            J.Block<J> body = cd.getBody();
            List<J> statements = new ArrayList<>(body.getStatements());
            statements.add(getMethod);
            cd = cd.withBody(body.withStatements(statements));

            return cd;
        }
    }
}
