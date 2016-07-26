package com.netflix.java.refactor.fix

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Test

class ChangeTypeTest: AbstractRefactorTest() {
    
    @Test
    fun refactorType() {
        val a1 = java("""
            |package a;
            |public class A1 {}
        """)

        val a2 = java("""
            |package a;
            |public class A2 {}
        """)
        
        val b = java("""
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
        """)

        parseJava(b, a1, a2).refactor().changeType("a.A1", "a.A2").fix()
        
        assertRefactored(b, """
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
        """)
    }
}