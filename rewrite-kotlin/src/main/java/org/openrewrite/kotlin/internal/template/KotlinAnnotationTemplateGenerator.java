/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.internal.template.AnnotationTemplateGenerator;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;

import java.util.Set;

public class KotlinAnnotationTemplateGenerator extends AnnotationTemplateGenerator {

    public KotlinAnnotationTemplateGenerator(Set<String> imports) {
        super(imports);
    }

    @Override
    protected void addDummyClass(Cursor cursor, StringBuilder after) {
        if (cursor.getParentOrThrow().getValue() instanceof JavaSourceFile) {
            after.insert(0, "class `$Clazz` {}");
        }
    }

    @Override
    protected void addDummyAnnotationType(StringBuilder after) {
        after.append("\nannotation class `$Placeholder` {}");
    }

    @Override
    protected void addDummyMethod(StringBuilder after) {
        after.insert(0, " fun `$method`() {};");
    }

    @Override
    protected void addDummyVariable(StringBuilder after) {
        after.insert(0, " val `$variable`: Int = 0;");
    }

    @Override
    protected String variable(J.VariableDeclarations variable, Cursor cursor) {
        if (variable.getTypeExpression() == null) {
            return variable.getVariables().get(0).getSimpleName();
        }
        return "val " + variable.getVariables().get(0).getSimpleName() + ": " +
               variable.getTypeExpression().withPrefix(Space.EMPTY).printTrimmed(cursor);
    }

}
