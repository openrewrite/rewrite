package com.netflix.java.refactor.fix

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Ignore
import org.junit.Test

class ChangeTypeTest: AbstractRefactorTest() {
    
    @Ignore
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
            |import java.util.*;
            |import a.A1;
            |
            |public class B extends A1 {
            |    List<A1> aList = new ArrayList<>();
            |    A1 aField = new A1();
            |
            |    public A1 b(A1 aParam) {
            |        A1 aVar = new A1();
            |        return aVar;
            |    }
            |}
        """)

        val javaSource = parseJava(b, a1, a2)
        javaSource.refactor().changeType("a.A1", "a.A2").fix()
        javaSource.refactor().removeImport("a.A1").fix()
        
        assertRefactored(b, """
            |package b;
            |import java.util.*;
            |import a.A2;
            |
            |public class B extends A2 {
            |    List<A2> aList = new ArrayList<>();
            |    A2 aField = new A2();
            |
            |    public A2 b(A2 aParam) {
            |        A2 aVar = new A2();
            |        return aVar;
            |    }
            |}
        """)
    }
}