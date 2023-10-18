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

class CreateJavaClassTest implements RewriteTest {

    @DocumentExample
    @Test
    void hasCreatedJavaClass() {
        rewriteRun(
                spec -> spec.recipe(new CreateJavaClass(
                        "main",
                        "org.openrewrite.example",
                        "ExampleClass",
                        "package %s;\n\npublic class %s {}",
                        null,
                        "foo/bar/"
                )),
                java(
                        null,
                        "package org.openrewrite.example;\n\npublic class ExampleClass {}\n",
                        spec -> spec.path("foo/bar/src/main/java/org/openrewrite/example/ExampleClass.java")
                )
        );
    }

    @DocumentExample
    @Test
    void hasOverwrittenFile() {
        rewriteRun(
                spec -> spec.recipe(new CreateJavaClass(
                        "main",
                        "org.openrewrite.example",
                        "ExampleClass",
                        "package %s;\n\npublic class %s {}",
                        true,
                        null
                )),
                java(
                        "package org.openrewrite.example;\n\npublic class ExampleClass { Object o = null; }",
                        "package org.openrewrite.example;\n\npublic class ExampleClass {}",
                        spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass.java")
                )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
                spec -> spec.recipe(new CreateJavaClass(
                        "main",
                        "org.openrewrite.example",
                        "ExampleClass",
                        "package %s;\n\npublic class %s {}\n",
                        false,
                        null
                )),
                java(
                        "package org.openrewrite.example;\n\npublic class ExampleClass { Object o = null; }\n",
                        spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass.java")
                )
        );
    }

    @Test
    void shouldNotChangeExistingFileWhenOverwriteNull() {
        rewriteRun(
                spec -> spec.recipe(new CreateJavaClass(
                        "main",
                        "org.openrewrite.example",
                        "ExampleClass",
                        "package %s;\n\npublic class %s {}\n",
                        null,
                        null
                )),
                java(
                        "package org.openrewrite.example;\n\npublic class ExampleClass { Object o = null; }\n",
                        spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass.java")
                )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
                spec -> spec.recipe(new CreateJavaClass(
                        "main",
                        "org.openrewrite.example",
                        "ExampleClass2",
                        "package %s;\n\npublic class %s {}\n",
                        true,
                        null
                )),
                java(
                        "package org.openrewrite.example;\n\npublic class ExampleClass1 { Object o = null; }\n",
                        spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass1.java")
                ),
                java(
                        null,
                        "package org.openrewrite.example;\n\npublic class ExampleClass2 {}\n",
                        spec -> spec.path("src/main/java/org/openrewrite/example/ExampleClass2.java")
                )
        );
    }
}
