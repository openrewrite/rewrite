package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ConstructorTest implements RewriteTest {

    @Test
    void noConstructor() {
        rewriteRun(
          java(
            """
              public class A {}
              """
          )
        );
    }

    @Test
    void defaultConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
              }
              """
          )
        );
    }

    @Test
    void multipleConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {}
              }
              """
          )
        );
    }

    @Test
    void thisCallingConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      this();
                  }
              }
              """
          )
        );
    }

    @Test
    void superCallingConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      super();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1144")
    @Test
    void validationBeforeThisConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() {}
                  public A(String a) {
                      if (a.equals("foo")) {
                          throw new RuntimeException();
                      }
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1144")
    @Test
    void validationBeforeSuperConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A(String a) {
                      if (a.equals("foo")) {
                          throw new RuntimeException();
                      }
                      super();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1144")
    @Test
    void assignmentBeforeThisConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  String stringA;
                  public A() {}
                  public A(String a) {
                      stringA = a;
                      this();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1144")
    @Test
    void assignmentBeforeSuperConstructor() {
        rewriteRun(
          java(
            """
              public class A {
                  String stringA;
                  public A(String a) {
                      stringA = a;
                      super();
                  }
              }
              """
          )
        );
    }
}
