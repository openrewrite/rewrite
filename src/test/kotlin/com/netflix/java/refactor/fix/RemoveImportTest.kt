package com.netflix.java.refactor.fix

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Test
import java.util.*

class RemoveImportTest: AbstractRefactorTest() {

    @Test
    fun removeNamedImport() {
        val a = java("""
            |import java.util.List;
            |class A {}
        """)
        
        parseJava(a).refactor().removeImport("java.util.List").fix()
        
        assertRefactored(a, "class A {}")
    }

    @Test
    fun removeNamedImportByClass() {
        val a = java("""
            |import java.util.List;
            |class A {}
        """)

        parseJava(a).refactor().removeImport(List::class.java).fix()
        
        assertRefactored(a, "class A {}")
    }
    
    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo() {
        val a = java("""
            |import java.util.List;
            |class A {
            |   List list;
            |}
        """)

        parseJava(a).refactor().removeImport(List::class.java).fix()

        assertRefactored(a, """
            |import java.util.List;
            |class A {
            |   List list;
            |}
        """)
    }
    
    @Test
    fun removeStarImportIfNoTypesReferredTo() {
        val a = java("""
            |import java.util.*;
            |class A {}
        """.trimMargin())

        parseJava(a).refactor().removeImport(List::class.java).fix()

        assertRefactored(a, "class A {}")
    }
    
    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() {
        val a = java("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |}
        """)

        parseJava(a).refactor().removeImport(List::class.java).fix()
        
        assertRefactored(a, """
            |import java.util.Collection;
            |class A {
            |   Collection c;
            |}
        """)
    }
    
    @Test
    fun leaveStarImportInPlaceIfMoreThanTwoTypesStillReferredTo() {
        val a = java("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """)
        
        parseJava(a).refactor().removeImport("java.util.List").fix()

        assertRefactored(a, """
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """)
    }

    @Test
    fun removeStarStaticImport() {
        val a = java("""
            |import static java.util.Collections.*;
            |class A {}
        """)

        parseJava(a).refactor().removeImport(Collections::class.java).fix()

        assertRefactored(a, "class A {}")
    }

    @Test
    fun leaveStarStaticImportIfReferenceStillExists() {
        val a = java("""
            |import static java.util.Collections.*;
            |class A {
            |   Object o = emptyList();
            |}
        """)

        parseJava(a).refactor().removeImport(Collections::class.java).fix()

        assertRefactored(a, """
            |import static java.util.Collections.*;
            |class A {
            |   Object o = emptyList();
            |}
        """)
    }
    
    @Test
    fun leaveNamedStaticImportIfReferenceStillExists() {
        val a = java("""
            |import static java.util.Collections.emptyList;
            |import static java.util.Collections.emptySet;
            |class A {
            |   Object o = emptyList();
            |}
        """)

        parseJava(a).refactor().removeImport(Collections::class.java).fix()

        assertRefactored(a, """
            |import static java.util.Collections.emptyList;
            |class A {
            |   Object o = emptyList();
            |}
        """)
    }

}
