package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class ChangeFieldTypeTest(p: Parser): Parser by p {
    
    @Test
    fun changeFieldType() {
        val a = parse("""
            |import java.util.List;
            |public class A {
            |   List collection;
            |}
        """)

        val fixed = a.refactor {
            a.classes[0].findFields(List::class.java).forEach { f ->
                changeType(f, Collection::class.java)
            }
        }.fix()

        assertRefactored(fixed, """
            |import java.util.Collection;
            |
            |public class A {
            |   Collection collection;
            |}
        """)
    }
}

class OracleJdkChangeFieldTypeTest : ChangeFieldTypeTest(OracleJdkParser())