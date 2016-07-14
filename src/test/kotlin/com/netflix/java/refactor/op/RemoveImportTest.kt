package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorTest
import org.junit.Test

class RemoveImportTest: RefactorTest() {

    @Test
    fun removeNamedImport() {
        val a = java("""
            |import java.util.List;
            |class A {}
        """)
        
        refactor(a).removeImport("java.util.List")
        
        assertRefactored(a, "class A {}")
    }

    @Test
    fun removeNamedImportByClass() {
        val a = java("""
            |import java.util.List;
            |class A {}
        """)

        refactor(a).removeImport(List::class.java)
        
        assertRefactored(a, "class A {}")
    }
    
    @Test
    fun removeStarImportIfNoTypesReferredTo() {
        val a = java("""
            |import java.util.*;
            |class A {}
        """.trimMargin())

        refactor(a).removeImport(List::class.java)

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

        refactor(a).removeImport(List::class.java)
        
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
        
        refactor(a).removeImport("java.util.List")

        assertRefactored(a, """
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """)
    }
}
