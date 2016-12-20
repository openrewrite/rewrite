package com.netflix.rewrite.refactor.rule

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.refactor.rule.JUnitToAssertJ
import org.junit.Test

class JUnitToAssertJTest: Parser by OracleJdkParser() {

    @Test
    fun assertEquals() {
        val a = parse("""
            |import org.junit.Test;
            |import static org.junit.Assert.*;
            |public class A {
            |    @Test
            |    public void test() {
            |        assertEquals("c", "abc".substring(2));
            |    }
            |}
        """)

        val fixed = JUnitToAssertJ().refactor(a).fix()

        assertRefactored(fixed, """
            |import org.junit.Test;
            |import static org.assertj.core.api.Assertions.*;
            |
            |public class A {
            |    @Test
            |    public void test() {
            |        assertThat("abc".substring(2)).isEqualTo("c");
            |    }
            |}
        """)
    }
}
