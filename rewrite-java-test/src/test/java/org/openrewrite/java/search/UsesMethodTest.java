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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("RedundantOperationOnEmptyContainer")
class UsesMethodTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1169")
    @Test
    void emptyConstructor() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesMethod<>("abc.Thing newConcurrentHashSet()"))),
          java(
            """
              package abc;
                            
              import java.util.Set;
              import java.util.Collections;
              import java.util.concurrent.ConcurrentHashMap;
              public class Thing {
                  public static <E> Set<E> newConcurrentHashSet() {
                      return Collections.newSetFromMap(new ConcurrentHashMap<>());
                  }
                  public static <E> Set<E> newConcurrentHashSet(Iterable<? extends E> elements) {
                      return newConcurrentHashSet();
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package abc;
                            
              import java.util.Set;
              class Test {
                  Set<String> s = Thing.newConcurrentHashSet();
              }
              """,
            """
              /*~~>*/package abc;
                            
              import java.util.Set;
              class Test {
                  Set<String> s = Thing.newConcurrentHashSet();
              }
              """
          )
        );
    }

    @Test
    void usesMethodReferences() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesMethod<>("A singleArg(String)"))),
          java(
            """
              class Test {
                  void test() {
                      new java.util.ArrayList<String>().forEach(new A()::singleArg);
                  }
              }
              """,
            """
              /*~~>*/class Test {
                  void test() {
                      new java.util.ArrayList<String>().forEach(new A()::singleArg);
                  }
              }
              """
          ),
          java(
            """
              class A {
                  public void singleArg(String s) {}
              }
              """
          )
        );
    }

    @Test
    void usesStaticMethodCalls() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesMethod<>("java.util.Collections emptyList()"))),
          java(
            """
              import java.util.Collections;
              public class A {
                 Object o = Collections.emptyList();
              }
              """,
            """
              /*~~>*/import java.util.Collections;
              public class A {
                 Object o = Collections.emptyList();
              }
              """
          )
        );
    }

    @Test
    void usesStaticallyImportedMethodCalls() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesMethod<>("java.util.Collections emptyList()"))),
          java(
            """
              import static java.util.Collections.emptyList;
              public class A {
                 Object o = emptyList();
              }
              """,
            """
              /*~~>*/import static java.util.Collections.emptyList;
              public class A {
                 Object o = emptyList();
              }
              """
          )
        );
    }

    @Test
    void matchVarargs() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesMethod<>("A foo(String, Object...)"))),
          java(
            """
              public class B {
                 public void test() {
                     new A().foo("s", "a", 1);
                 }
              }
              """,
            """
              /*~~>*/public class B {
                 public void test() {
                     new A().foo("s", "a", 1);
                 }
              }
              """
          ),
          java(
            """
              public class A {
                  public void foo(String s, Object... o) {}
              }
              """
          )
        );
    }

    @Test
    void matchOnInnerClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesMethod<>("B.C foo()"))),
          java(
            """
              public class A {
                 void test() {
                     new B.C().foo();
                 }
              }
              """,
            """
              /*~~>*/public class A {
                 void test() {
                     new B.C().foo();
                 }
              }
              """
          ),
          java(
            """
              public class B {
                 public static class C {
                     public void foo() {}
                 }
              }
              """
          )
        );
    }
}
