/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveMethodThrowsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveMethodThrows("A foo(..)", "java.io.IOException",
          true));
    }

    @Test
    @DocumentExample
    void removeSingleException() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.IOException;
              
              class A {
                  public void foo() throws IOException {
                      // no-op
                  }
              }
              """
            ,
            """
              class A {
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void removeSingleExceptionOverrides() {
        rewriteRun(
          spec -> spec.recipe(new RemoveMethodThrows("Itf foo(..)", "java.io.IOException",
            true)),
          //language=java
          java(
            """
              import java.io.IOException;
              
              interface Itf {
                  void foo();
              }
              
              class A implements Itf {
                  @Override
                  public void foo() throws IOException {
                      // no-op
                  }
              }
              """
            ,
            """
              interface Itf {
                  void foo();
              }
              
              class A implements Itf {
                  @Override
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void removeSingleExceptionCascadingOverrides() {
        rewriteRun(
          spec -> spec.recipe(new RemoveMethodThrows("ItfA foo(..)", "java.lang.Exception",
            true)),
          //language=java
          java(
            """
              interface ItfA {
                  void foo() throws Exception;
              }
              
              abstract class AbsB implements ItfA {
                  @Override
                  public void foo() throws Exception {
                      // no-op
                  }
              }

              class C extends AbsB {
                  @Override
                  public void foo() throws Exception {
                      // no-op
                  }
              }
              """
            ,
            """
              interface ItfA {
                  void foo();
              }
              
              abstract class AbsB implements ItfA {
                  @Override
                  public void foo() {
                      // no-op
                  }
              }

              class C extends AbsB {
                  @Override
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void removeExceptionWithMultipleExceptions() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  public void foo() throws java.io.IOException, java.lang.IllegalArgumentException {
                      // no-op
                  }
              }
              """
            ,
            """
              class A {
                  public void foo() throws java.lang.IllegalArgumentException {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void removeAllExceptions() {
        rewriteRun(
          spec -> spec.recipe(new RemoveMethodThrows("A foo(..)", "*",
            false)),
          //language=java
          java(
            """
              import java.io.IOException;
              import java.lang.IllegalArgumentException;
              
              class A {
                  public void foo() throws IOException, IllegalArgumentException {
                      // no-op
                  }
              }
              """
            ,
            """
              class A {
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void noThrows() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void noMatchingExceptionInThrows() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  public void foo() throws java.lang.IllegalArgumentException {
                      // no-op
                  }
              }
              """
          ));
    }

}
