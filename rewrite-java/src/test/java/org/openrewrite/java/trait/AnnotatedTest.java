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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.trait.Traits.annotated;

class AnnotatedTest implements RewriteTest {

    @Nested
    class PrimitiveValues {
        @Test
        void attributes() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example").asVisitor(a -> SearchResult.found(a.getTree(),
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
        void match() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=\"test\")").asVisitor(a -> SearchResult.found(a.getTree())))),
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
                  @Example(name = "test")
                  @Example(name = "other")
                  class Test {
                  }
                  """,
                """
                  /*~~>*/@Example(name = "test")
                  @Example(name = "other")
                  class Test {
                  }
                  """
              )
            );
        }

        @Test
        void withField() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=Test.TEST)").asVisitor(a -> SearchResult.found(a.getTree())))),
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
                  @Example(name = Test.TEST)
                  @Example(name = "other")
                  class Test {
                      static final String TEST = "test";
                  }
                  """,
                """
                  /*~~>*/@Example(name = Test.TEST)
                  @Example(name = "other")
                  class Test {
                      static final String TEST = "test";
                  }
                  """
              )
            );
        }
    }

    @Nested
    class ClassValues {
        @Test
        void normalClass() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=FindMe.class)").asVisitor(a -> SearchResult.found(a.getTree())))),
              java(
                """
                  import java.lang.annotation.Repeatable;
                  @Repeatable
                  @interface Example {
                      String value() default "";
                      Class<?> name() default "";
                  }
                  class FindMe {}
                  class Other {}
                  """
              ),
              java(
                """
                  @Example(name = FindMe.class)
                  @Example(name = Other.class)
                  class Test {
                  }
                  """,
                """
                  /*~~>*/@Example(name = FindMe.class)
                  @Example(name = Other.class)
                  class Test {
                  }
                  """
              )
            );
        }

        @Test
        void innerCass() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=Example.FindMe.class)").asVisitor(a -> SearchResult.found(a.getTree())))),
              java(
                """
                  import java.lang.annotation.Repeatable;
                  @Repeatable
                  @interface Example {
                      String value() default "";
                      Class<?> name() default "";
                      class FindMe {}
                      class Other{}
                  }
                  """
              ),
              java(
                """
                  @Example(name = Example.FindMe.class)
                  @Example(name = Example.Other.class)
                  class Test {
                  }
                  """,
                """
                  /*~~>*/@Example(name = Example.FindMe.class)
                  @Example(name = Example.Other.class)
                  class Test {
                  }
                  """
              )
            );
        }
    }

    @Nested
    class EnumValues {
        @Test
        void sameFile() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=Values.FindMe)")
                  .asVisitor(a -> SearchResult.found(a.getTree())))),
              java(
                """
                  import java.lang.annotation.Repeatable;
                  @Repeatable
                  @interface Example {
                      String second() default "";
                      Values name() default "";
                  }
                  enum Values {FindMe,OTHER}
                  """
              ),
              java(
                """
                  @Example(name = Values.FindMe, second="test")
                  @Example(name = Values.OTHER)
                  class Test {
                  }
                  """,
                """
                  /*~~>*/@Example(name = Values.FindMe, second="test")
                  @Example(name = Values.OTHER)
                  class Test {
                  }
                  """
              )
            );
        }

        @Test
        void otherFile() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=Values.FindMe)")
                  .asVisitor(a -> SearchResult.found(a.getTree())))),
              java(
                """
                  import java.lang.annotation.Repeatable;
                  @Repeatable
                  @interface Example {
                      String second() default "";
                      Values name() default "";
                  }
                  """
              ),
              java(
                """
                  enum Values {FindMe,OTHER}
                  """
              ),
              java(
                """
                  @Example(name = Values.FindMe, second="test")
                  @Example(name = Values.OTHER)
                  class Test {
                  }
                  """,
                """
                  /*~~>*/@Example(name = Values.FindMe, second="test")
                  @Example(name = Values.OTHER)
                  class Test {
                  }
                  """
              )
            );
        }

        @Test
        void innerEnum() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                annotated("@Example(name=Example.Values.FindMe)")
                  .asVisitor(a -> SearchResult.found(a.getTree())))),
              java(
                """
                  import java.lang.annotation.Repeatable;
                  @Repeatable
                  @interface Example {
                      String second() default "";
                      Values name() default "";
                      enum Values {FindMe,OTHER}
                  }
                  """
              ),
              java(
                """
                  @Example(name = Example.Values.FindMe, second="test")
                  @Example(name = Example.Values.OTHER)
                  class Test {
                  }
                  """,
                """
                  /*~~>*/@Example(name = Example.Values.FindMe, second="test")
                  @Example(name = Example.Values.OTHER)
                  class Test {
                  }
                  """
              )
            );
        }
    }
}
