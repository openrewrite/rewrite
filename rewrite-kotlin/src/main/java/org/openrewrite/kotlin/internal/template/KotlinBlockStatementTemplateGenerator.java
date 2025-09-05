/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.internal.template.BlockStatementTemplateGenerator;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;

import java.util.Collection;
import java.util.Set;

public class KotlinBlockStatementTemplateGenerator extends BlockStatementTemplateGenerator {
    public KotlinBlockStatementTemplateGenerator(Set<String> imports, boolean contextSensitive) {
        super(imports, contextSensitive);
    }

    @Override
    protected void contextFreeTemplate(Cursor cursor, J j, Collection<JavaType.GenericTypeVariable> typeVariables, StringBuilder before, StringBuilder after, JavaCoordinates.Mode mode) {
        if (j instanceof J.MethodInvocation) {
            before.insert(0, "class Template {\n");
            JavaType.Method methodType = ((J.MethodInvocation) j).getMethodType();
            if (mode == JavaCoordinates.Mode.REPLACEMENT && (methodType == null || methodType.getReturnType() != JavaType.Primitive.Void)) {
                before.append("var o : Any = ");
            }
            after.append(";\n}");
        } else if (j instanceof Expression && !(j instanceof J.Assignment)) {
            before.insert(0, "class Template {\n");
            if(mode == JavaCoordinates.Mode.REPLACEMENT) {
                before.append("var o : Any = ");
            }
            after.append(";\n}");
        } else if (j instanceof J.ClassDeclaration || j instanceof K.ClassDeclaration) {
            throw new IllegalArgumentException(
                    "Templating a class declaration requires context from which package declaration and imports may be reached. " +
                    "Mark this template as context-sensitive by calling KotlinTemplate.Builder#contextSensitive().");
        } else if (j instanceof Statement && !(j instanceof J.Import) && !(j instanceof J.Package)) {
            before.insert(0, "class Template {\ninit {\n");
            after.append("\n}\n}");
        } else {
            throw new IllegalArgumentException(
                    "Kotlin templating is currently only implemented for context-free expressions and statements and not for `" + j.getClass() + "` instances.");
        }

        before.insert(0, TEMPLATE_INTERNAL_IMPORTS);
        for (String anImport : imports) {
            before.insert(0, anImport);
        }
    }
}
