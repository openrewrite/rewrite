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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("RedundantOperationOnEmptyContainer")
interface UsesMethodTest : JavaRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1169")
    @Test
    fun emptyConstructor(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesMethod("abc.Thing newConcurrentHashSet()")
        },
        dependsOn = arrayOf(
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
            """
        ),
        before = """
            package abc;
            
            import java.util.Set;
            class Test {
                Set<String> s = Thing.newConcurrentHashSet();
            }
        """,
        after = """
            /*~~>*/package abc;
            
            import java.util.Set;
            class Test {
                Set<String> s = Thing.newConcurrentHashSet();
            }
        """
    )

    @Test
    fun usesMethodReferences(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesMethod<ExecutionContext>("A singleArg(String)")
        },
        before = """
            class Test {
                void test() {
                    new java.util.ArrayList<String>().forEach(new A()::singleArg);
                }
            }
        """,
        after = """
            /*~~>*/class Test {
                void test() {
                    new java.util.ArrayList<String>().forEach(new A()::singleArg);
                }
            }
        """,
        dependsOn = arrayOf("""
            class A {
                public void singleArg(String s) {}
            }
        """)
    )

    @Test
    fun usesStaticMethodCalls(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesMethod<ExecutionContext>("java.util.Collections emptyList()")
        },
        before = """
            import java.util.Collections;
            public class A {
               Object o = Collections.emptyList();
            }
        """,
        after = """
            /*~~>*/import java.util.Collections;
            public class A {
               Object o = Collections.emptyList();
            }
        """
    )

    @Test
    fun usesStaticallyImportedMethodCalls(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesMethod<ExecutionContext>("java.util.Collections emptyList()")
        },
        before = """
            import static java.util.Collections.emptyList;
            public class A {
               Object o = emptyList();
            }
        """,
        after = """
            /*~~>*/import static java.util.Collections.emptyList;
            public class A {
               Object o = emptyList();
            }
        """
    )

    @Test
    fun matchVarargs(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesMethod<ExecutionContext>("A foo(String, Object...)")
        },
        before = """
            public class B {
               public void test() {
                   new A().foo("s", "a", 1);
               }
            }
        """,
        after = """
            /*~~>*/public class B {
               public void test() {
                   new A().foo("s", "a", 1);
               }
            }
        """,
        dependsOn = arrayOf("""
            public class A {
                public void foo(String s, Object... o) {}
            }
        """)
    )

    @Test
    fun matchOnInnerClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesMethod<ExecutionContext>("B.C foo()")
        },
        before = """
            public class A {
               void test() {
                   new B.C().foo();
               }
            }
        """,
        after = """
            /*~~>*/public class A {
               void test() {
                   new B.C().foo();
               }
            }
        """,
        dependsOn = arrayOf("""
            public class B {
               public static class C {
                   public void foo() {}
               }
            }
        """)
    )
}
