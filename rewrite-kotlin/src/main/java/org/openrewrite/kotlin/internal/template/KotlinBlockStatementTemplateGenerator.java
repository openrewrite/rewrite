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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class KotlinBlockStatementTemplateGenerator extends BlockStatementTemplateGenerator {
    public KotlinBlockStatementTemplateGenerator(Set<String> imports, boolean contextSensitive) {
        super(imports, contextSensitive);
    }

    @Override
    protected void contextFreeTemplate(Cursor cursor, J j, Collection<JavaType.GenericTypeVariable> typeVariables, StringBuilder before, StringBuilder after) {
        String classDeclaration = "class Template" + kotlinTypeParameters(typeVariables);
        if (j instanceof Expression && !(j instanceof J.Assignment)) {
            before.insert(0, classDeclaration + " {\n");
            before.append("var o : Any = ");
            after.append(";\n}");
        } else if (j instanceof J.ClassDeclaration || j instanceof K.ClassDeclaration) {
            throw new IllegalArgumentException(
                    "Templating a class declaration requires context from which package declaration and imports may be reached. " +
                    "Mark this template as context-sensitive by calling KotlinTemplate.Builder#contextSensitive().");
        } else if (j instanceof Statement && !(j instanceof J.Import) && !(j instanceof J.Package)) {
            before.insert(0, classDeclaration + " {\ninit {\n");
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

    private static String kotlinTypeParameters(Collection<JavaType.GenericTypeVariable> typeVariables) {
        if (typeVariables.isEmpty()) {
            return "";
        }
        StringBuilder params = new StringBuilder("<");
        StringBuilder where = new StringBuilder();
        boolean firstParam = true;
        for (JavaType.GenericTypeVariable tv : typeVariables) {
            if ("?".equals(tv.getName())) {
                continue;
            }
            if (!firstParam) {
                params.append(", ");
            }
            firstParam = false;
            params.append(tv.getName());
            List<JavaType> bounds = tv.getBounds();
            if (tv.getVariance() == JavaType.GenericTypeVariable.Variance.COVARIANT && !bounds.isEmpty()) {
                if (bounds.size() == 1) {
                    params.append(" : ").append(TypeUtils.toString(bounds.get(0)));
                } else {
                    for (JavaType bound : bounds) {
                        if (where.length() > 0) {
                            where.append(", ");
                        }
                        where.append(tv.getName()).append(" : ").append(TypeUtils.toString(bound));
                    }
                }
            }
        }
        if (firstParam) {
            return "";
        }
        params.append(">");
        if (where.length() > 0) {
            params.append(" where ").append(where);
        }
        return params.toString();
    }
}
