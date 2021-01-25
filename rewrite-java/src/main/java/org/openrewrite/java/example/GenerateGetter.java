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
package org.openrewrite.java.example;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

@Data
@EqualsAndHashCode(callSuper = true)
public class GenerateGetter extends Recipe {
    private static final JavaTemplate GETTER = JavaTemplate
            .builder("" +
                    "public #{} get#{}() {\n" +
                    "    return #{};\n" +
                    "}")
            .build();

    private final String fieldName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GenerateGetterVisitor<>();
    }

    private class GenerateGetterVisitor<P> extends JavaIsoVisitor<P> {

        public GenerateGetterVisitor() {
            setCursoringOn();
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, P p) {
            if (variable.isField(getCursor()) && variable.getSimpleName().equals(fieldName)) {
                getCursor().putMessageOnFirstEnclosing(J.ClassDecl.class, "varCursor", getCursor());
            }
            return super.visitVariable(variable, p);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
            J.ClassDecl c = super.visitClassDecl(classDecl, p);
            Cursor varCursor = getCursor().pollNearestMessage("varCursor");
            if (varCursor != null) {
                J.VariableDecls.NamedVar var = varCursor.getValue();
                J.Block body = c.getBody();
                J.MethodDecl generatedMethodDecl =
                        (J.MethodDecl) GETTER.generateAfter(varCursor,
                                TypeUtils.asClass(var.getType()).getClassName(),
                                StringUtils.capitalize(var.getSimpleName()),
                                var.getSimpleName()).iterator().next();
                c = c.withBody(body.withStatements(
                        ListUtils.concat(
                                body.getStatements(),
                                new JRightPadded<>(generatedMethodDecl, Space.EMPTY, Markers.EMPTY)
                        )));
            }
            return c;
        }
    }
}
