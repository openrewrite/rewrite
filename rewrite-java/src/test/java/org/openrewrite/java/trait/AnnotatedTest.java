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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AnnotatedTest implements RewriteTest {

    @Test
    void attributes() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new Annotated.Matcher("@Example").asVisitor(a -> SearchResult.found(a.getTree(),
              a.getDefaultAttribute("name")
                .map(lit -> lit.getValue(String.class))
                .orElse("unknown"))
            )
          )),
          java(
            """
              import java.lang.annotation.Repeatable;
              @Repeatable
              @interface Example {
                  String value() default "";
                  String name() default "";
              }
              """
          ),
          java(
            """
              @Example("test")
              @Example(value = "test")
              @Example(name = "test")
              class Test {
              }
              """,
            """
              /*~~(test)~~>*/@Example("test")
              /*~~(test)~~>*/@Example(value = "test")
              /*~~(test)~~>*/@Example(name = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void checkOnArray() {
        rewriteRun(
          spec ->
            spec.recipe(RewriteTest.toRecipe(() ->
              new Annotated.Matcher("@Example(other=\"World\")")
                .asVisitor(a -> SearchResult.found(a.getTree()))
            )),
          java(
            //language=java
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Target(ElementType.TYPE)
              @Retention(RetentionPolicy.RUNTIME)
              @interface Example {
                  String[] other;
              }
              """
          ),
          java(
            //language=java
            """
              @Example(other = {"Hello", "World"})
              class Test {
              }
              """,
            //language=java
            """
              /*~~>*/@Example(other = {"Hello", "World"})
              class Test {
              }
              """
          )
        );
    }
}
