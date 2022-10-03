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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("Convert2Diamond")
interface UseDiamondOperatorTest: RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UseDiamondOperator())
    }

    @Test
    fun useDiamondOperator() = rewriteRun(
        java("""
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
        """)
    )

    @Suppress("rawtypes")
    @Test
    fun useDiamondOperatorTest2() = rewriteRun(
        {spec -> spec.expectedCyclesThatMakeChanges(2)},
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
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2274")
    @Test
    fun returnTypeParamsDoNotMatchNewClassParams() = rewriteRun(
        java("""
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
        ""","""
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
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1297")
    @Test
    fun doNotUseDiamondOperatorsForVariablesHavingNullOrUnknownTypes() = rewriteRun(
        java("""
            import lombok.val;
            import java.util.ArrayList;

            class Test<X, Y> {
                void test() {
                    val ls = new ArrayList<String>();
                }
            }
        """)
    )

    @Test
    fun noLeftSide() = rewriteRun(
        java("""
            import java.util.HashMap;
            class Test {
                static {
                    new HashMap<String, String>();
                }
            }
        """)
    )

    @Test
    fun notAsAChainedMethodInvocation() = rewriteRun(
        java("""
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
        """)
    )
}
