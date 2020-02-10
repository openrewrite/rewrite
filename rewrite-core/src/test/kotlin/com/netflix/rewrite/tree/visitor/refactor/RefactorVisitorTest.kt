package com.netflix.rewrite.tree.visitor.refactor

import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.Statement
import com.netflix.rewrite.tree.Tree
import org.junit.Test

open class RefactorVisitorTest : Parser by OpenJdkParser() {
    @Test
    fun traversalIsDepthFirst() {
        val a = parse("""
            public class A {
                {
                    for(;;) {
                        String s = "s";
                    }
                }
            }
        """.trimIndent())

        a.refactor().run(object: RefactorVisitor() {
            override fun getRuleName(): String = "traversal"

            override fun visitStatement(statement: Statement?): MutableList<AstTransform> {
                println(cursor)
                return super.visitStatement(statement)
            }
        }).fix()
    }
}