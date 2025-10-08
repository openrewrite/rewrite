package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MinimumViableSpacingVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().dependsOn("""
            package com.example;
            
            public class MyObject {
                public static Builder builder() { return new Builder(); }
                public static Builder newBuilder() { return new Builder(); }
                public static class Builder {
                    Builder name(String n) { return this; }
                    Builder age(int a) { return this; }
                    Builder items(java.util.List<String> items) { return this; }
                    Builder nested(MyObject nested) { return this; }
                    MyObject build() { return new MyObject(); }
                }
            }
            """))
          .recipe(toRecipe(() -> new MinimumViableSpacingVisitor<>(null)));
    }

    @Test
    void reformatChainedMethodInvocationToSingleLine() {
        rewriteRun(
          java(
            """
            package com.example;
            class Test {
                void test() {
                    MyObject myObject = MyObject.builder()
                        .

                        name("John");
                }
            }
            """,
            """
            package com.example;
            class Test {
                void test() {
                    MyObject myObject = MyObject.builder()
                        .name("John");
                }
            }
            """
          )
        );
    }

    @Test
    void doNotReformatChainedMethodInvocationToSingleLineWhenCommentInPrefixOfName() {
        rewriteRun(
          java(
            """
            package com.example;
            class Test {
                void test() {
                    MyObject myObject = MyObject.builder()
                        . //Some comment
                        name("John");
                }
            }
            """
          )
        );
    }
}