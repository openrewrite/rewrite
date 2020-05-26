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
package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

open class RemoveImportTest : JavaParser() {

    @Test
    fun removeNamedImport() {
        val a = parse("""
            import java.util.List;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.List")).fix().fixed

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo() {
        val a = parse("""
            import java.util.List;
            class A {
               List list;
            }
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.List")).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            class A {
               List list;
            }
        """)
    }

    @Test
    fun removeStarImportIfNoTypesReferredTo() {
        val a = parse("""
            import java.util.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.List")).fix().fixed

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() {
        val a = parse("""
            import java.util.*;
            class A {
               Collection c;
            }
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.List")).fix().fixed

        assertRefactored(fixed, """
            import java.util.Collection;
            class A {
               Collection c;
            }
        """)
    }

    @Test
    fun leaveStarImportInPlaceIfMoreThanTwoTypesStillReferredTo() {
        val a = parse("""
            import java.util.*;
            class A {
               Collection c;
               Set s;
            }
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.List")).fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            class A {
               Collection c;
               Set s;
            }
        """)
    }

    @Test
    fun removeStarStaticImport() {
        val a = parse("""
            import static java.util.Collections.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.Collections")).fix().fixed

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun leaveStarStaticImportIfReferenceStillExists() {
        val a = parse("""
            import static java.util.Collections.*;
            class A {
               Object o = emptyList();
            }
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.Collections")).fix().fixed

        assertRefactored(fixed, """
            import static java.util.Collections.*;
            class A {
               Object o = emptyList();
            }
        """)
    }

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists() {
        val a = parse("""
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptySet;
            class A {
               Object o = emptyList();
            }
        """.trimIndent())

        val fixed = a.refactor().visit(RemoveImport("java.util.Collections")).fix().fixed

        assertRefactored(fixed, """
            import static java.util.Collections.emptyList;
            class A {
               Object o = emptyList();
            }
        """)
    }

    @Test
    fun leaveNamedStaticImportOnFieldIfReferenceStillExists() {
        val bSource = """
            package foo;
            public class B {
                public static final String STRING = "string";
                public static final String STRING2 = "string2";
            }
        """.trimIndent()

        val cSource = """
            package foo;
            public class C {
                public static final String ANOTHER = "string";
            }
        """.trimIndent()

        val a = parse("""
            import static foo.B.STRING;
            import static foo.B.STRING2;
            import static foo.C.*;
            public class A {
                String a = STRING;
            }
        """.trimIndent(), bSource, cSource)

        val fixed = a.refactor()
                .visit(RemoveImport("foo.B"))
                .visit(RemoveImport("foo.C"))
                .fix().fixed

        assertRefactored(fixed, """
            import static foo.B.STRING;
            public class A {
                String a = STRING;
            }
        """)
    }

    @Test
    fun removeImportForChangedMethodArgument() {
        val b = """
            package b;
            public interface B {
                void doSomething();
            }
        """.trimIndent()

        val c = """
            package c;
            public interface C {
                void doSomething();
            }
        """.trimIndent()

        val a = parse("""
            import b.B;
            
            class A {
                void foo(B arg) {
                    arg.doSomething();
                }
            }
        """.trimIndent(), b, c)

        val fixed = a.refactor().visit(ChangeType("b.B", "c.C")).fix().fixed

        assertRefactored(fixed, """
            import c.C;
            
            class A {
                void foo(C arg) {
                    arg.doSomething();
                }
            }
        """)
    }
}
