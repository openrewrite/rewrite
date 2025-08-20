/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@MinimumJava17
class RecordTest implements RewriteTest {

    @Test
    void emptyRecord() {
        rewriteRun(
          java(
            """
              public record JavaRecord() {
              }
              """
          )
        );
    }

    @Test
    void javaRecord() {
        rewriteRun(
          java(
            """
              public record JavaRecord(String name, @jdk.jfr.Name("A") @Deprecated int age) {
              }
              """
          )
        );
    }

    @Test
    void typeParameterAnnotation() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              
              import static java.lang.annotation.ElementType.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(PARAMETER)
              public @interface A {
                  String value() default "";
              }
              """
          ),
          java(
            """
              record JavaRecord(@jdk.jfr.Name("A") @A("one value") String name) {
              }
              """
          )
        );
    }

    @Test
    void differentLiteralTypesAnnotation() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              
              import static java.lang.annotation.ElementType.*;
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target({PARAMETER})
              public @interface A {
                  String value() default "";
                  Long a() default 0L;
                  int b() default 0;
              }
              """
          ),
          java(
            """
              record JavaRecord(@jdk.jfr.Name("A") @A(value = ""\"
                  one value "with a quote" and
                  another value
                  ""\", a=2_000L, b=123) String name) {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2455")
    @Test
    void compactConstructor() {
        rewriteRun(
          java(
            """
              public record JavaRecord(String name, @Deprecated int age) {
                  public JavaRecord {
                      java.util.Objects.requireNonNull(name);
                  }
              }
              """
          )
        );
    }
}
