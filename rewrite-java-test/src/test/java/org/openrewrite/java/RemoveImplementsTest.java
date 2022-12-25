/*
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveImplementsTest implements RewriteTest {

    @Test
    void removeBasic() {
        rewriteRun(
          spec -> spec.recipe(new RemoveImplements("java.io.Serializable", null)),
          java(
            """
              import java.io.Serializable;
              
              class A implements Serializable {
              }
              """,
            """
              class A {
              }
              """
          )
        );
    }

    @Test
    void preservesOtherInterfaces() {
        rewriteRun(
          spec -> spec.recipe(new RemoveImplements("java.io.Serializable", "")),
          java(
            """
              import java.io.Closeable;
              import java.io.Serializable;
              
              class A implements Serializable, Closeable {
              }
              """,
            """
              import java.io.Closeable;
              
              class A implements Closeable {
              }
              """
          )
        );
    }

    @Test
    void removeFullyQualifiedInterface() {
        rewriteRun(
          spec -> spec.recipe(new RemoveImplements("java.io.Serializable", null)),
          java(
            """
              class A implements java.io.Serializable {
              }
              """,
            """
              class A {
              }
              """
          )
        );
    }


    @Test
    void innerClassOnly() {
        rewriteRun(
          spec -> spec.recipe(new RemoveImplements("java.io.Serializable", "com.yourorg.Outer$Inner")),
          java(
            """
              package com.yourorg;
              
              import java.io.Serializable;
              
              class Outer implements Serializable {
                  class Inner implements Serializable {
                  }
              }
              """,
            """
              package com.yourorg;
              
              import java.io.Serializable;
              
              class Outer implements Serializable {
                  class Inner {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void removeOverrideFromMethods() {
        rewriteRun(
          spec -> spec.recipe(new RemoveImplements("java.io.Closeable", null)),
          java(
            """
              import java.io.Closeable;
              import java.io.IOException;
              
              class A implements Closeable {
                  @Override
                  public void close() throws IOException {}
              }
              """,
            """
              import java.io.IOException;
              
              class A {
                  public void close() throws IOException {}
              }
              """
          )
        );
    }
}
