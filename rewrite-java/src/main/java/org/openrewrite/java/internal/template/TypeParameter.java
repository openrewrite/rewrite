/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.internal.template;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.GenericPatternNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.TypeNode;
import org.openrewrite.java.internal.template.parser.TemplateParameterParser.TypeParameterNode;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class TypeParameter {

    private static final JavaType.Class TYPE_OBJECT = JavaType.ShallowClass.build("java.lang.Object");

    public static JavaType toJavaType(@Nullable TypeNode type, Map<String, JavaType.GenericTypeVariable> genericTypes) {
        if (type == null) {
            return TYPE_OBJECT;
        }

        // FIXME handle `$` separator
        JavaType result = resolveTypeName(type.typeName(), genericTypes);
        if (!type.typeParameters().isEmpty()) {
            List<JavaType> typeParameters = new ArrayList<>(type.typeParameters().size());
            for (TypeParameterNode param : type.typeParameters()) {
                typeParameters.add(toJavaTypeParameter(param, genericTypes));
            }
            result = new JavaType.Parameterized(null, (JavaType.FullyQualified) result, typeParameters);
        }
        for (int i = 0; i < type.arrayDepth(); i++) {
            result = new JavaType.Array(null, result, null);
        }
        return result;
    }

    private static JavaType resolveTypeName(String fqn, Map<String, JavaType.GenericTypeVariable> genericTypes) {
        if (fqn.indexOf('.') >= 0) {
            return JavaType.ShallowClass.build(fqn);
        }
        if (genericTypes.containsKey(fqn)) {
            return genericTypes.get(fqn);
        }
        if ("String".equals(fqn)) {
            return JavaType.ShallowClass.build("java.lang.String");
        }
        if ("Object".equals(fqn)) {
            return TYPE_OBJECT;
        }
        JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(fqn);
        if (primitive != null) {
            return primitive;
        }
        throw new IllegalArgumentException("Unknown type " + fqn + ". Make sure all types are fully qualified.");
    }

    private static JavaType toJavaTypeParameter(TypeParameterNode node, Map<String, JavaType.GenericTypeVariable> genericTypes) {
        if (node.isWildcard()) {
            return new JavaType.GenericTypeVariable(null, "?", JavaType.GenericTypeVariable.Variance.INVARIANT, emptyList());
        }
        JavaType bound = toJavaType(node.type(), genericTypes);
        if (node.variance() != null) {
            JavaType.GenericTypeVariable.Variance variance = node.variance() == TemplateParameterParser.Variance.EXTENDS ?
                    JavaType.GenericTypeVariable.Variance.COVARIANT :
                    JavaType.GenericTypeVariable.Variance.CONTRAVARIANT;
            return new JavaType.GenericTypeVariable(null, "?", variance, singletonList(bound));
        }
        return bound;
    }

    public static Map<String, JavaType.GenericTypeVariable> parseGenericTypes(Set<String> genericTypes) {
        Map<String, GenericPatternNode> contexts = new LinkedHashMap<>();
        for (String e : genericTypes) {
            GenericPatternNode pattern = TemplateParameterParser.parseGenericPattern(e);
            if (contexts.put(pattern.genericName(), pattern) != null) {
                throw new IllegalArgumentException("Found duplicated generic type.");
            }
        }

        Map<String, JavaType.GenericTypeVariable> genericTypesMap = new LinkedHashMap<>();
        for (String name : contexts.keySet()) {
            genericTypesMap.put(name,
                    new JavaType.GenericTypeVariable(null, name, JavaType.GenericTypeVariable.Variance.INVARIANT, emptyList()));
        }

        for (Map.Entry<String, JavaType.GenericTypeVariable> entry : genericTypesMap.entrySet()) {
            String name = entry.getKey();
            JavaType.GenericTypeVariable genericType = entry.getValue();
            List<TypeNode> rawBounds = contexts.get(name).bounds();
            if (rawBounds.isEmpty()) {
                continue;
            }
            List<JavaType> bounds = new ArrayList<>(rawBounds.size());
            for (TypeNode b : rawBounds) {
                bounds.add(toJavaType(b, genericTypesMap));
            }
            //noinspection ResultOfMethodCallIgnored
            genericType.unsafeSet(name, JavaType.GenericTypeVariable.Variance.COVARIANT, bounds);
        }
        return genericTypesMap;
    }
}
