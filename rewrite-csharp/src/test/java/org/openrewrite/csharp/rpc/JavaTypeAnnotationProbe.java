/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.csharp.CSharpIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only visitor the C# {@code JavaTypeAnnotationRpcTest} runs on this Java peer to prove type
 * annotations survive the RPC boundary in both directions:
 * <ul>
 *   <li><b>C# &rarr; Java:</b> the {@link SearchResult} description this visitor attaches is
 *   rendered from the annotations <em>as materialized on the Java side</em> — it can only come out
 *   right if the C# sender and the Java receiver agreed on every element and value.</li>
 *   <li><b>Java &rarr; C#:</b> the probed class' type is replaced with a freshly built
 *   {@link JavaType.Class} whose annotation values are visibly transformed ({@code "probed:"}
 *   prefix on string constants). Fresh instances defeat the delta cache, so the annotations travel
 *   back through the real Java sender and real C# receiver rather than resolving to the C# side's
 *   local copies.</li>
 * </ul>
 * Instantiated by name via {@code PreparedRecipeCache.instantiateVisitor}, with the fully
 * qualified name of the class to probe as the {@code type} visitor option.
 */
@RequiredArgsConstructor
public class JavaTypeAnnotationProbe extends CSharpIsoVisitor<ExecutionContext> {
    private final String type;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
        if (!(cd.getType() instanceof JavaType.Class)) {
            return cd;
        }
        JavaType.Class cls = (JavaType.Class) cd.getType();
        if (!type.equals(cls.getFullyQualifiedName()) || cls.getAnnotations().isEmpty()) {
            return cd;
        }

        StringBuilder description = new StringBuilder();
        List<JavaType.FullyQualified> transformed = new ArrayList<>(cls.getAnnotations().size());
        for (JavaType.FullyQualified annotation : cls.getAnnotations()) {
            if (description.length() > 0) {
                description.append(';');
            }
            if (!(annotation instanceof JavaType.Annotation)) {
                description.append(annotation.getFullyQualifiedName());
                transformed.add(annotation);
                continue;
            }
            JavaType.Annotation a = (JavaType.Annotation) annotation;
            description.append(render(a));
            transformed.add(transform(a));
        }

        return SearchResult.found(cd.withType(cls.withAnnotations(transformed)), description.toString());
    }

    private static String render(JavaType.Annotation annotation) {
        StringBuilder s = new StringBuilder("@");
        s.append(annotation.getType() == null ? "{undefined}" : annotation.getType().getFullyQualifiedName());
        List<JavaType.Annotation.ElementValue> values = annotation.getValues();
        if (!values.isEmpty()) {
            s.append('(');
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    s.append(',');
                }
                JavaType.Annotation.ElementValue value = values.get(i);
                s.append(renderElement(value.getElement())).append('=').append(renderValue(value));
            }
            s.append(')');
        }
        return s.toString();
    }

    private static String renderElement(JavaType element) {
        if (element instanceof JavaType.Variable) {
            return ((JavaType.Variable) element).getName();
        }
        if (element instanceof JavaType.Method) {
            return ((JavaType.Method) element).getName() + "()";
        }
        return String.valueOf(element);
    }

    private static String renderValue(JavaType.Annotation.ElementValue value) {
        if (value instanceof JavaType.Annotation.SingleElementValue) {
            JavaType.Annotation.SingleElementValue single = (JavaType.Annotation.SingleElementValue) value;
            if (single.getConstantValue() != null) {
                return String.valueOf(single.getConstantValue());
            }
            return renderReference(single.getReferenceValue());
        }
        if (value instanceof JavaType.Annotation.ArrayElementValue) {
            JavaType.Annotation.ArrayElementValue array = (JavaType.Annotation.ArrayElementValue) value;
            StringBuilder s = new StringBuilder("[");
            if (array.getConstantValues() != null) {
                for (int i = 0; i < array.getConstantValues().length; i++) {
                    if (i > 0) {
                        s.append(',');
                    }
                    s.append(array.getConstantValues()[i]);
                }
            } else if (array.getReferenceValues() != null) {
                for (int i = 0; i < array.getReferenceValues().length; i++) {
                    if (i > 0) {
                        s.append(',');
                    }
                    s.append(renderReference(array.getReferenceValues()[i]));
                }
            }
            return s.append(']').toString();
        }
        return String.valueOf(value);
    }

    private static String renderReference(JavaType reference) {
        if (reference instanceof JavaType.Variable) {
            JavaType.Variable variable = (JavaType.Variable) reference;
            String owner = variable.getOwner() instanceof JavaType.FullyQualified ?
                    ((JavaType.FullyQualified) variable.getOwner()).getFullyQualifiedName() : "?";
            return owner + "." + variable.getName();
        }
        if (reference instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) reference).getFullyQualifiedName();
        }
        return String.valueOf(reference);
    }

    private static JavaType.FullyQualified transform(JavaType.Annotation annotation) {
        List<JavaType.Annotation.ElementValue> values = new ArrayList<>(annotation.getValues().size());
        for (JavaType.Annotation.ElementValue value : annotation.getValues()) {
            if (value instanceof JavaType.Annotation.SingleElementValue &&
                ((JavaType.Annotation.SingleElementValue) value).getConstantValue() instanceof String) {
                JavaType.Annotation.SingleElementValue single = (JavaType.Annotation.SingleElementValue) value;
                values.add(new JavaType.Annotation.SingleElementValue(
                        single.getElement(), "probed:" + single.getConstantValue(), null));
            } else {
                values.add(value);
            }
        }
        return new JavaType.Annotation(annotation.getType(), values);
    }
}
