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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InlineMethodCallsRecipeGeneratorTest {

    @Test
    void generateRecipesFromTypeTable(@TempDir Path tempDir) throws Exception {
        // Create a test TypeTable TSV file
        Path inputTsv = tempDir.resolve("test.tsv");
        String tsvContent = """
          groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t-1\t\t\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\toldMethod\t(Ljava/lang/String;)V\t\tinput\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"newMethod(input)",imports=[s"java.util.List"])\t\t\t
          """;
        Files.write(inputTsv, tsvContent.getBytes());

        Path outputYaml = tempDir.resolve("output.yaml");

        // Run the generator
        InlineMethodCallsRecipeGenerator.main(new String[]{
          inputTsv.toString(),
          outputYaml.toString()
        });

        // Verify the output was created
        assertThat(outputYaml).exists();

        // Read and verify the content
        String yamlContent = Files.readString(outputYaml);
        assertThat(yamlContent)
          .contains("type: specs.openrewrite.org/v1beta/recipe")
          .contains("name: org.openrewrite.java.InlineMethodCallsGenerated")
          .contains("org.openrewrite.java.InlineMethodCalls:")
          .contains("methodPattern: 'com.example.TestClass oldMethod(..)'")
          .contains("replacement: 'newMethod(input)'")
          .contains("imports:")
          .contains("- 'java.util.List'");
    }

    @Test
    void handleMultipleAnnotatedMethods(@TempDir Path tempDir) throws Exception {
        // Create a test TypeTable TSV file with multiple annotated methods
        Path inputTsv = tempDir.resolve("test.tsv");
        String tsvContent = """
          groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t-1\t\t\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tmethod1\t()V\t\t\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"replacement1()")\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tmethod2\t(I)V\t\tnum\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"replacement2(num)",staticImports=[s"java.util.Collections.emptyList"])\t\t\t
          """;
        Files.write(inputTsv, tsvContent.getBytes());

        Path outputYaml = tempDir.resolve("output.yaml");

        // Run the generator
        InlineMethodCallsRecipeGenerator.main(new String[]{
          inputTsv.toString(),
          outputYaml.toString()
        });

        // Verify the output was created
        assertThat(outputYaml).exists();

        // Read and verify the content
        String yamlContent = Files.readString(outputYaml);

        // Check for both methods
        assertThat(yamlContent)
          .contains("methodPattern: 'com.example.TestClass method1(..)'")
          .contains("replacement: 'replacement1()'")
          .contains("methodPattern: 'com.example.TestClass method2(..)'")
          .contains("replacement: 'replacement2(num)'")
          .contains("staticImports:")
          .contains("- 'java.util.Collections.emptyList'");
    }

    @Test
    void skipMethodsWithoutInlineMeAnnotation(@TempDir Path tempDir) throws Exception {
        // Create a test TypeTable TSV file with a mix of annotated and non-annotated methods
        Path inputTsv = tempDir.resolve("test.tsv");
        String tsvContent = """
          groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t-1\t\t\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tregularMethod\t()V\t\t\t\t\t\t\t
          com.example\ttest-lib\t1.0.0\t1\tcom/example/TestClass\t\tjava/lang/Object\t\t1\tannotatedMethod\t()V\t\t\t\t@Lorg/openrewrite/java/InlineMe;(replacement=s"replacement()")\t\t\t
          """;
        Files.write(inputTsv, tsvContent.getBytes());

        Path outputYaml = tempDir.resolve("output.yaml");

        // Run the generator
        InlineMethodCallsRecipeGenerator.main(new String[]{
          inputTsv.toString(),
          outputYaml.toString()
        });

        // Verify the output was created
        assertThat(outputYaml).exists();

        // Read and verify the content
        String yamlContent = Files.readString(outputYaml);

        // Should only contain the annotated method
        assertThat(yamlContent)
          .contains("methodPattern: 'com.example.TestClass annotatedMethod(..)'")
          .contains("replacement: 'replacement()'")
          // Should not contain the regular method
          .doesNotContain("regularMethod");
    }
}