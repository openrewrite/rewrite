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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeMethodTargetToVariableTest implements RewriteTest {

    @Test
    void explicitStaticToVariable() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToVariable("b.B foo()", "a", "a.A", null)),
          java(
            """ 
              package a;
              public class A {
                 public void foo() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void foo() {}
              }
              """
          ),
          java(
            """
              import a.*;
                            
              import b.B;
              public class C {
                 A a;
                 public void test() {
                     B.foo();
                 }
              }
              """,
            """
              import a.*;
              public class C {
                 A a;
                 public void test() {
                     a.foo();
                 }
              }
              """
          )
        );
    }

    @Test
    void staticImportToVariable() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToVariable("b.B foo()", "a", "a.A", null)),
          java(
            """
              package a;
              public class A {
                 public void foo() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void foo() {}
              }
              """
          ),
          java(
            """
              import a.*;
              import static b.B.*;
              public class C {
                 A a;
                 public void test() {
                     foo();
                 }
              }
              """,
            """
              import a.*;
              public class C {
                 A a;
                 public void test() {
                     a.foo();
                 }
              }
              """
          )
        );
    }
}
