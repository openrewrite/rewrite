package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class RelocateSuperCallTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RelocateSuperCall())
          .allSources(src -> src.markers(javaVersion(25)));

    }
    @Test
    void moveSuperAfterIf() {
        rewriteRun(
          java(
            """
            class A {
                public A(String bar) {
                    super();
                    if(bar.equals("test"))
                        throw new RuntimeException();
                }
            }
            """,
            """
            class A {
                public A(String bar) {
                    if(bar.equals("test"))
                        throw new RuntimeException();
                    super();
                }
            }
            """
          )
        );
    }

    @Test
    void moveSuperAfterIfStatement() {
        rewriteRun(
          java(
            // language=java
            """
            class Person {
                final int age;
                public Person(int age) {
                    if (age < 0) {
                        throw new IllegalArgumentException("Invalid age");
                    }
                    this.age = age;
                }
            }
            
            class Employee extends Person {
                public Employee(int age) {
                    super(age);
                    if (age < 18 || age > 67) {
                        throw new IllegalArgumentException("Invalid employee age");
                    }
                }
            }
            """,
            // Expected output
            """
            class Person {
                final int age;
                public Person(int age) {
                    if (age < 0) {
                        throw new IllegalArgumentException("Invalid age");
                    }
                    this.age = age;
                }
            }
            
            class Employee extends Person {
                public Employee(int age) {
                    if (age < 18 || age > 67) {
                        throw new IllegalArgumentException("Invalid employee age");
                    }
                    super(age);
                }
            }
            """
          )
        );
    }

    @Test
    void moveSuperAfterIf_withJdkBelow25Version() {
        rewriteRun(
          spec -> spec.recipe(new RelocateSuperCall())
            .allSources(src -> src.markers(javaVersion(8))),
          java(
            """
            class A {
                public A(String bar) {
                    super();
                    if(bar.equals("test"))
                        throw new RuntimeException();
                }
            }
            """,
            spec -> spec.markers(javaVersion(8))
          )
        );
    }
}
