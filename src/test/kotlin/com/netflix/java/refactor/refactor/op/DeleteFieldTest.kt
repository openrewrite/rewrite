package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class DeleteFieldTest(p: Parser): Parser by p {

    @Test
    fun deleteField() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |   List collection = null;
            |}
        """)

        val fixed = a.refactor {
            a.classes[0].findFields(List::class.java).forEach {
                delete(it)
            }
        }.fix()

        assertRefactored(fixed, """
            |public class A {
            |}
        """)
    }
}

class OracleDeleteFieldTest: DeleteFieldTest(OracleJdkParser())