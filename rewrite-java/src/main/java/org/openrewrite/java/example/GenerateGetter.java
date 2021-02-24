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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = true)
public class GenerateGetter extends Recipe {

    String fieldName;

    @Override
    public String getDisplayName() {
        return "Generate Getter";
    }

    @Override
    public String getDescription() {
        return "Generates a 'get' accessor method for fieldName";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GenerateGetterVisitor<>();
    }

    private class GenerateGetterVisitor<P> extends JavaIsoVisitor<P> {
        private final JavaTemplate getter = template("" +
                "public #{} get#{}() {\n" +
                "    return #{};\n" +
                "}"
        ).build();

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
            if (variable.isField(getCursor()) && variable.getSimpleName().equals(fieldName)) {
                getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "varCursor", getCursor());
            }
            return super.visitVariable(variable, p);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
            Cursor varCursor = getCursor().pollNearestMessage("varCursor");
            if (varCursor != null) {
                J.VariableDeclarations.NamedVariable var = varCursor.getValue();
                c = c.withTemplate(getter, c.getBody().getCoordinates().lastStatement(),
                        TypeUtils.asClass(var.getType()).getClassName(),
                        StringUtils.capitalize(var.getSimpleName()),
                        var.getSimpleName());
            }
            return c;
        }
    }
}
