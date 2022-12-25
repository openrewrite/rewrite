/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup;/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"RedundantThrows", "resource"})
class UnnecessaryThrowsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UnnecessaryThrows());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2144")
    @Test
    void genericException() {
        rewriteRun(
          java(
            """
              class Test {
                  public <E extends Exception> void accept(Class<E> e) throws E {
                  }
              }
              """
          )
        );
    }

    @Test
    void unnecessaryThrows() {
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.FileNotFoundException;
              import java.io.IOException;
              import java.io.UncheckedIOException;
              class Test {
                  private void test() throws FileNotFoundException, UncheckedIOException {
                  }

                  void test() throws IOException, UncheckedIOException {
                      new FileInputStream("test");
                  }
              }
              """,
            """
              import java.io.FileInputStream;
              import java.io.IOException;
              import java.io.UncheckedIOException;
              class Test {
                  private void test() throws UncheckedIOException {
                  }
                            
                  void test() throws IOException, UncheckedIOException {
                      new FileInputStream("test");
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("EmptyTryBlock")
    @Issue("https://github.com/openrewrite/rewrite/issues/631")
    @Test
    void necessaryThrowsFromCloseable() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.net.URL;
              import java.net.URLClassLoader;
                            
              class Test {
                  public void closeable() throws IOException {
                      // URLClassLoader implements Closeable and throws IOException from its close() method
                      try (URLClassLoader cl = new URLClassLoader(new URL.get(0))) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void necessaryThrows() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
                            
              class Test {
                  
                  void test() throws IOException {
                      throw new IOException();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/519")
    @Test
    void interfaces() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
                            
              interface Test {
                  void test() throws IOException;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/519")
    @Test
    void abstractMethods() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
                            
              abstract class Test {
                  abstract void test() throws IOException;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1059")
    @Test
    void necessaryThrowsFromStaticMethod() {
        rewriteRun(
          java(
            """
              import javax.xml.datatype.DatatypeFactory;
                            
              class Test {
                  void test() throws Exception {
                      DatatypeFactory.newInstance();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/897")
    @Test
    void necessaryThrowsOnInterfaceWithExplicitOverride() {
        rewriteRun(
          java(
            """
              public interface Foo {
                  void bar() throws Exception;
              }
              """
          ),
          java(
            """
              public class FooImpl implements Foo {
                  @Override
                  public void bar() throws Exception {
                      // no-op
                  }
              }
              """
          )
        );
    }

    @Test
    void necessaryThrowsOnInterfaceWithImplicitOverride() {
        rewriteRun(
          java(
            """
              public interface Foo {
                  void bar() throws Exception;
              }
              """
          ),
          java(
            """
              public class FooImpl implements Foo {
                  public void bar() throws Exception {
                      // no-op
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveDocumentedExceptions() {
        rewriteRun(
          java(
            """
              public class ParentFoo {
                  /**
                   * @throws Exception Throws an exception
                   */
                  public void bar() throws Exception { // this throws should not be removed
                  }
              }
                            
              class Foo extends ParentFoo {
                  @Override
                  public void bar() throws Exception {
                      // no-op
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1298")
    @Test
    void doNotRemoveExceptionCoveringOtherExceptions() {
        rewriteRun(
          java(
            """
              package com.yourorg;
                            
              import java.io.IOException;
                            
              class A {
                  void foo() throws Exception {
                      throw new IOException("");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2105")
    @Test
    void preventTransformationIfAnyThrownExceptionHasNullOrUnknownType() {
        rewriteRun(
          java(
            """
              package com.yourorg;
                            
              import java.io.IOException;
                            
              class A {
                  void foo() throws ExceptionWithUnknownType {
                      someUnknownMethodInvocation();
                  }
              }
              """
          )
        );
    }
}
