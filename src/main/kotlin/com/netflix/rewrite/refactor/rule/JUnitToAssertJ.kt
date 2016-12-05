package com.netflix.rewrite.refactor.rule

import com.netflix.rewrite.Rewrite
import com.netflix.rewrite.ast.AstTransform
import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.Type
import com.netflix.rewrite.refactor.RefactorVisitor

@Rewrite("junit-to-assertj", description = "convert JUnit-style assertions to AssertJ")
class JUnitToAssertJ {
    fun fix(cu: Tr.CompilationUnit): Tr.CompilationUnit {
        // TODO what to do with assertEquals with message at beginning?
        val assertEquals = cu.findMethodCalls("org.junit.Assert assertEquals(..)").map { it.id }

        return cu.refactor {
            if(assertEquals.isNotEmpty()) {
                addImport("org.assertj.core.api.Assertions", "*")
            }

            run(ConvertAssertions(assertEquals))

            removeImport("org.junit.Assert")
        }.fix()
    }
}

class ConvertAssertions(val assertEquals: List<String>) : RefactorVisitor<Tr.MethodInvocation>() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.MethodInvocation>> {
        if(assertEquals.contains(meth.id)) {
            val assertionsClass = Type.Class.build(cu.typeCache(), "org.assertj.core.api.Assertions")
            return transform {
                val assertThat = Tr.MethodInvocation(null, null,
                        Tr.Ident("assertThat", null),
                        Tr.MethodInvocation.Arguments(listOf(meth.args.args[1].format(Formatting.Reified.Empty))),
                        assertionsClass,
                        null)

                Tr.MethodInvocation(assertThat, null,
                        Tr.Ident("isEqualTo", null),
                        Tr.MethodInvocation.Arguments(listOf(meth.args.args[0].format(Formatting.Reified.Empty))),
                        null,
                        null,
                        meth.formatting)
            }
        }
        return super.visitMethodInvocation(meth)
    }
}