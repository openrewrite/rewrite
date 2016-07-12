package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChangeTypeTest {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Test
    fun refactorType() {
        val a1 = temp.newFile("A1.java")
        val a2 = temp.newFile("A2.java")
        val b = temp.newFile("B.java")
        
        a1.writeText("""
            package a;
            public class A1 {}
        """)

        a2.writeText("""
            package a;
            public class A2 {}
        """)
        
        b.writeText("""
            |package b;
            |import java.util.List;
            |import a.A1;
            |
            |public class B extends A1 {
            |    List<A1> aList = new ArrayList<>();
            |    A1 aField = new A1();
            |
            |    public <T extends A1> T b(A1 aParam) {
            |        A1 aVar = new A1();
            |        return aVar;
            |    }
            |}
        """.trimMargin())

        RefactorRule()
                .changeType("a.A1", "a", "A2")
                .refactorAndFix(listOf(b, a1, a2))
        
        assertEquals("""
            |package b;
            |import java.util.List;
            |import a.A2;
            |
            |public class B extends A2 {
            |    List<A2> aList = new ArrayList<>();
            |    A2 aField = new A2();
            |
            |    public <T extends A2> T b(A2 aParam) {
            |        A2 aVar = new A2();
            |        return aVar;
            |    }
            |}
        """.trimMargin(), b.readText())
    }
}