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

@SuppressWarnings({"RedundantOperationOnEmptyContainer", "ResultOfMethodCallIgnored", "Convert2MethodRef"})
class LambdaTest implements RewriteTest {

    @Test
    void lambda() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;
              class Test {
                  void test() {
                      Function<String, String> func = (String s) -> "";
                  }
              }
              """
          )
        );
    }

    @Test
    void untypedLambdaParameter() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      list.stream().filter(s -> s.isEmpty());
                  }
              }
              """
          )
        );
    }

    @Test
    void optionalSingleParameterParentheses() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      list.stream().filter((s) -> s.isEmpty());
                  }
              }
              """
          )
        );
    }

    @Test
    void rightSideBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  Action a = ( ) -> { };
              }

              interface Action {
                  void call();
              }
              """
          )
        );
    }

    @Test
    void multipleParameters() {
        rewriteRun(
          java(
            """
              import java.util.function.BiConsumer;
              class Test {
                  void test() {
                      BiConsumer<String, String> a = (s1, s2) -> { };
                  }
              }
              """
          )
        );
    }
}
