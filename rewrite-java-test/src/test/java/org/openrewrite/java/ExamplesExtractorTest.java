package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.AdHocRecipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class ExamplesExtractorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        Recipe recipe = toRecipe(() -> new ExamplesExtractor());
        spec.recipe(recipe)
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }


    @Test
    void extractJavaExample() {
        rewriteRun(
          java(
            """
            import org.junit.jupiter.api.Test;
            import org.openrewrite.internal.DocumentExample;
            import org.openrewrite.test.RecipeSpec;
            import org.openrewrite.test.RewriteTest;
            
            import static org.openrewrite.java.Assertions.java;
            
            class ChainStringBuilderAppendCallsTest implements RewriteTest {
                @Override
                public void defaults(RecipeSpec spec) {
                    spec.recipe(new ChainStringBuilderAppendCalls());
                }
            
                @DocumentExample
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
    }


}
