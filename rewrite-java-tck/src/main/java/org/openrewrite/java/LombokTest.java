package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class LombokTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @Test
    void getter() {
        rewriteRun(
            java(
                """
                import lombok.Getter;
                
                @Getter
                class A {
                    int n;
                
                    void test() {
                        System.out.println(getN());
                    }
                }
                """
            )
        );
    }
}
