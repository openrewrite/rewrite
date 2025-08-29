/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnotationSignatureParserTest {

    @Test
    void shouldParseAnnotationSignatureWithoutArguments() {
        var annotationSignature = "@Query";

        var parser = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(annotationSignature))));
        var ctx = parser.annotation();

        // then
        assertNull(ctx.exception);

        assertEquals("Query", ctx.annotationName().getText());
        assertNull(ctx.elementValue());
        assertNull(ctx.elementValuePairs());
    }

    @Test
    void shouldParseAnnotationSignatureWithFullyQualifiedName() {
        var annotationSignature = "@org.springframework.data.jpa.repository.Query";
        var parser = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(annotationSignature))));
        var ctx = parser.annotation();

        // then
        assertNull(ctx.exception);

        assertEquals("org.springframework.data.jpa.repository.Query", ctx.annotationName().getText());
        assertNull(ctx.elementValue());
        assertNull(ctx.elementValuePairs());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "\"str\"", "true", "false", "'a'", "1.23", "4.56f"})
    void shouldParseAnnotationSignatureWithValueArgument(String argument) {
        var annotationSignature = String.format("@Query(%s)", argument);

        var parser = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(annotationSignature))));
        var ctx = parser.annotation();

        // then
        assertNull(ctx.exception);

        assertEquals("Query", ctx.annotationName().getText());
        assertEquals(argument, ctx.elementValue().getText());
        assertNull(ctx.elementValuePairs());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "\"str\"", "true", "false", "'a'", "1.23", "4.56f"})
    void shouldParseAnnotationSignatureWithNamedArguments(String argument) {
        var annotationSignature = String.format("@Query(value = %s, nativeQuery = true)", argument);

        var parser = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(annotationSignature))));
        var ctx = parser.annotation();

        // then
        assertNull(ctx.exception);

        assertEquals("Query", ctx.annotationName().getText());
        assertNull(ctx.elementValue());
        assertEquals("value = " + argument, ctx.elementValuePairs().elementValuePair(0).getText());
        assertEquals(" nativeQuery = true", ctx.elementValuePairs().elementValuePair(1).getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nativeQuery = 1", " nativeQuery = 1", "  nativeQuery=1", "nativeQuery  =     1", "nativeQuery=  1"})
    void shouldParseAnnotationSignatureWithNamedArgumentsAndAnyNumberOfSpaces(String argument) {
        var annotationSignature = String.format("@Query(%s)", argument);

        var parser = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(annotationSignature))));
        var ctx = parser.annotation();

        // then
        assertNull(ctx.exception);

        assertEquals("Query", ctx.annotationName().getText());
        assertNull(ctx.elementValue());
        assertEquals(argument, ctx.elementValuePairs().elementValuePair(0).getText());
    }

    @Test
    void shouldPreserveSpacesInStringLiterals() {
        var annotationSignature = "@Query(\"1 2 3\")";

        var parser = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(annotationSignature))));
        var ctx = parser.annotation();

        // then
        assertNull(ctx.exception);

        assertEquals("Query", ctx.annotationName().getText());
        assertEquals("\"1 2 3\"", ctx.elementValue().getText());
        assertNull(ctx.elementValuePairs());
    }

}
