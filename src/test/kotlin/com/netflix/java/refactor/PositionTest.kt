package com.netflix.java.refactor

import com.sun.source.tree.MethodInvocationTree
import com.sun.source.util.TreePath
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.table
import kotlin.test.assertEquals

class PositionTest : Spek({
    table { code: String, desc: String, lineNumber: Int, lastLineNumber: Int, columnNumber: Int, lastColumnNumber: Int ->
        val source = """
            |class Container { 
            |    void method() { 
            |        $code
            |    }
            |}
        """.trimMargin()

        println(source)

        val parser = AstParser()
        val cu = parser.parseSources(source).first()

        var meth: JCTree.JCMethodInvocation? = null
        object : TreePathScanner<TreePath, Context>() {
            override fun visitMethodInvocation(node: MethodInvocationTree, p: Context): TreePath? {
                if (meth == null)
                    meth = node as JCTree.JCMethodInvocation
                return super.visitMethodInvocation(node, p)
            }
        }.scan(cu, parser.context)

        val pos = meth!!.positionIn(cu)

        describe("for a $desc code snippet, identify the position and contents of foo()") {
            it("should identify the starting line number") {
                assertEquals(lineNumber, pos.lineNumber)
            }

            it("should identify the last line number") {
                assertEquals(lastLineNumber, pos.lastLineNumber)
            }

            it("should identify the starting column number") {
                assertEquals(columnNumber, pos.columnNumber)
            }

            it("should identify the last column number") {
                assertEquals(lastColumnNumber, pos.lastColumnNumber)
            }
        }
    } where {
        row("foo();",           "single line, single expression",   3, 3, 9, 14)
        row("foo(); bar();",    "single line, multiple expression", 3, 3, 9, 14)
        row("foo(\n        );", "multiple line, single expression", 3, 4, 9, 9)
    }
})