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
package org.openrewrite.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Disabled
class ReplaceAnnotationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("lombok", "annotations"));
    }

    @Nested
    class OnMatch {
        @Test
        void matchNoPrams() {
            rewriteRun(spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull")), java("""
              import org.jetbrains.annotations.NotNull;
                                
              class A {
                  @NotNull
                  String testMethod() {}
              }
              """, """
              import lombok.NonNull;
                              
              class A {
                  @NonNull
                  String testMethod() {}
              }
              """));
        }

        @Test
        @DocumentExample
        void matchWithPrams() {
            rewriteRun(spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull(\"Test\")", "@lombok.NonNull")), java("""
              import org.jetbrains.annotations.NotNull;
                                
              class A {
                  @NotNull("Test")
                  String testMethod() {}
              }
              """, """
              import lombok.NonNull;
                              
              class A {
                  @NonNull
                  String testMethod() {}
              }
              """));
        }

        @Test
        void insertWithParams() {
            rewriteRun(spec -> spec.recipe(new ReplaceAnnotation("@lombok.NonNull", "@org.jetbrains.annotations.NotNull(\"Test\")")), java("""
              import lombok.NonNull;
                                
              class A {
                  @NonNull
                  String testMethod() {}
              }
              """, """
              import org.jetbrains.annotations.NotNull;
                              
              class A {
                  @NotNull("Test")
                  String testMethod() {}
              }
              """));
        }
    }

    @Nested
    class NoMatch {
        @Test
        void noMatchOtherType() {
            rewriteRun(spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull")), java("""
              import org.jetbrains.annotations.Nullable;
                                
              class A {
                  @Nullable("Test")
                  String testMethod() {}
              }
              """));
        }

        @Test
        void noMatchParameter() {
            rewriteRun(spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull(\"Test\")", "@lombok.NonNull")), java("""
              import org.jetbrains.annotations.Nullable;
                                
              class A {
                  @Nullable("Other")
                  String testMethod() {}
              }
              """));
        }
    }
}
