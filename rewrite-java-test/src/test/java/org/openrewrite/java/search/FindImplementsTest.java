package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindImplementsTest implements RewriteTest {

    @Test
    void found() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Runnable")),
          java(
            """
              class Test implements Runnable {
                  @Override
                  public void run() {
                  }
              }
              """,
            """
              /*~~>*/class Test implements Runnable {
                  @Override
                  public void run() {
                  }
              }
              """
          )
        );
    }

    @Test
    void notFound() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Runnable")),
          java(
            """
              class Test  {
                  public void run() {
                  }
              }
              """
          )
        );
    }

    @Test
    void genericType() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Comparable<java.lang.String>")),
          java(
            """
              class Test implements Comparable<String> {
                  @Override
                  public int compareTo(String o) {
                      return 0;
                  }
              }
              """,
            """
              /*~~>*/class Test implements Comparable<String> {
                  @Override
                  public int compareTo(String o) {
                      return 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void unmatchedGenericType() {
        rewriteRun(
          spec -> spec.recipe(new FindImplements("java.lang.Comparable<java.lang.Runnable>")),
          java(
            """
              class Test implements Comparable<String> {
                  @Override
                  public int compareTo(String o) {
                      return 0;
                  }
              }
              """
          )
        );
    }
}
