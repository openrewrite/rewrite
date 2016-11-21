package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class AddFieldTest(p: Parser): Parser by p {
    
    @Test
    fun addFieldDefaultIndent() {
        val a = parse("""
            |class A {
            |}
        """)

        val fixed = a.refactor {
            addField(a.classes[0], List::class.java, "list", "new ArrayList<>()")
        }.fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |
            |class A {
            |    private List list = new ArrayList<>();
            |}
        """)
    }

    @Test
    fun addFieldMatchSpaces() {
        val a = parse("""
            |import java.util.List;
            |
            |class A {
            |  List l;
            |}
        """)

        val fixed = a.refactor {
            addField(a.classes[0], List::class.java, "list")
        }.fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |
            |class A {
            |  private List list;
            |  List l;
            |}
        """)
    }

    @Test
    fun addFieldMatchTabs() {
        val a = parse("""
            |import java.util.List;
            |
            |class A {
            |           List l;
            |}
        """)

        val fixed = a.refactor {
            addField(a.classes[0], List::class.java, "list")
        }.fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |
            |class A {
            |           private List list;
            |           List l;
            |}
        """)
    }
}

class OracleJdkAddFieldTest: AddFieldTest(OracleJdkParser())