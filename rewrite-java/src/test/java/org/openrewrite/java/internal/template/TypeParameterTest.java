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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class TypeParameterTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "boolean",
      "byte",
      "char",
      "int",
      "long",
      "float",
      "double",
//      "void",
      "short"
    })
    void primitive(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        assertThat(type).isSameAs(JavaType.Primitive.fromKeyword(name));
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Object",
      "String",
    })
    void javaLang(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        assertThat(type).isInstanceOf(JavaType.Class.class);
        assertThat(TypeUtils.toString(type)).isEqualTo("java.lang." + name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "boolean[]",
      "byte[][]",
      "char[]",
      "int[]",
      "long[]",
      "float[][]",
      "double[]",
      "short[]",
      "java.util.function.Function[]",
      "java.lang.String[][][]",
      "java.util.List<java.lang.String>[]",
    })
    void arrays(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        assertThat(type).isInstanceOf(JavaType.Array.class);
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.List",
    })
    void qualified(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        assertThat(type).isInstanceOf(JavaType.Class.class);
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.List<java.lang.String>",
      "java.util.Map<java.lang.String, java.lang.Integer>",
      "java.util.Map<java.lang.String, int[]>",
      "java.util.List<? extends java.lang.Object>",
      "java.util.List<? super java.lang.Integer>",
      "java.util.List<? super java.lang.Integer[]>",
      "java.util.List<java.util.List<? super java.lang.Integer>>",
    })
    void parameterized(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        assertThat(type).isInstanceOf(JavaType.Parameterized.class);
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.List<java.lang.String>",
      "java.util.Map<java.lang.String, java.lang.Integer>",
      "java.util.List<? extends java.lang.Object>",
      "java.util.List<? super java.lang.Integer>",
      "java.util.List<java.util.List<? super java.lang.Integer>>",
    })
    void parameterizedWithModifierShouldNeverHideParametrizedType(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        JavaType.Parameterized pType = (JavaType.Parameterized) type;
        assertThat(pType.withFullyQualifiedName("test")).isInstanceOf(JavaType.Parameterized.class);
        assertThat(pType.withFullyQualifiedName("test")).isNotSameAs(pType);
        assertThat(pType.withFullyQualifiedName(pType.getFullyQualifiedName())).isSameAs(pType);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.List<?>",
    })
    void unbounded(String name) {
        TemplateParameterParser parser = TypeParameter.parser(name);
        JavaType type = TypeParameter.toJavaType(parser.type(), emptyMap());
        assertThat(type).isInstanceOf(JavaType.Parameterized.class);
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @Test
    void parseGenericType() {
        Map<String, JavaType.GenericTypeVariable> genericTypes = TypeParameter.parseGenericTypes(
          Set.of("A", "B extends C", "C extends java.lang.Comparable<? super B> & java.io.Serializable"));

        assertThat(genericTypes).containsKeys("A", "B", "C");
        JavaType.GenericTypeVariable a = genericTypes.get("A");
        JavaType.GenericTypeVariable b = genericTypes.get("B");
        JavaType.GenericTypeVariable c = genericTypes.get("C");

        assertThat(TypeUtils.toGenericTypeString(a)).isEqualTo("A");
        assertThat(TypeUtils.toGenericTypeString(b)).isEqualTo("B extends C");
        assertThat(TypeUtils.toGenericTypeString(c)).isEqualTo("C extends java.lang.Comparable<? super B> & java.io.Serializable");

        assertThat(a.getVariance()).isEqualTo(JavaType.GenericTypeVariable.Variance.INVARIANT);
        assertThat(b.getVariance()).isEqualTo(JavaType.GenericTypeVariable.Variance.COVARIANT);
        assertThat(c.getVariance()).isEqualTo(JavaType.GenericTypeVariable.Variance.COVARIANT);

        assertThat(a.getBounds()).isEmpty();
        assertThat(b.getBounds()).hasSize(1).first().isSameAs(c);
        assertThat(c.getBounds()).hasSize(2);

        assertThat(TypeUtils.toString(c.getBounds().getFirst())).isEqualTo("java.lang.Comparable<? super B>");
        assertThat(TypeUtils.toString(c.getBounds().get(1))).isEqualTo("java.io.Serializable");

        JavaType cBound = ((JavaType.Parameterized) c.getBounds().getFirst()).getTypeParameters().getFirst();
        assertThat(cBound).isInstanceOfSatisfying(JavaType.GenericTypeVariable.class, type -> {
            assertThat(type.getName()).isEqualTo("?");
            assertThat(type.getVariance()).isEqualTo(JavaType.GenericTypeVariable.Variance.CONTRAVARIANT);
            assertThat(type.getBounds()).hasSize(1).first().isSameAs(b);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "T extends java.lang.Comparable<? super T>",
      "T extends java.lang.Enum<T>",
      "T extends java.util.function.Supplier<? extends T>"
    })
    void parseRecursiveGenericType(String name) {
        Set<String> genericTypes = singleton(name);
        Map<String, JavaType.GenericTypeVariable> result = TypeParameter.parseGenericTypes(genericTypes);
        assertThat(result)
          .containsOnlyKeys("T")
          .extractingByKey("T")
          .satisfies(type -> {
              assertThat(type.getVariance()).isEqualTo(JavaType.GenericTypeVariable.Variance.COVARIANT);
              assertThat(type.getBounds())
                .hasSize(1)
                .first().isInstanceOf(JavaType.Parameterized.class);
          });

        JavaType.GenericTypeVariable type = result.get("T");
        JavaType.Parameterized parameterizedType = ((JavaType.Parameterized) type.getBounds().getFirst());
        assertThat(TypeUtils.toString(parameterizedType)).isSubstringOf(name);
        if (name.contains("?")) {
            JavaType.GenericTypeVariable wildcard = (JavaType.GenericTypeVariable) parameterizedType.getTypeParameters().getFirst();
            assertThat(wildcard.getBounds().getFirst()).isSameAs(type);
        } else {
            assertThat(parameterizedType.getTypeParameters().getFirst()).isSameAs(type);
        }
    }
}
