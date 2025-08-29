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
package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveAnnotationAttributeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveAnnotationAttribute("TestAnnotation", "deletedAttribute"))
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              @interface SomeAnnotation {
                    String value();
                    String attributeX() default "";
              }
              
              @interface TestAnnotation {
                    String attributeA();
                    String deletedAttribute() default "";
                    String value() default "";
              }
              """
          ));
    }

    @Nested
    class SingleAnnotation {
        @DocumentExample
        @Test
        void withTwoAttributesOnSingleLine() {
            rewriteRun(
              java(
                """
                  @TestAnnotation(attributeA = "attributeValue", deletedAttribute = "deletedAttributeValue")
                  class SomeClass {}
                  """,
                """  
                  @TestAnnotation(attributeA = "attributeValue")
                  class SomeClass {}
                  """
              )
            );
        }

        @Test
        void withTwoAttributesSplitAcrossLines() {
            rewriteRun(
              java(
                """
                  @TestAnnotation(
                    attributeA = "attributeValue",
                    deletedAttribute = "deletedAttributeValue"
                  )
                  class SomeClass {}
                  """,
                """  
                  @TestAnnotation(
                    attributeA = "attributeValue")
                  class SomeClass {}
                  """
              )
            );
        }

        @Test
        void withSingleAttribute() {
            rewriteRun(
              java(
                """
                  @TestAnnotation(deletedAttribute = "deletedAttributeValue")
                  class SomeClass {}
                  """,
                """  
                  @TestAnnotation
                  class SomeClass {}
                  """
              )
            );
        }

        @Test
        void withValueAttribute() {
            rewriteRun(
              java(
                """
                  @TestAnnotation(value = "attributeValue", deletedAttribute = "deletedAttributeValue")
                  class SomeClass {}
                  """,
                """  
                  @TestAnnotation("attributeValue")
                  class SomeClass {}
                  """
              )
            );
        }
    }


    @Issue("https://github.com/openrewrite/rewrite/pull/5976")
    @Nested
    class TwoAnnotations {
        @Test
        void unrelatedAnnotationWithValueAttribute() {
            rewriteRun(
              java(
                """
                  @SomeAnnotation(value = "attributeValue")
                  @TestAnnotation(attributeA = "attributeValue", deletedAttribute = "deletedAttributeValue")
                  class SomeClass {}
                  """,
                """  
                  @SomeAnnotation(value = "attributeValue")
                  @TestAnnotation(attributeA = "attributeValue")
                  class SomeClass {}
                  """
              )
            );
        }

        @Test
        void bothWithValueAttribute() {
            rewriteRun(
              java(
                """
                  @SomeAnnotation(value = "attributeValue")
                  @TestAnnotation(value = "attributeValue", deletedAttribute = "deletedAttributeValue")
                  class SomeClass {}
                  """,
                """  
                  @SomeAnnotation(value = "attributeValue")
                  @TestAnnotation("attributeValue")
                  class SomeClass {}
                  """
              )
            );
        }
    }
}
