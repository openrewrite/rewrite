package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemoveImportSpec {
    @JvmField @Rule
    val temp = TemporaryFolder()

    val removeImportRule = RefactorRule()
            .removeImport("java.util.List")
    
    @Test
    fun removeNamedImport() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |import java.util.List;
            |class A {}
        """.trimMargin())
        
        removeImportRule.refactorAndFix(a)
        assertEquals("class A {}", a.readText())
    }
    
    @Test
    fun removeStarImportIfNoTypesReferredTo() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |import java.util.*;
            |class A {}
        """.trimMargin())

        removeImportRule.refactorAndFix(a)
        assertEquals("class A {}", a.readText())
    }
    
    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |}
        """.trimMargin())
        
        removeImportRule.refactorAndFix(a)
        assertEquals("""
            |import java.util.Collection;
            |class A {
            |   Collection c;
            |}
        """.trimMargin(), a.readText())
    }
    
    @Test
    fun leaveStarImportInPlaceIfMoreThanTwoTypesStillReferredTo() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """.trimMargin())
        
        removeImportRule.refactorAndFix(a)
        assertEquals("""
            |import java.util.*;
            |class A {
            |   Collection c;
            |   Set s;
            |}
        """.trimMargin(), a.readText())
    }
}
