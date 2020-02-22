/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.visitor.refactor.op

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.Parser
import org.junit.Test

open class RemoveImportTest: Parser() {
    
    @Test
    fun removeNamedImport() {
        val a = parse("""
            import java.util.List;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().removeImport("java.util.List").fix().fixed

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun removeNamedImportByClass() {
        val a = parse("""
            import java.util.List;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().removeImport("java.util.List").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.List").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.List").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.List").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.List").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.Collections").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.Collections").fix().fixed

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

        val fixed = a.refactor().removeImport("java.util.Collections").fix().fixed

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
            }
        """.trimIndent()

        val cSource = """
            package foo;
            public class C {
                public static final String ANOTHER = "string";
            }
        """.trimIndent()

        val a = parse("""
            import static foo.B.*;
            import static foo.C.*;
            public class A {
                String a = STRING;
            }
        """.trimIndent(), bSource, cSource)

        val fixed = a.refactor().removeImport("foo.B").removeImport("foo.C").fix().fixed

        assertRefactored(fixed, """
            import static foo.B.*;
            public class A {
                String a = STRING;
            }
        """)
    }
}