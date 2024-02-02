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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.internal.grammar.TemplateParameterLexer;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

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
        TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
          CharStreams.fromString(name))));
        JavaType type = TypeParameter.toFullyQualifiedName(parser.type());
        assertThat(type).isSameAs(JavaType.Primitive.fromKeyword(name));
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Object",
      "String",
    })
    void javaLang(String name) {
        TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
          CharStreams.fromString(name))));
        JavaType type = TypeParameter.toFullyQualifiedName(parser.type());
        assertThat(type).isInstanceOf(JavaType.Class.class);
        assertThat(TypeUtils.toString(type)).isEqualTo("java.lang." + name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.List",
    })
    void qualified(String name) {
        TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
          CharStreams.fromString(name))));
        JavaType type = TypeParameter.toFullyQualifiedName(parser.type());
        assertThat(type).isInstanceOf(JavaType.Class.class);
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
    void parameterized(String name) {
        TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
          CharStreams.fromString(name))));
        JavaType type = TypeParameter.toFullyQualifiedName(parser.type());
        assertThat(type).isInstanceOf(JavaType.Parameterized.class);
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "java.util.List<?>",
    })
    void unbounded(String name) {
        TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
          CharStreams.fromString(name))));
        JavaType type = TypeParameter.toFullyQualifiedName(parser.type());
        assertThat(type).isInstanceOf(JavaType.Parameterized.class);
        assertThat(TypeUtils.toString(type)).isEqualTo(name);
    }
}