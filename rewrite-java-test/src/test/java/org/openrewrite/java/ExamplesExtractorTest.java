package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ExamplesExtractorTest implements RewriteTest {

    @Test
    void extractJavaExampleWithDefault() {
        ExamplesExtractor examplesExtractor = new ExamplesExtractor();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> examplesExtractor))
            .parser(JavaParser.fromJavaVersion()
              .classpath(JavaParser.runtimeClasspath())),
          java(
            """
            package org.openrewrite.java.cleanup;
            
            import org.junit.jupiter.api.Test;
            import org.openrewrite.Recipe;
            import org.openrewrite.internal.DocumentExample;
            import org.openrewrite.test.RecipeSpec;
            import org.openrewrite.test.RewriteTest;
            
            import static org.openrewrite.java.Assertions.java;
            
            class ChainStringBuilderAppendCallsTest implements RewriteTest {
                @Override
                public void defaults(RecipeSpec spec) {
                    Recipe recipe = new ChainStringBuilderAppendCalls();
                    spec.recipe(recipe);
                }

                @DocumentExample(name = "Objects concatenation.")
                @Test
                void objectsConcatenation() {
                    rewriteRun(
                      java(
                        \"""
                                                      class A {
                                                          void method1() {
                                                              StringBuilder sb = new StringBuilder();
                                                              String op = "+";
                                                              sb.append("A" + op + "B");
                                                              sb.append(1 + op + 2);
                                                          }
                                                      }
                                                      \""",
                        \"""
                                                      class A {
                                                          void method1() {
                                                              StringBuilder sb = new StringBuilder();
                                                              String op = "+";
                                                              sb.append("A").append(op).append("B");
                                                              sb.append(1).append(op).append(2);
                                                          }
                                                      }
                                                      \"""
                      )
                    );
                }
            }
            """
          )
        );

        String yaml = examplesExtractor.printRecipeExampleYaml();
        assertThat(yaml).isEqualTo(
          """
            type: specs.openrewrite.org/v1beta/example
            recipeName: org.openrewrite.java.cleanup.ChainStringBuilderAppendCalls
            examples:
            - description: "Objects concatenation."
              before: |
                class A {
                    void method1() {
                        StringBuilder sb = new StringBuilder();
                        String op = "+";
                        sb.append("A" + op + "B");
                        sb.append(1 + op + 2);
                    }
                }
              after: |
                class A {
                    void method1() {
                        StringBuilder sb = new StringBuilder();
                        String op = "+";
                        sb.append("A").append(op).append("B");
                        sb.append(1).append(op).append(2);
                    }
                }
              language: "java"
            """
        );
    }

    @Test
    void extractJavaExampleRecipeInSpec() {
        ExamplesExtractor examplesExtractor = new ExamplesExtractor();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> examplesExtractor))
            .parser(JavaParser.fromJavaVersion()
              .classpath(JavaParser.runtimeClasspath())),
          java(
            """
            package org.openrewrite.java.cleanup;
            
            import org.junit.jupiter.api.Test;
            import org.openrewrite.internal.DocumentExample;
            import org.openrewrite.test.RecipeSpec;
            import org.openrewrite.test.RewriteTest;
            
            import static org.openrewrite.java.Assertions.java;
            
            class ChainStringBuilderAppendCallsTest implements RewriteTest {

                @DocumentExample(name = "Objects concatenation.")
                @Test
                void objectsConcatenation() {
                    rewriteRun(
                      spec -> spec.recipe(new ChainStringBuilderAppendCalls()),
                      java(
                        \"""
                                                      class A {
                                                          void method1() {
                                                              StringBuilder sb = new StringBuilder();
                                                              String op = "+";
                                                              sb.append("A" + op + "B");
                                                              sb.append(1 + op + 2);
                                                          }
                                                      }
                                                      \""",
                        \"""
                                                      class A {
                                                          void method1() {
                                                              StringBuilder sb = new StringBuilder();
                                                              String op = "+";
                                                              sb.append("A").append(op).append("B");
                                                              sb.append(1).append(op).append(2);
                                                          }
                                                      }
                                                      \"""
                      )
                    );
                }
            }
            """
          )
        );
        String yaml = examplesExtractor.printRecipeExampleYaml();
        assertThat(yaml).isEqualTo(
          """
            type: specs.openrewrite.org/v1beta/example
            recipeName: org.openrewrite.java.cleanup.ChainStringBuilderAppendCalls
            examples:
            - description: "Objects concatenation."
              before: |
                class A {
                    void method1() {
                        StringBuilder sb = new StringBuilder();
                        String op = "+";
                        sb.append("A" + op + "B");
                        sb.append(1 + op + 2);
                    }
                }
              after: |
                class A {
                    void method1() {
                        StringBuilder sb = new StringBuilder();
                        String op = "+";
                        sb.append("A").append(op).append("B");
                        sb.append(1).append(op).append(2);
                    }
                }
              language: "java"
            """
        );
    }
}
