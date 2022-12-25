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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindEmptyMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindEmptyMethods(false));
    }

    @Test
    void methodNotEmpty() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int x = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void abstractClass() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void containsComment() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      // comment
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMatchOverride() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              
              class Test implements Collection<String> {
                  @Override
                  public boolean isEmpty() {
                  }
              }
              """
          )
        );
    }

    @Test
    void matchOverride() {
        rewriteRun(
          spec -> spec.recipe(new FindEmptyMethods(true)),
          java(
            """
              import java.util.Collection;
              
              class Test implements Collection<String> {
                  @Override
                  public boolean isEmpty() {
                  }
              }
              """,
            """
              import java.util.Collection;
              
              class Test implements Collection<String> {
                  /*~~>*/@Override
                  public boolean isEmpty() {
                  }
              }
              """
          )
        );
    }

    @Test
    void singleNoArgConstructor() {
        rewriteRun(
          java(
            """
              class Test {
                  public Test() {
                  }
              }
              """,
            """
              class Test {
                  /*~~>*/public Test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyMethod() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                  }
              }
              """,
            """
              class Test {
                  /*~~>*/void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void interfaceMethods() {
        rewriteRun(
          java(
            """
              interface MyInterface {
                  void doSomething();
                  default Integer doSomethingElse(Integer i) {}
                  default Integer doSomethingElseAgain(Integer i) {
                      return i + 1;
                  }
              }
              """,
            """
              interface MyInterface {
                  void doSomething();
                  /*~~>*/default Integer doSomethingElse(Integer i) {}
                  default Integer doSomethingElseAgain(Integer i) {
                      return i + 1;
                  }
              }
              """
          )
        );
    }
}
