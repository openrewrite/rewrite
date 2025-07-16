package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class SwitchEnhancementsTest implements RewriteTest {
    @DocumentExample
    @Test
    void yieldReformated() {
        rewriteRun(
                spec -> spec.recipe(new AutoFormat()),
                java("""
                        class Test {
                            String yielded(int i) {
                                return switch (i) {
                                    default: yield"value";
                                };
                            }
                        }
                        """, """
                        class Test {
                            String yielded(int i) {
                                return switch (i) {
                                    default: yield "value";
                                };
                            }
                        }
                        """));
    }
}
