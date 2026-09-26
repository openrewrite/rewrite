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
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class KotlinBlockStatementTemplateGenerator extends BlockStatementTemplateGenerator {
    /** Kotlin's top type, the counterpart of Java's {@code Object} default. */
    public static final String DEFAULT_BIND_TYPE = "Any";

    private final boolean contextSensitive;
    private final String bindType;

    public KotlinBlockStatementTemplateGenerator(Set<String> imports, boolean contextSensitive, String bindType) {
        super(imports, contextSensitive, bindType);
        this.contextSensitive = contextSensitive;
        this.bindType = bindType;
    }

    /**
     * Context-sensitive Kotlin templates print the whole enclosing source file rather than reconstructing the
     * scope bottom-up, so this bypasses the inherited before/after assembly entirely. See
     * {@link KotlinContextTemplateGenerator}.
     */
    @Override
    public String template(Cursor cursor, String template, Collection<JavaType.GenericTypeVariable> typeVariables,
                           Space.Location location, JavaCoordinates.Mode mode) {
        if (!contextSensitive) {
            return super.template(cursor, template, typeVariables, location, mode);
        }
        if (cursor.firstEnclosing(J.Annotation.class) != null) {
            throw new UnsupportedOperationException(
                    "Annotation arguments must be compile-time constants, so there is no placeholder that can " +
                    "stand in for the surrounding scope. Template annotation arguments with a context-free " +
                    "template instead.");
        }
        String hole = "/*" + TEMPLATE_COMMENT + "*/" + template + "/*" + STOP_COMMENT + "*/";
        return KotlinContextTemplateGenerator.stub(cursor, hole, mode == JavaCoordinates.Mode.REPLACEMENT);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    protected void contextFreeTemplate(Cursor cursor, J j, Collection<JavaType.GenericTypeVariable> typeVariables, StringBuilder before, StringBuilder after) {
        String classDeclaration = "class Template" + kotlinTypeParameters(typeVariables);
        if (j instanceof J.MethodInvocation) {
            // An invocation accepts either an expression or a statement in its place, so wrap in an
            // initializer block, which admits both. The binding is added only when the invocation being
            // replaced produces a value, since that is when the template needs an expected type.
            before.insert(0, classDeclaration + " {\ninit {\n");
            JavaType.Method methodType = ((J.MethodInvocation) j).getMethodType();
            if (methodType == null || methodType.getReturnType() != JavaType.Primitive.Void) {
                before.append("var o : ").append(bindType).append(" = ");
            }
            after.append("\n}\n}");
        } else if (j instanceof Expression && !(j instanceof J.Assignment)) {
            before.insert(0, classDeclaration + " {\n");
            // The declared type of this synthetic binding is the expected type the template expression is
            // inferred against. `Any` suffices for most expressions, but not for ones that have no type
            // without a target — a lambda, a callable reference, `emptyList()`, or a generic constructor.
            before.append("var o : ").append(bindType).append(" = ");
            after.append(";\n}");
        } else if (j instanceof J.ClassDeclaration || j instanceof K.ClassDeclaration) {
            System.out.println("here");
            // Unlike Java, Kotlin allows a class at file scope, so the template needs no enclosing declaration
            // to be parseable and the imports prepended below are sufficient context.
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
                // Wildcards do not appear in declaration position; skip.
                continue;
            }
            if (!firstParam) {
                params.append(", ");
            }
            firstParam = false;
            params.append(tv.getName());
            // A bounded but invariant `T : Bound` is mapped to COVARIANT (KotlinTypeMapping.kt:371), so a
            // bound can arrive under either variance and both must emit it.
            JavaType.GenericTypeVariable.Variance variance = tv.getVariance();
            List<JavaType> bounds = tv.getBounds();
            if ((variance == JavaType.GenericTypeVariable.Variance.COVARIANT ||
                 variance == JavaType.GenericTypeVariable.Variance.CONTRAVARIANT) && !bounds.isEmpty()) {
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
