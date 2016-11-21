package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test
import java.util.*

abstract class RemoveImportTest(p: Parser): Parser by p {
    
    @Test
    fun removeNamedImport() {
        val a = parse("""
            |import java.util.List;
            |class A {}
        """)

        val fixed = a.refactor().removeImport("java.util.List").fix()

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun removeNamedImportByClass() {
        val a = parse("""
            |import java.util.List;
            |class A {}
        """)

        val fixed = a.refactor().removeImport(List::class.java).fix()

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo() {
        val a = parse("""
            |import java.util.List;
            |class A {
            |   List list;
            |}
        """)

        val fixed = a.refactor().removeImport(List::class.java).fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |class A {
            |   List list;
            |}
        """)
    }

    @Test
    fun removeStarImportIfNoTypesReferredTo() {
        val a = parse("""
            |import java.util.*;
            |class A {}
        """.trimMargin())

        val fixed = a.refactor().removeImport(List::class.java).fix()

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() {
        val a = parse("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |}
        """)

        val fixed = a.refactor().removeImport(List::class.java).fix()

        assertRefactored(fixed, """
            |import java.util.Collection;
            |class A {
            |   Collection c;
            |}
        """)
    }

    @Test
    fun leaveStarImportInPlaceIfMoreThanTwoTypesStillReferredTo() {
        val a = parse("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """)

        val fixed = a.refactor().removeImport("java.util.List").fix()

        assertRefactored(fixed, """
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """)
    }

    @Test
    fun removeStarStaticImport() {
        val a = parse("""
            |import static java.util.Collections.*;
            |class A {}
        """)

        val fixed = a.refactor().removeImport(Collections::class.java).fix()

        assertRefactored(fixed, "class A {}")
    }

    @Test
    fun leaveStarStaticImportIfReferenceStillExists() {
        val a = parse("""
            |import static java.util.Collections.*;
            |class A {
            |   Object o = emptyList();
            |}
        """)

        val fixed = a.refactor().removeImport(Collections::class.java).fix()

        assertRefactored(fixed, """
            |import static java.util.Collections.*;
            |class A {
            |   Object o = emptyList();
            |}
        """)
    }

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists() {
        val a = parse("""
            |import static java.util.Collections.emptyList;
            |import static java.util.Collections.emptySet;
            |class A {
            |   Object o = emptyList();
            |}
        """)

        val fixed = a.refactor().removeImport(Collections::class.java).fix()

        assertRefactored(fixed, """
            |import static java.util.Collections.emptyList;
            |class A {
            |   Object o = emptyList();
            |}
        """)
    }
}

class OracleJdkRemoveImportTest: RemoveImportTest(OracleJdkParser())