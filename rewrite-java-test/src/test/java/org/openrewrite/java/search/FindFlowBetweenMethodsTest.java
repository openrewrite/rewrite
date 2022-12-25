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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("FunctionName")
class FindFlowBetweenMethodsTest implements RewriteTest {

    @Test
    void taintFlowBetweenSubjectOnly() {
        rewriteRun(
          spec -> spec.recipe(new FindFlowBetweenMethods("java.util.LinkedList <constructor>()", true, "java.util.LinkedList remove()", true, "Select", "Taint")),
          java(
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      LinkedList<Integer> l = new LinkedList<>();
                      l.add(5);
                      System.out.println(l);
                      l.remove();
                  }
              }
              """,
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      LinkedList<Integer> l = /*~~>*/new LinkedList<>();
                      /*~~>*/l.add(5);
                      System.out.println(/*~~>*/l);
                      /*~~>*/l.remove();
                  }
              }
              """
          )
        );
    }

    @Test
    void taintFlowBetweenArgumentsOnly() {
        rewriteRun(
          spec -> spec.recipe(new FindFlowBetweenMethods("java.lang.Integer parseInt(String)", true, "java.util.LinkedList remove(..)", true, "Arguments", "Taint")),
          java(
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = Integer.parseInt("10");
                      LinkedList<Integer> l = new LinkedList<>();
                      l.add(x);
                      l.remove(x);
                  }
              }
              """,
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = /*~~>*/Integer.parseInt("10");
                      LinkedList<Integer> l = new LinkedList<>();
                      l.add(/*~~>*/x);
                      l.remove(/*~~>*/x);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void taintFlowThroughMultipleSubjectsIntegerSourceAndSinkMethodsSpecified() {
        rewriteRun(
          spec -> spec.recipe(new FindFlowBetweenMethods("java.lang.Integer parseInt(String)", true, "java.lang.Integer equals(..)", true, "Both", "Taint")),
          java(
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = Integer.parseInt("10");
                      LinkedList<Integer> l = new LinkedList<>();
                      l.add(x);
                      System.out.println(l);
                      x.equals(10);
                  }
              }
              """,
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = /*~~>*/Integer.parseInt("10");
                      LinkedList<Integer> l = new LinkedList<>();
                      l.add(/*~~>*/x);
                      System.out.println(l);
                      /*~~>*/x.equals(10);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void noTaintFlowThroughArguments() {
        rewriteRun(
          spec -> spec.recipe(new FindFlowBetweenMethods("java.lang.Integer parseInt(String)", true, "java.lang.Integer equals(..)", true, "Arguments", "Taint")),
          java(
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = Integer.parseInt("10");
                      x.equals(10);
                  }
              }
              """
          )
        );
    }

    @Test
    void taintFlowBetweenArgumentsAndSubject() {
        rewriteRun(
          spec -> spec.recipe(new FindFlowBetweenMethods("java.util.LinkedList <constructor>()", true, "java.util.LinkedList remove()", true, "Both", "Taint")),
          java(
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = Integer.parseInt("10");
                      LinkedList<Integer> l = new LinkedList<>();
                      System.out.println(x);
                      System.out.println(l);
                      l.remove();
                  }
              }
              """,
            """
              import java.util.LinkedList;
              class Test {
                  void test() {
                      Integer x = Integer.parseInt("10");
                      LinkedList<Integer> l = /*~~>*/new LinkedList<>();
                      System.out.println(x);
                      System.out.println(/*~~>*/l);
                      /*~~>*/l.remove();
                  }
              }
              """
          )
        );
    }
}
