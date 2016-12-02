package com.netflix.java.refactor.ast.visitor

import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.TreeBuilder
import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
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