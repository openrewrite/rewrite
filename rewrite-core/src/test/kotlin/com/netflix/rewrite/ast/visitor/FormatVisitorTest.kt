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
package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.TreeBuilder
import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.ast.format
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Test

class FormatVisitorTest : Parser by OracleJdkParser() {

    @Test
    fun fixFormatting() {
        val a = parse("public class A {}")
        val list = TreeBuilder.buildName("java.util.List", format(" ")) as Tr.FieldAccess
        val importAdded = a.copy(imports = listOf(Tr.Import(list, false, Formatting.Infer)))

        val formats = FormatVisitor().visit(importAdded)
        val fixed = TransformVisitor(formats).visit(importAdded) as Tr.CompilationUnit

        assertRefactored(fixed, """
            |import java.util.List;
            |
            |public class A {}
        """)
    }
}