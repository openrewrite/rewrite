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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"Convert2Diamond", "unchecked", "rawtypes"})
class UseDiamondOperatorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseDiamondOperator());
    }

    @Test
    void useDiamondOperator() {
        rewriteRun(
          java(
            """
              import java.util.*;

              class Test<X, Y> {
                  void test() {
                      List<String> ls = new ArrayList<String>();
                      Map<X,Y> map = new HashMap<X,Y>();
                      List<String> ls2 = new ArrayList<String>() {
                      };
                  }
              }
              """,
            """
              import java.util.*;

              class Test<X, Y> {
                  void test() {
                      List<String> ls = new ArrayList<>();
                      Map<X,Y> map = new HashMap<>();
                      List<String> ls2 = new ArrayList<String>() {
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void varArgIsParameterizedNewClass() {
        rewriteRun(
          java(
            """
              import java.util.*;

              class Foo {
                  void something(List<Integer>... lists) {}
                  void somethingElse(Object[] o, List<Integer> s){}
                  void doSomething() {
                      something(new ArrayList<Integer>(), new ArrayList<Integer>());
                      something(new ArrayList<Integer>());
                      somethingElse(new String[0], new ArrayList<Integer>());
                  }
              }
              """,
            """
              import java.util.*;

              class Foo {
                  void something(List<Integer>... lists) {}
                  void somethingElse(Object[] o, List<Integer> s){}
                  void doSomething() {
                      something(new ArrayList<>(), new ArrayList<>());
                      something(new ArrayList<>());
                      somethingElse(new String[0], new ArrayList<>());
                  }
              }
              """
          )
        );
    }

    @Test
    void useDiamondOperatorTest2() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.HashMap;
              import java.util.function.Predicate;
              import java.util.List;
              import java.util.Map;
                            
              class Foo<T> {
                  Map<String, Integer> map;
                  Map unknownMap;
                  public Foo(Predicate<T> p) {}
                  public void something(Foo<List<String>> foos){}
                  public void somethingEasy(List<List<String>> l){}
                  
                  Foo getFoo() {
                      // variable type initializer
                      Foo<List<String>> f = new Foo<List<String>>(it -> it.stream().anyMatch(baz -> true));
                      // assignment
                      map = new HashMap<String, Integer>();
                      unknownMap = new HashMap<String, Integer>();
                      // method argument type assignment
                      something(new Foo<List<String>>(it -> it.stream().anyMatch(b -> true)));
                      somethingEasy(new ArrayList<List<String>>());
                      // return type and assignment type unknown
                      Object o = new Foo<List<String>>(it -> it.stream().anyMatch(baz -> true));
                      // return type unknown
                      return new Foo<List<String>>(it -> it.stream().anyMatch(baz -> true));
                  }
                  
                  Foo<List<String>> getFoo2() {
                      // return type expression
                      return new Foo<List<String>>(it -> it.stream().anyMatch(baz -> true));
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.HashMap;
              import java.util.function.Predicate;
              import java.util.List;
              import java.util.Map;
                            
              class Foo<T> {
                  Map<String, Integer> map;
                  Map unknownMap;
                  public Foo(Predicate<T> p) {}
                  public void something(Foo<List<String>> foos){}
                  public void somethingEasy(List<List<String>> l){}
                  
                  Foo getFoo() {
                      // variable type initializer
                      Foo<List<String>> f = new Foo<>(it -> it.stream().anyMatch(baz -> true));
                      // assignment
                      map = new HashMap<>();
                      unknownMap = new HashMap<String, Integer>();
                      // method argument type assignment
                      something(new Foo<>(it -> it.stream().anyMatch(b -> true)));
                      somethingEasy(new ArrayList<>());
                      // return type and assignment type unknown
                      Object o = new Foo<List<String>>(it -> it.stream().anyMatch(baz -> true));
                      // return type unknown
                      return new Foo<List<String>>(it -> it.stream().anyMatch(baz -> true));
                  }
                  
                  Foo<List<String>> getFoo2() {
                      // return type expression
                      return new Foo<>(it -> it.stream().anyMatch(baz -> true));
                  }
              }
              """
          )
        );

    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2274")
    @Test
    void returnTypeParamsDoNotMatchNewClassParams() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.function.Predicate;
                            
              class Test {
                  interface MyInterface<T> { }
                  class MyClass<S, T> implements MyInterface<T>{
                      public MyClass(Predicate<S> p, T check) {}
                  }
                            
                  public MyInterface<Integer> a() {
                      return new MyClass<List<String>, Integer>(l -> l.stream().anyMatch(String::isEmpty), 0);
                  }
                  public MyClass<List<String>, Integer> b() {
                      return new MyClass<List<String>, Integer>(l -> l.stream().anyMatch(String::isEmpty), 0);
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.function.Predicate;
                            
              class Test {
                  interface MyInterface<T> { }
                  class MyClass<S, T> implements MyInterface<T>{
                      public MyClass(Predicate<S> p, T check) {}
                  }
                            
                  public MyInterface<Integer> a() {
                      return new MyClass<List<String>, Integer>(l -> l.stream().anyMatch(String::isEmpty), 0);
                  }
                  public MyClass<List<String>, Integer> b() {
                      return new MyClass<>(l -> l.stream().anyMatch(String::isEmpty), 0);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1297")
    @Test
    void doNotUseDiamondOperatorsForVariablesHavingNullOrUnknownTypes() {
        rewriteRun(
          java(
            """
              import lombok.val;
              import java.util.ArrayList;

              class Test<X, Y> {
                  void test() {
                      var ls = new ArrayList<String>();
                  }
              }
              """
          )
        );
    }

    @Test
    void noLeftSide() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              class Test {
                  static {
                      new HashMap<String, String>();
                  }
              }
              """
          )
        );
    }

    @Test
    void notAsAChainedMethodInvocation() {
        rewriteRun(
          java(
            """
              class Test {
                  public static ResponseBuilder<String> bResponseEntity(String entity) {
                      return new ResponseBuilder<String>().entity(entity);
                  }
                  public static ResponseBuilder<String> bResponse(String entity) {
                      return new ResponseBuilder<String>();
                  }

                  public static class ResponseBuilder<T> {
                      public ResponseBuilder<T> entity(T entity) {
                          return this;
                      }
                  }
              }
              """,
            """
              class Test {
                  public static ResponseBuilder<String> bResponseEntity(String entity) {
                      return new ResponseBuilder<String>().entity(entity);
                  }
                  public static ResponseBuilder<String> bResponse(String entity) {
                      return new ResponseBuilder<>();
                  }

                  public static class ResponseBuilder<T> {
                      public ResponseBuilder<T> entity(T entity) {
                          return this;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotConvertVar() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  void test() {
                      var ls1 = new ArrayList<String>();
                      List<String> ls2 = new ArrayList<String>();
                  }
              }
              """,
            """
              import java.util.*;
              class Test {
                  void test() {
                      var ls1 = new ArrayList<String>();
                      List<String> ls2 = new ArrayList<>();
                  }
              }
              """
          )
        );
    }
}
