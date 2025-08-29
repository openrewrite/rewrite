package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class RemoveAnnotationAttributeTest implements RewriteTest {

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
