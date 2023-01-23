package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class InstanceOfPatternMatchTest implements RewriteTest {

    @Test
    void ifConditionWithPattern() {
        rewriteRun(
          spec -> spec.recipe(new InstanceOfPatternMatch()),
          version(
            java(
              """
                public class A {
                    void test(Object o) {
                        if (o instanceof String s && s.length() > 0) {
                            System.out.println(s);
                        }
                    }
                }
                 """
            ), 17)
        );
    }

    @Test
    void ifConditionWithoutPattern() {
        rewriteRun(
          spec -> spec.recipe(new InstanceOfPatternMatch()),
          version(
            java(
              """
                public class A {
                    void test(Object o) {
                        if (o instanceof String && ((String) o).length() > 0) {
                            if (((String) o).length() > 1) {
                                System.out.println(o);
                            }
                        }
                    }
                }
                """,
              """
                public class A {
                    void test(Object o) {
                        if (o instanceof String s && s.length() > 0) {
                            if (s.length() > 1) {
                                System.out.println(o);
                            }
                        }
                    }
                }
                """
            ), 17
          )
        );
    }

    @Test
    void nonApplicableIfCondition() {
        rewriteRun(
          spec -> spec.recipe(new InstanceOfPatternMatch()),
          version(
            java(
              """
                public class A {
                    void test(Object o) {
                        if (o instanceof String || ((String) o).length() > 0) {
                            if (((String) o).length() > 1) {
                                System.out.println(o);
                            }
                        }
                    }
                }
                """
            ), 17
          )
        );
    }
}