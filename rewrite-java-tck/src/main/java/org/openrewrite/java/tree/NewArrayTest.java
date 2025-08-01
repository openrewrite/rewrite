/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NewArrayTest implements RewriteTest {

    @Test
    void newArray() {
        rewriteRun(
          java(
            """
              class Test {
                  int[] n = new int[0];
              }
              """
          )
        );
    }

    @Test
    void initializers() {
        rewriteRun(
          java(
            """
              class Test {
                  int[] n = new int[] { 0, 1, 2 };
              }
              """
          )
        );
    }

    @Test
    void initializersWithTrailingComma() {
        rewriteRun(
          java(
            """
              class Test {
                  int[] n = new int[] { 0, 1, 2, };
              }
              """
          )
        );
    }

    @Test
    void dimensions() {
        rewriteRun(
          java(
            """
              class Test {
                  int[][] n = new int [ 0 ] [ 1 ];
              }
              """
          )
        );
    }

    @Test
    void emptyDimension() {
        rewriteRun(
          java(
            """
              class Test {
                  int[][] n = new int [ 0 ] [ ];
              }
              """
          )
        );
    }

    @Test
    void newArrayShortcut() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @Target({ElementType.TYPE})
              public @interface Produces {
                  String[] value() default "*/*";
              }

              @Produces({"something"}) class A {}
              """
          )
        );
    }
}
