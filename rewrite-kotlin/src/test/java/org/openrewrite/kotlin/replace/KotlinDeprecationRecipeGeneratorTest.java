/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.replace;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.kotlin.replace.KotlinDeprecatedMethodScanner.DeprecatedMethod;
import org.openrewrite.kotlin.replace.KotlinDeprecatedMethodScanner.ScanResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.replace.KotlinDeprecationRecipeGenerator.*;

class KotlinDeprecationRecipeGeneratorTest {

    @Nested
    class GenerateYamlRecipe {

        @Test
        void generatesCorrectDisplayName() {
            ScanResult result = new ScanResult(
                    "org.jetbrains.kotlinx",
                    "kotlinx-coroutines-core",
                    "1.10.2",
                    "1",
                    new ArrayList<>()
            );

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).contains(
              "name: org.jetbrains.kotlinx.ReplaceDeprecatedKotlinxCoroutinesCore1Methods",
              "displayName: Replace deprecated `kotlinx-coroutines-core` methods");
        }

        @Test
        void includesMethodPattern() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod(
                    "com.example.Foo bar(java.lang.String)",
                    "baz(s)",
                    List.of(),
                    "my-lib-1"
            ));
            ScanResult result = new ScanResult("com.example", "my-lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).contains("methodPattern: 'com.example.Foo bar(java.lang.String)'");
        }

        @Test
        void includesReplacement() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod(
                    "com.example.Foo bar()",
                    "newBar()",
                    List.of(),
                    "my-lib-1"
            ));
            ScanResult result = new ScanResult("com.example", "my-lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).contains("replacement: 'newBar()'");
        }

        @Test
        void includesImportsWhenPresent() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod(
                    "com.example.Foo bar()",
                    "Helper.create()",
                    List.of("com.example.Helper", "com.example.util.Utils"),
                    "my-lib-1"
            ));
            ScanResult result = new ScanResult("com.example", "my-lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).contains("imports:");
            assertThat(yaml).contains("- 'com.example.Helper'");
            assertThat(yaml).contains("- 'com.example.util.Utils'");
        }

        @Test
        void omitsImportsWhenEmpty() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod(
                    "com.example.Foo bar()",
                    "baz()",
                    List.of(),
                    "my-lib-1"
            ));
            ScanResult result = new ScanResult("com.example", "my-lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).doesNotContain("imports:");
        }

        @Test
        void includesClasspathFromResources() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod(
                    "com.example.Foo bar()",
                    "baz()",
                    List.of(),
                    "my-lib-1"
            ));
            ScanResult result = new ScanResult("com.example", "my-lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).contains("classpathFromResources:");
            assertThat(yaml).contains("- 'my-lib-1'");
        }

        @Test
        void escapesSingleQuotesInYaml() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod(
                    "com.example.Foo bar()",
                    "it's working",
                    List.of(),
                    "my-lib-1"
            ));
            ScanResult result = new ScanResult("com.example", "my-lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            assertThat(yaml).contains("replacement: 'it''s working'");
        }

        @Test
        void sortsMethodsByPattern() {
            List<DeprecatedMethod> methods = new ArrayList<>();
            methods.add(new DeprecatedMethod("com.example.Zoo method()", "z()", List.of(), "lib-1"));
            methods.add(new DeprecatedMethod("com.example.Alpha method()", "a()", List.of(), "lib-1"));
            methods.add(new DeprecatedMethod("com.example.Middle method()", "m()", List.of(), "lib-1"));

            ScanResult result = new ScanResult("com.example", "lib", "1.0.0", "1", methods);

            String yaml = generateYamlRecipe(result).toString();

            int alphaPos = yaml.indexOf("com.example.Alpha");
            int middlePos = yaml.indexOf("com.example.Middle");
            int zooPos = yaml.indexOf("com.example.Zoo");

            assertThat(alphaPos).isLessThan(middlePos);
            assertThat(middlePos).isLessThan(zooPos);
        }
    }

    @Nested
    class BuildRecipeName {
        @Test
        void capitalizesArtifactNameParts() {
            String name = buildRecipeName("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1");

            assertThat(name).isEqualTo("org.jetbrains.kotlinx.ReplaceDeprecatedKotlinxCoroutinesCore1Methods");
        }

        @Test
        void handlesHyphenatedGroupId() {
            String name = buildRecipeName("io.arrow-kt", "arrow-core", "2");

            assertThat(name).isEqualTo("io.arrow-kt.ReplaceDeprecatedArrowCore2Methods");
        }

        @Test
        void handlesSinglePartArtifact() {
            String name = buildRecipeName("com.example", "mylib", "3");

            assertThat(name).isEqualTo("com.example.ReplaceDeprecatedMylib3Methods");
        }
    }

    @Nested
    class EscapeYaml {
        @Test
        void escapesSingleQuotes() {
            assertThat(escapeYaml("it's")).isEqualTo("it''s");
        }

        @Test
        void escapesMultipleSingleQuotes() {
            assertThat(escapeYaml("it's Bob's")).isEqualTo("it''s Bob''s");
        }

        @Test
        void leavesOtherCharactersUnchanged() {
            assertThat(escapeYaml("hello world")).isEqualTo("hello world");
        }

        @Test
        void handlesEmptyString() {
            assertThat(escapeYaml("")).isEqualTo("");
        }
    }
}
