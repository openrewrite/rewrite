package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import org.junit.Test
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser

abstract class ChangeFieldNameTest(p: Parser): Parser by p {

    @Test
    fun changeFieldName() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |   List collection = null;
            |}
        """)

        val fixed = a.refactor() {
            a.classes[0].findFields(List::class.java).forEach {
                changeName(it, "list")
            }
        }.fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |public class A {
            |   List list = null;
            |}
        """)
    }
}

class OracleJdkChangeFieldNameTest: ChangeFieldNameTest(OracleJdkParser())