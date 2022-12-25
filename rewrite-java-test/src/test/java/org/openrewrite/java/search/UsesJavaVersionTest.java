/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.Issue;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.test.RewriteTest.toRecipe;

class UsesJavaVersionTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2035")
    @ParameterizedTest
    @CsvSource(textBlock = """
      8,      8
      1.8,    8
      11,     11
      17,     17
      """)
    void mavenCompilerSources(String version, int major) {
        assertThat(new JavaVersion(randomId(), "", "", version, version).getMajorVersion())
          .isEqualTo(major);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2035")
    @ParameterizedTest
    @CsvSource(textBlock = """
      1.8.0.332,  8
      11.0.15,    11
      17.0.3,     17
      """)
    void javaRuntimeVersions(String version, int major) {
        assertThat(new JavaVersion(randomId(), "", "", version, version).getMajorVersion())
          .isEqualTo(major);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/983")
    @Test
    void invalidJavaVersion() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesJavaVersion<>(-1, Integer.MAX_VALUE))),
          java("class Test {}")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/983")
    @Test
    void findJavaVersion() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesJavaVersion<>(8, Integer.MAX_VALUE))),
          version(
            java(
              "class Test {}",
              "/*~~>*/class Test {}"
            ),
            8
          )
        );
    }

    @Test
    void usesJavaVersion() {
        var usesJavaVersion = new UsesJavaVersion<>(8, 11);
        assertThat(usesJavaVersion.majorVersionMin).isEqualTo(8);
        assertThat(usesJavaVersion.majorVersionMax).isEqualTo(11);
    }
}
