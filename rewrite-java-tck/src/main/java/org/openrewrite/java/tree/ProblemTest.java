package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class ProblemTest implements RewriteTest {

    @Test
    void annotationsWithComments() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @Yo
              // doc
              @Ho
              public @Yo /* grumpy */ @Ho final @Yo
              // happy
              @Ho class Test {
                  @Yo /* sleepy */ @Ho private @Yo /* bashful */ @Ho transient @Yo /* sneezy */ @Ho String s;
                  @Yo /* dopey */ @Ho
                  public @Yo /* evil queen */ @Ho final @Yo /* mirror */ @Ho <T> @Yo /* apple */ @Ho T itsOffToWorkWeGo() {
                      return null;
                  }
                  @Yo /* snow white */ @Ho
                  public @Yo /* prince */ @Ho Test() {
                  }
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface Hos {
                  Ho[] value();
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @Repeatable(Hos.class)
              @interface Ho {
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface Yos {
                  Yo[] value();
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @Repeatable(Yos.class)
              @interface Yo {
              }
              """
          )
        );
    }
}
