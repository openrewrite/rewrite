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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.maven.AddRuntimeConfig.*;
import static org.openrewrite.maven.AddRuntimeConfig.Separator;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

class AddRuntimeConfigTest implements RewriteTest {
    private static final SourceSpecs POM_XML_SOURCE_SPEC = pomXml(
      """
        <project>
            <groupId>com.mycompany.app</groupId>
            <artifactId>my-app</artifactId>
            <version>1</version>
        </project>
        """
    );

    @DocumentExample
    @Test
    void createConfigFileWithRuntimeConfigIfFileDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "3", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            null,
            "-T=3",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4368")
    @Test
    void fromYaml() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.test.AddRuntimeConfig
              description: Test deserialization.
              recipeList:
                - org.openrewrite.maven.AddRuntimeConfig:
                    relativeConfigFileName: maven.config
                    flag: -T
                    argument: 3
                    separator: '='
              """, "org.test.AddRuntimeConfig"
          ),
          POM_XML_SOURCE_SPEC,
          text(
            null,
            "-T=3",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @Test
    void appendRuntimeFlagToEmptyConfigFile() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "3", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            "",
            "-T=3",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @EnumSource(Separator.class)
    @ParameterizedTest
    void createConfigFileWithRuntimeConfigForAllSeparators(Separator separator) {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "3", separator)),
          POM_XML_SOURCE_SPEC,
          text(
            "",
            "-T" + separator.getNotation() + "3",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @Test
    void appendRuntimeFlagIfItDoesNotExist() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "3", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            "-U",
            """
              -U
              -T=3
              """,
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @Test
    void doesNotModifyRuntimeFlagIfExistingWithoutArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-U", null, Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            "-U",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @Test
    void doesNotModifyRuntimeFlagIfExistingWithSameArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "3", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            "-T=3",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"--threads=2", "--threads=3"})
    void appendRuntimeFlagIfExistingForFlagFormatMismatch(String existingConfig) {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "3", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            existingConfig,
            existingConfig + "\n-T=3",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"-T 3", "-T3", "-T=3"})
    void replaceRuntimeFlagIfExistingWithDifferentArgument(String existingConfig) {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(MAVEN_CONFIG_FILENAME, "-T", "4", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            existingConfig,
            "-T=4",
            spec -> spec.path(MAVEN_CONFIG_PATH)
          )
        );
    }

    @Test
    void addJvmRuntimeFlagOnTheSameLine() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(JVM_CONFIG_FILENAME, "-XX:MaxPermSize", "512m", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            "-Xmx2048m -Xms1024m",
            "-Xmx2048m -Xms1024m -XX:MaxPermSize=512m",
            spec -> spec.path(JVM_CONFIG_PATH)
          )
        );
    }

    @Test
    void replaceJvmRuntimeFlagOnTheSameLine() {
        rewriteRun(
          spec -> spec.recipe(new AddRuntimeConfig(JVM_CONFIG_FILENAME, "-XX:MaxPermSize", "1024m", Separator.EQUALS)),
          POM_XML_SOURCE_SPEC,
          text(
            "-Xmx2048m -XX:MaxPermSize=512m -Xms1024m",
            "-Xmx2048m -XX:MaxPermSize=1024m -Xms1024m",
            spec -> spec.path(JVM_CONFIG_PATH)
          )
        );
    }
}
