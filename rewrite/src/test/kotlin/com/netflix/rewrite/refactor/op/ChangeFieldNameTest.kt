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
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.assertRefactored
import org.junit.Test
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser

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
                changeFieldName(it, "list")
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