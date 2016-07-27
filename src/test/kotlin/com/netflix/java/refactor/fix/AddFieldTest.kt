package com.netflix.java.refactor.fix

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Test

class AddFieldTest: AbstractRefactorTest() {
    
    @Test
    fun addField() {
        val a = java("""
            |class A {
            |}
        """)

        parseJava(a).refactor().addField(List::class.java, "list", "new ArrayList<>()").fix()

        assertRefactored(a, """
            |import java.util.List;
            |class A {
            |   List list = new ArrayList<>();
            |}
        """)
    }
}