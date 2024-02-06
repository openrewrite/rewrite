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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeAnnotationAttributeNameTest implements RewriteTest {
    @Test
    void renameAttributeName() {
        rewriteRun(
            spec -> spec.recipe(new ChangeAnnotationAttributeName("java.lang.Deprecated", "since", "asOf")),
            //language=java
            java(
                """
                @Deprecated(since = "1.0")
                class A {}
                """,
                """
                @Deprecated(asOf = "1.0")
                class A {}
                """
            )
        );
    }

    @Test
    void doNotChangeIdenticalAssignment() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName("java.lang.SuppressWarnings", "value", "value")),
          java(
            """
              @SuppressWarnings(value = {"foo", "bar"})
              class A {}
              """
          )
        );
    }

    @Test
    void renameValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName("org.example.Foo", "value", "bar")),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
                  String bar() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
                            
              @Foo(/* some comment */ "1.0")
              class A {}
              """,
            """
              import org.example.Foo;
                            
              @Foo(/* some comment */ bar = "1.0")
              class A {}
              """
          )
        );
    }

    @Test
    void expandImplicitValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName("java.lang.SuppressWarnings", "value", "value")),
          java(
            """
              @SuppressWarnings({"foo", "bar"})
              class A {}
              """,
            """
              @SuppressWarnings(value = {"foo", "bar"})
              class A {}
              """
          )
        );
    }

    @Test
    void rewriteEnumValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName("org.example.Foo", "value", "bar")),
          java(
            """
              package org.example;
                            
              public enum E { A, B }
              """
          ),
          java(
            """
              package org.example;

              public @interface Foo {
                  E value() default E.A;
                  E bar() default E.A;
              }
              """
          ),
          java(
            """
              import org.example.E;
              import org.example.Foo;
                            
              @Foo(E.B)
              class A {}
              """,
            """
              import org.example.E;
              import org.example.Foo;
                            
              @Foo(bar = E.B)
              class A {}
              """
          )
        );
    }
}