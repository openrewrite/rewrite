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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class InstanceOfTest implements RewriteTest {

    @Test
    void instanceOf() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(Object o) {
                      boolean b = o instanceof String;
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void patternMatch() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                public void match(Collection<?> c) {
                      if (c instanceof List<?> l) {
                          System.out.println("List");
                      } else if (c instanceof Set<?> s) {
                          System.out.println("Set");
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaVisitor<Integer>() {
                @Override
                public J visitInstanceOf(J.InstanceOf instanceOf, Integer integer) {
                    assertThat(instanceOf.getPattern()).isNotNull();
                    return instanceOf;
                }
            }.visit(cu, 0))
          )
        );
    }
}
