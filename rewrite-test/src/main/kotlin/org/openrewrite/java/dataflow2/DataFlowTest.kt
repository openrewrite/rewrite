/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow2

import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.openrewrite.Cursor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

interface DataFlowTest {

    fun assertPrevious(cu: J.CompilationUnit, programPointSignature: String, entryOrExit: ProgramPoint, vararg previousSignatures: String) {
        assertPrevious(cu, programPointSignature, programPointInScopeOf = null, entryOrExit, *previousSignatures)
    }

    /**
     * Find a ProgramPoint which is a descendant of the specified type
     */
    fun assertPrevious(
        cu: J.CompilationUnit,
        programPointSignature: String,
        programPointInScopeOf: Class<out J>?,
        entryOrExit: ProgramPoint,
        vararg previousSignatures: String
    ) {
        Assertions.assertThat(cu).isNotNull
        val c :Cursor = findProgramPoint(cu, programPointInScopeOf, programPointSignature)
        Assertions.assertThat(c).isNotNull
        val dfg = DataFlowGraph(cu)
        val pps :Collection<Cursor> = dfg.previousIn(c, entryOrExit)
        Assertions.assertThat(pps).isNotNull
        Assertions.assertThat(pps).isNotEmpty

        val actual :List<String> = pps.stream().map { prev -> Utils.print(prev) }.collect(Collectors.toList())
        val expected :List<String> = previousSignatures.asList()

        AssertionsForClassTypes
            .assertThat(actual)
            .withFailMessage("previous($programPointSignature, $entryOrExit)\nexpected: $expected\n but was: $actual")
            .isEqualTo(expected)
    }

    /**
     * Find a ProgramPoint which is a descendant of the specified type
     */
    fun findProgramPoint(cu: J.CompilationUnit, inScopeOfType: Class<out J>?, ppToFind: String): Cursor {
        val pp: AtomicReference<Cursor> = AtomicReference()
        FindProgramPointVisitor(inScopeOfType, ppToFind).visit(cu, pp)
        return pp.get()
    }

    class FindProgramPointVisitor(private val inScopeOfType: Class<out J>?, private val ppToFind: String) :
        JavaIsoVisitor<AtomicReference<Cursor>>() {

        private fun isInScopeOfType(cursor: Cursor, p: AtomicReference<Cursor>): Boolean {
            return p.get() == null && (inScopeOfType == null || cursor.firstEnclosing(inScopeOfType) != null)
        }

        override fun visitLiteral(literal: J.Literal, p: AtomicReference<Cursor>): J.Literal {
            super.visitLiteral(literal, p)
            if (isInScopeOfType(cursor, p) && ppToFind == literal.value) {
                p.set(cursor)
            }
            return literal
        }

        override fun visitIdentifier(identifier: J.Identifier, p: AtomicReference<Cursor>): J.Identifier {
            super.visitIdentifier(identifier, p)
            if (isInScopeOfType(cursor, p) && identifier.printPP(cursor).equals(ppToFind)) {
                p.set(cursor)
            }
            return identifier
        }

        override fun visitStatement(statement: Statement, p: AtomicReference<Cursor>): Statement {
            super.visitStatement(statement, p)
            if (isInScopeOfType(cursor, p) && ppToFind == printProgramPoint(statement, cursor)) {
                p.set(cursor)
            }
            return statement
        }

        override fun visitExpression(expression: Expression, p: AtomicReference<Cursor>): Expression {
            super.visitExpression(expression, p)
            if (isInScopeOfType(cursor, p) && ppToFind == printProgramPoint(expression, cursor)) {
                p.set(cursor)
            }
            return expression
        }

        override fun visitVariable(variable: J.VariableDeclarations.NamedVariable, p: AtomicReference<Cursor>): J.VariableDeclarations.NamedVariable {
            super.visitVariable(variable, p)
            if (isInScopeOfType(cursor, p) && ppToFind == printProgramPoint(variable, cursor)) {
                p.set(cursor)
            }
            return variable
        }

        override fun visitElse(elze: J.If.Else, p: AtomicReference<Cursor>): J.If.Else {
            super.visitElse(elze, p)
            if (isInScopeOfType(cursor, p) && ppToFind == printProgramPoint(elze, cursor)) {
                p.set(cursor)
            }
            return elze
        }

        private fun printProgramPoint(p: ProgramPoint, c: Cursor): String {
            return (p as J).print(c).replace("\n", " ").replace(" +".toRegex(), " ").trim { it <= ' ' }
        }
    }


}