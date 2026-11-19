/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.internal.template.AnnotationTemplateGenerator;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;

import java.util.Set;
import java.util.stream.Collectors;

public class ScalaAnnotationTemplateGenerator extends AnnotationTemplateGenerator {

    public ScalaAnnotationTemplateGenerator(Set<String> imports) {
        super(imports);
    }

    @Override
    protected void addDummyClass(Cursor cursor, StringBuilder after) {
        if (cursor.getParentOrThrow().getValue() instanceof JavaSourceFile) {
            after.insert(0, "class $Clazz {}");
        }
    }

    @Override
    protected void addDummyMethod(StringBuilder after) {
        after.insert(0, " def $method(): Unit = {}");
    }

    @Override
    protected void addDummyVariable(StringBuilder after) {
        after.insert(0, " val $variable: Int = 0");
    }

    @Override
    protected void addDummyAnnotationType(StringBuilder after) {
        after.append("\nclass $Placeholder extends scala.annotation.StaticAnnotation {}");
    }

    @Override
    protected String openMethodBody() {
        return " = {";
    }

    @Override
    protected String variable(J.VariableDeclarations variable, Cursor cursor) {
        String names = variable.getVariables().stream()
                .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                .collect(Collectors.joining(", "));
        TypeTree type = variable.getTypeExpression();
        // A val without a declared type doesn't parse without an initializer
        return "val " + names + (type == null ? "" : ": " + type.withPrefix(Space.EMPTY).printTrimmed(cursor)) + " = ???";
    }
}
