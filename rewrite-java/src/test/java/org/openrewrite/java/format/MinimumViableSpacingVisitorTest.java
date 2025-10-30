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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MinimumViableSpacingVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().dependsOn("""
            package com.example;
            
            public class MyObject {
                public static Builder builder() { return new Builder(); }
                public static Builder newBuilder() { return new Builder(); }
                public static class Builder {
                    Builder name(String n) { return this; }
                    Builder age(int a) { return this; }
                    Builder items(java.util.List<String> items) { return this; }
                    Builder nested(MyObject nested) { return this; }
                    MyObject build() { return new MyObject(); }
                }
            }
            """))
          .recipe(toRecipe(() -> new MinimumViableSpacingVisitor<>(null)));
    }

    @DocumentExample
    @Test
    void reformatChainedMethodInvocationToSingleLine() {
        rewriteRun(
          java(
            """
            package com.example;
            class Test {
                void test() {
                    MyObject myObject = MyObject.builder()
                        .

                        name("John");
                }
            }
            """,
            """
            package com.example;
            class Test {
                void test() {
                    MyObject myObject = MyObject.builder()
                        .name("John");
                }
            }
            """
          )
        );
    }

    @Test
    void doNotReformatChainedMethodInvocationToSingleLineWhenCommentInPrefixOfName() {
        rewriteRun(
          java(
            """
            package com.example;
            class Test {
                void test() {
                    MyObject myObject = MyObject.builder()
                        . //Some comment
                        name("John");
                }
            }
            """
          )
        );
    }
}
