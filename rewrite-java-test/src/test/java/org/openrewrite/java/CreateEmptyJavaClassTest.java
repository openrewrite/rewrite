/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CreateEmptyJavaClassTest implements RewriteTest {

    @DocumentExample
    @Test
    void hasCreatedJavaClass() {
        rewriteRun(
          spec -> spec.recipe(new CreateEmptyJavaClass(
            "src/main/java",
            "org.openrewrite.example",
            "public",
            "ExampleClass",
            null,
            "foo/bar/"
          )),
          java(
            null,
            """
              package org.openrewrite.example;

              public class ExampleClass {
              }
              """,
            spec -> spec.path("foo/bar/src/main/java/org/openrewrite/example/ExampleClass.java")
          )
        );
    }

    @DocumentExample
    @Test
    void hasOverwrittenFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateEmptyJavaClass(
            "src/main/java",
            "org.openrewrite.example",
            "protected",
            "ExampleClass",
            true,
            null
          )).cycles(1).expectedCyclesThatMakeChanges(1),
          java(
            """
              package org.openrewrite.example;

              public class ExampleClass { Object o = null; }
              """,
            """
              package org.openrewrite.example;

              protected class ExampleClass {
              }
              """,
            spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass.java")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateEmptyJavaClass(
            "src/main/java",
            "org.openrewrite.example",
            "public",
            "ExampleClass",
            false,
            null
          )),
          java(
            """
              package org.openrewrite.example;

              public class ExampleClass { Object o = null; }
              """,
            spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass.java")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFileWhenOverwriteNull() {
        rewriteRun(
          spec -> spec.recipe(new CreateEmptyJavaClass(
            "src/main/java",
            "org.openrewrite.example",
            "public",
            "ExampleClass",
            null,
            null
          )),
          java(
            """
              package org.openrewrite.example;

              public class ExampleClass { Object o = null; }
              """,
            spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass.java")
          )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateEmptyJavaClass(
            "src/main/java",
            "org.openrewrite.example",
            "public",
            "ExampleClass2",
            true,
            null
          )).cycles(1).expectedCyclesThatMakeChanges(1),
          java(
            """
              package org.openrewrite.example;

              public class ExampleClass1 { Object o = null; }
              """,
            spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass1.java")
          ),
          java(
            null,
            """
              package org.openrewrite.example;

              public class ExampleClass2 {
              }
              """,
            spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass2.java")
          )
        );
    }

    @Test
    void shouldCreatePackagePrivateClass() {
        rewriteRun(
          spec -> spec.recipe(new CreateEmptyJavaClass(
            "src/main/java",
            "org.openrewrite.example",
            "package-private",
            "ExampleClass",
            null,
            "foo/bar"
          )),
          java(
            null,
            """
              package org.openrewrite.example;

              class ExampleClass {
              }
              """,
            spec -> spec.path("foo/bar/src/main/java/org/openrewrite/example/ExampleClass.java")
          )
        );
    }
}
