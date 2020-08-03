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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.whenParsedBy

interface ChangeMethodNameTest {
    companion object {
        private val b: String = """
                package com.abc;
                class B {
                   public void singleArg(String s) {}
                   public void arrArg(String[] s) {}
                   public void varargArg(String... s) {}
                   public static void static1(String s) {}
                   public static void static2(String s) {}
                }
            """.trimIndent()
    }

    @Test
    fun changeMethodNameForMethodWithSingleArgDeclarative(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void test() {
                   new B().singleArg("boo");
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply { setMethod("com.abc.B singleArg(String)"); setName("bar") })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void test() {
                           new B().bar("boo");
                       }
                    }
                """)
    }

    @Test
    fun changeMethodNameForMethodWithSingleArg(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void test() {
                   new B().singleArg("boo");
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.B singleArg(String)")
                    name = "bar"
                })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void test() {
                           new B().bar("boo");
                       }
                    }
                """)
    }

    @Test
    fun changeMethodNameForMethodWithArrayArg(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void test() {
                   new B().arrArg(new String[] {"boo"});
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.B arrArg(String[])")
                    name = "bar"
                })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void test() {
                           new B().bar(new String[] {"boo"});
                       }
                    }
                """)
    }

    @Test
    fun changeMethodNameForMethodWithVarargArg(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void test() {
                   new B().varargArg("boo", "again");
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.B varargArg(String...)")
                    name = "bar"
                })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void test() {
                           new B().bar("boo", "again");
                       }
                    }
                """)
    }

    @Test
    fun changeMethodNameWhenMatchingAgainstMethodWithNameThatIsAnAspectjToken(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void test() {
                   new B().error();
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn("""
                    package com.abc;
                    class B {
                       public void error() {}
                       public void foo() {}
                    }
                """)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.B error()")
                    name = "foo"
                })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void test() {
                           new B().foo();
                       }
                    }
                """)
    }

    @Test
    fun changeMethodDeclarationForMethodWithSingleArg(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void foo(String s) {
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.A foo(String)")
                    name = "bar"
                })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void bar(String s) {
                       }
                    }
                """)
    }

    @Test
    fun changeStaticMethodTest(jp: JavaParser) {
        """
            package com.abc;
            class A {
               public void test() {
                   B.static1("boo");
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.B static1(String)")
                    name = "static2"
                })
                .isRefactoredTo("""
                    package com.abc;
                    class A {
                       public void test() {
                           B.static2("boo");
                       }
                    }
                """)
    }

    /**
     * This test is known to be failing currently, ChangeMethodName() needs to be updated to handle this scenario
     */
    @Test
    fun changeStaticImportTest(jp: JavaParser) {
        """
            package com.abc;
            import static com.abc.B.static1;
            class A {
               public void test() {
                   static1("boo");
               }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(b)
                .whenVisitedBy(ChangeMethodName().apply {
                    setMethod("com.abc.B static1(String)")
                    name = "static2"
                })
                .isRefactoredTo("""
                    package com.abc;
                    import static com.abc.B.static2;
                    class A {
                       public void test() {
                           static2("boo");
                       }
                    }
                """)
    }
}
