package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeAnnotationAttributeName;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

public class ChangeAnnotationAttributeNameTest implements RewriteTest {

    @Test
    public void runKotlin() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName(
              "org.junit.jupiter.api.Tag",
              "value",
              "newValue"
            ))
            .parser(KotlinParser.builder().classpath("junit-jupiter-api")),
          kotlin(
            """
              package sample
               
              import org.junit.jupiter.api.Tag
              import org.junit.jupiter.api.Tags
               
              class SampleTest {
               
                  @Tags(
                      value = [
                          Tag(value = "Sample01"),
                          Tag(value = "Sample02"),
                      ]
                  )
                  fun run() {
                  }
              }
              """,
            """
              package sample
               
              import org.junit.jupiter.api.Tag
               import org.junit.jupiter.api.Tags
               
              class SampleTest {
               
                  @Tags(
                      value = [
                          Tag(newValue = "Sample01"),
                          Tag(newValue = "Sample02"),
                      ]
                  )
                  fun run() {
                  }
              }
              """
          )
        );
    }

    // working well
    @Test
    public void runJava() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName(
              "org.junit.jupiter.api.Tag",
              "value",
              "newValue"
            ))
            .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api")),
          java(
            """
                    package sample;
                                                    
                    import org.junit.jupiter.api.Tag;
                    import org.junit.jupiter.api.Tags;
                                                    
                    public class SampleJavaTest {
                                                    
                        @Tags(value = {@Tag(value = "Sample"), @Tag(value = "Sample03")})
                        public void runTest() {
                        }
                    }
                    """,
            """
                    package sample;
                                                    
                    import org.junit.jupiter.api.Tag;
                    import org.junit.jupiter.api.Tags;
                                                    
                    public class SampleJavaTest {
                                                    
                        @Tags(value = {@Tag(newValue = "Sample"), @Tag(newValue = "Sample03")})
                        public void runTest() {
                        }
                    }
                    """
          )
        );
    }
}
