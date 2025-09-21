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
package org.openrewrite.java.internal.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class InlineMethodCallsRecipeGeneratorTest {

    @Test
    void generateRecipesFromTypeTable(@TempDir Path tempDir) throws Exception {
        // Create a test TypeTable TSV file
        Path inputTsv = tempDir.resolve("test.tsv.gz");
        String tsvContent = """
          groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t-1\t\t\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\toldMethod\t(Ljava/lang/String;)V\t\tinput\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"newMethod(input)",imports=[s"java.util.List"])\t\t\t
          """;
        try (OutputStream os = Files.newOutputStream(inputTsv);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {
            gzos.write(tsvContent.getBytes());
        }

        // Run the generator
        Path outputYaml = tempDir.resolve("output.yaml");
        InlineMethodCallsRecipeGenerator.generate(inputTsv, outputYaml, "org.openrewrite.java.InlineMethodCallsGenerated");

        // Read and verify the content
        //language=yaml
        assertThat(outputYaml).hasContent("""
          #
          # Generated InlineMe recipes from TypeTable
          #

          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.java.InlineMethodCallsGenerated
          displayName: Inline methods annotated with `@InlineMe`
          description: >-
            Automatically generated recipes to inline method calls based on `@InlineMe` annotations
            discovered in the type table.
          recipeList:

            # From com.example:test-lib:1.0.0
            - org.openrewrite.java.InlineMethodCalls:
                methodPattern: 'com.example.TestClass oldMethod(java.lang.String)'
                replacement: 'newMethod(input)'
                imports:
                  - 'java.util.List'
                classpathFromResources:
                  - 'test-lib-1.0.0'
          """);
    }

    @Test
    void handleMultipleAnnotatedMethods(@TempDir Path tempDir) throws Exception {
        // Create a test TypeTable TSV file with multiple annotated methods
        Path inputTsv = tempDir.resolve("test.tsv.gz");
        String tsvContent = """
          groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t-1\t\t\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tmethod1\t()V\t\t\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"replacement1()")\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tmethod2\t(I)V\t\tnum\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"replacement2(num)",staticImports=[s"java.util.Collections.emptyList"])\t\t\t
          """;
        try (OutputStream os = Files.newOutputStream(inputTsv);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {
            gzos.write(tsvContent.getBytes());
        }

        // Run the generator
        Path outputYaml = tempDir.resolve("output.yaml");
        InlineMethodCallsRecipeGenerator.generate(inputTsv, outputYaml, "org.openrewrite.java.InlineMethodCallsGenerated");

        // Read and verify the content
        //language=yaml
        assertThat(outputYaml).hasContent("""
          #
          # Generated InlineMe recipes from TypeTable
          #

          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.java.InlineMethodCallsGenerated
          displayName: Inline methods annotated with `@InlineMe`
          description: >-
            Automatically generated recipes to inline method calls based on `@InlineMe` annotations
            discovered in the type table.
          recipeList:

            # From com.example:test-lib:1.0.0
            - org.openrewrite.java.InlineMethodCalls:
                methodPattern: 'com.example.TestClass method1()'
                replacement: 'replacement1()'
                classpathFromResources:
                  - 'test-lib-1.0.0'
            - org.openrewrite.java.InlineMethodCalls:
                methodPattern: 'com.example.TestClass method2(int)'
                replacement: 'replacement2(num)'
                staticImports:
                  - 'java.util.Collections.emptyList'
                classpathFromResources:
                  - 'test-lib-1.0.0'
          """);
    }

    @Test
    void skipMethodsWithoutInlineMeAnnotation(@TempDir Path tempDir) throws Exception {
        // Create a test TypeTable TSV file with a mix of annotated and non-annotated methods
        Path inputTsv = tempDir.resolve("test.tsv.gz");
        String tsvContent = """
          groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t-1\t\t\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tregularMethod\t()V\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tannotatedMethod\t()V\t\t\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"replacement()")\t\t\t
          """;
        try (OutputStream os = Files.newOutputStream(inputTsv);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {
            gzos.write(tsvContent.getBytes());
        }

        // Run the generator
        Path outputYaml = tempDir.resolve("output.yaml");
        InlineMethodCallsRecipeGenerator.generate(inputTsv, outputYaml, "org.openrewrite.java.InlineMethodCallsGenerated");

        // Read and verify the content
        //language=yaml
        assertThat(outputYaml).hasContent("""
          #
          # Generated InlineMe recipes from TypeTable
          #

          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.java.InlineMethodCallsGenerated
          displayName: Inline methods annotated with `@InlineMe`
          description: >-
            Automatically generated recipes to inline method calls based on `@InlineMe` annotations
            discovered in the type table.
          recipeList:

            # From com.example:test-lib:1.0.0
            - org.openrewrite.java.InlineMethodCalls:
                methodPattern: 'com.example.TestClass annotatedMethod()'
                replacement: 'replacement()'
                classpathFromResources:
                  - 'test-lib-1.0.0'
          """);
    }
}
