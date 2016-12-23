/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
