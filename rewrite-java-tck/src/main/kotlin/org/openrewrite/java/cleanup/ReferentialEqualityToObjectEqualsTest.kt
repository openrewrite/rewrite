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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("ConstantConditions", "StatementWithEmptyBody", "NewObjectEquality", "StringEquality",
    "EqualsWhichDoesntCheckParameterClass")
interface ReferentialEqualityToObjectEqualsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ReferentialEqualityToObjectEquals())
    }

    @Test
    fun doesNotModifyBoxedTypes(): Unit = rewriteRun(
        java("""
            class C {
                char c = 'c';
                boolean isC(Integer value){
                    return value == c || value == 99;
                }
            }
        """)
    )

    @Test
    fun doesNotModifyEnumComparison() = rewriteRun(
        java(
            """
                class B {
                  private void method() {
                    if(Foo.FOO == Foo.BAR) {}
                  }
                  enum Foo {FOO, BAR}
                }
            """
        )
    )

    @Test
    fun doesNotModifyClassComparisons() = rewriteRun(
        java(
            """
                class A {
                    void check() {
                        B b = new B();
                        if(b == this) {}
                    }
                    
                    class B {
                        @Override
                        public boolean equals(Object anObject) {return true;}
                    }
                }
            """
        )
    )

    @Test
    fun typeDoesNotOverrideEquals() = rewriteRun(
        java("""
            class T {
                void doSomething() {
                    A a1 = new A();
                    B b1 = new B();
                    if (a1 == b1) {}
                }
                class A {}
                class B {}
            }
        """)
    )

    @Test
    fun onlyOneSideOverridesEquals() = rewriteRun(
        java("""
            class T {
                void doSomething() {
                    A a1 = new A();
                    B b1 = new B();
                    if (a1 == b1) {}
                }
                class A {
                    @Override
                    public boolean equals(Object anObject) {return true;}
                }
                class B {}
            }
        """)
    )

    @Test
    fun doNotModifyWithinEqualsMethod() = rewriteRun(
        java("""
            class T {
                String s1;
                String s2;
                @Override
                public boolean equals(Object obj) {
                    if (s1 != s2) {}
                    if (s1 == s2) {}
                    return super.equals(obj);
                }
            }
        """)
    )

    @Test
    fun bothSidesOverrideEquals() = rewriteRun(
        java("""
            class T {
                void doSomething() {
                    A a1 = new A();
                    A a2 = new A();
                    if (a1 == a2) {}
                }
                class A {
                    @Override
                    public boolean equals(Object anObject) {return true;}
                }
            }
        """,
        """
            class T {
                void doSomething() {
                    A a1 = new A();
                    A a2 = new A();
                    if (a1.equals(a2)) {}
                }
                class A {
                    @Override
                    public boolean equals(Object anObject) {return true;}
                }
            }
        """)
    )
}
