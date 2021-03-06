/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java

import org.junit.jupiter.api.fail
import org.openrewrite.Cursor
import org.openrewrite.java.TypeValidator.InvalidTypeResult
import org.openrewrite.java.tree.J

/**
 * Produces a report about missing type attributions within a CompilationUnit.
 */
class TypeValidator : JavaIsoVisitor<MutableList<InvalidTypeResult>>() {
    data class InvalidTypeResult(val cursor: Cursor, val astElement: J, val message: String)
    companion object {
        @JvmStatic
        fun analyzeTypes(cu: J.CompilationUnit): List<InvalidTypeResult> {
            val report = mutableListOf<InvalidTypeResult>()
            TypeValidator().visit(cu, report)
            return report
        }

        @JvmStatic
        fun assertTypesValid(cu: J.CompilationUnit) {
            val report = analyzeTypes(cu)
            if(report.isNotEmpty()) {

                val reportText = report.asSequence()
                        .map { """
                             |  ${it.message}
                             |    At : ${it.cursor}
                             |    AST:
                             ${it.astElement.printTrimmed().prependIndent("|      ")}
                             |
                        """.trimMargin() }
                        .joinToString("\n")
                fail("Found missing or invalid types: \n$reportText")
            }
        }

        /**
         * Convenience method for creating an InvalidType result without having to manually specify anything except the message.
         * And a convenient place to put a breakpoint if you want to catch an invalid type in the debugger.
         */
        private fun JavaVisitor<*>.invalidTypeResult(message: String) = InvalidTypeResult(cursor, cursor.getValue(), message)
    }

    override fun visitMethodInvocation(method: J.MethodInvocation, p: MutableList<InvalidTypeResult>): J.MethodInvocation {
        val m = super.visitMethodInvocation(method, p)
        val mt = method.type
        if(mt == null) {
            p.add(invalidTypeResult("J.MethodInvocation type is null"))
            return m
        }
        if(mt.genericSignature == null) {
            p.add(invalidTypeResult("J.MethodInvocation is missing a genericSignature"))
        }
        if(!m.simpleName.equals(mt.name)) {
            p.add(invalidTypeResult("J.MethodInvocation name \"${m.simpleName}\" does not match the name in its type information \"${mt.name}\""))
        }

        return m
    }
}
