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

import com.netflix.rewrite.Rewrite
import com.netflix.rewrite.ast.AstTransform
import com.netflix.rewrite.ast.Formatting
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.Type
import com.netflix.rewrite.auto.Rule
import com.netflix.rewrite.refactor.Refactor
import com.netflix.rewrite.refactor.RefactorVisitor

@Rewrite("junit-to-assertj", description = "convert JUnit-style assertions to AssertJ")
class JUnitToAssertJ: Rule {
    override fun refactor(cu: Tr.CompilationUnit): Refactor {
        // TODO what to do with assertEquals with message at beginning?
        val assertEquals = cu.findMethodCalls("org.junit.Assert assertEquals(..)")

        return cu.refactor {
            if(assertEquals.isNotEmpty()) {
                addImport("org.assertj.core.api.Assertions", "*")
            }

            run(ConvertAssertions(assertEquals))

            removeImport("org.junit.Assert")
        }
    }
}

class ConvertAssertions(val assertEquals: List<Tr.MethodInvocation>) : RefactorVisitor<Tr.MethodInvocation>() {
    override val ruleName: String = "change-junit-assertion"

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tr.MethodInvocation>> {
        if(assertEquals.contains(meth)) {
            return transform {
                val assertThat = Tr.MethodInvocation(null, null,
                        Tr.Ident.build("assertThat", null),
                        Tr.MethodInvocation.Arguments(listOf(meth.args.args[1].changeFormatting(Formatting.Empty))),
                        null)

                Tr.MethodInvocation(assertThat, null,
                        Tr.Ident.build("isEqualTo", null),
                        Tr.MethodInvocation.Arguments(listOf(meth.args.args[0].changeFormatting(Formatting.Empty))),
                        null,
                        meth.formatting)
            }
        }
        return super.visitMethodInvocation(meth)
    }
}