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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceAnnotationTest implements RewriteTest {

    @SuppressWarnings("NullableProblems")
    @Nested
    class OnMatch {

        @Test
        @DocumentExample
        void matchWithPrams() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull(\"Test\")", "@lombok.NonNull", null)),
              java(
                """
                  import org.jetbrains.annotations.NotNull;
                 
                  class A {
                      @NotNull("Test")
                      String testMethod() {}
                  }
                  """,
                """
                  import lombok.NonNull;
                  
                  class A {
                      @NonNull
                      String testMethod() {}
                  }
                  """
              )
            );
        }
        @Test
        void matchNoPrams() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull", null)),
              java(
                """
                  import org.jetbrains.annotations.NotNull;
                  
                  class A {
                      @NotNull
                      String testMethod() {}
                  }
                  """,
                """
                  import lombok.NonNull;
                  
                  class A {
                      @NonNull
                      String testMethod() {}
                  }
                  """
              )
            );
        }

        @Test
        void insertWithParams() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceAnnotation("@lombok.NonNull", "@org.jetbrains.annotations.NotNull(\"Test\")", null)),
              java(
                """
                  import lombok.NonNull;
                  
                  class A {
                      @NonNull
                      String testMethod() {}
                  }
                  """,
                """
                  import org.jetbrains.annotations.NotNull;
                  
                  class A {
                      @NotNull("Test")
                      String testMethod() {}
                  }
                  """
              )
            );
        }

        @Test
        @Issue("https://github.com/openrewrite/rewrite/issues/4441")
        void methodWithAnnotatedParameter() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull", null)),
              java(
                """
                  import org.jetbrains.annotations.NotNull;
                  import org.jetbrains.annotations.Nullable;
                  
                  class A {
                      void methodName(
                          @Nullable final boolean valueVar) {
                          @NotNull final String nullableVar = "test";
                      }
                  }
                  """,
                """
                  import lombok.NonNull;
                  import org.jetbrains.annotations.Nullable;
                  
                  class A {
                      void methodName(
                          @Nullable final boolean valueVar) {
                          @NonNull final String nullableVar = "test";
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class NoMatch {
        @Test
        void noMatchOtherType() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull", null)),
              java(
                """
                  import org.jetbrains.annotations.Nullable;
                  
                  class A {
                      @Nullable("Test")
                      String testMethod() {}
                  }
                  """
              )
            );
        }

        @Test
        void noMatchParameter() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull(\"Test\")", "@lombok.NonNull", null)),
              java(
                """
                  import org.jetbrains.annotations.Nullable;
                  
                  class A {
                      @Nullable("Other")
                      String testMethod() {}
                  }
                  """
              )
            );
        }
    }
}
