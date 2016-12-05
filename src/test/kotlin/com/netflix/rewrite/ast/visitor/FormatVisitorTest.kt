package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.TreeBuilder
import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Test

class FormatVisitorTest : Parser by OracleJdkParser() {

    @Test
    fun fixFormatting() {
        val a = parse("public class A {}")
        val list = TreeBuilder.buildName(a.typeCache(), "java.util.List", Formatting.Reified(" ")) as Tr.FieldAccess
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