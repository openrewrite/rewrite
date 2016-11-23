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
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.refactor.RefactorVisitor
import org.apache.commons.lang.StringEscapeUtils

class ChangeLiteralArgument(val expr: Expression, val transform: (Any?) -> Any?): RefactorVisitor() {

    override fun visitExpression(expr: Expression): List<AstTransform<*>> {
        if(expr.id == this.expr.id) {
            return LiteralVisitor().visit(expr)
        }
        return super.visitExpression(expr)
    }

    private inner class LiteralVisitor(): RefactorVisitor() {
        override fun visitLiteral(literal: Tr.Literal): List<AstTransform<*>> {
            val transformed = transform.invoke(literal.value)
            return if(transformed != literal.value) {
                listOf(AstTransform<Tr.Literal>(this@ChangeLiteralArgument.cursor().parent() + cursor()) {
                    val transformedValueSource = when(literal.typeTag) {
                        TypeTag.Boolean -> transformed.toString()
                        TypeTag.Byte -> transformed.toString()
                        TypeTag.Char -> {
                            val escaped = StringEscapeUtils.escapeJavaScript(transformed.toString())

                            // there are two differences between javascript escaping and character escaping
                            "'" + when(escaped) {
                                "\\\"" -> "\""
                                "\\/" -> "/"
                                else -> escaped
                            } + "'"
                        }
                        TypeTag.Double -> "${transformed}d"
                        TypeTag.Float -> "${transformed}f"
                        TypeTag.Int -> transformed.toString()
                        TypeTag.Long -> "${transformed}L"
                        TypeTag.Short -> transformed.toString()
                        TypeTag.Void -> transformed.toString()
                        TypeTag.String -> "\"${StringEscapeUtils.escapeJava(transformed.toString())}\""
                        TypeTag.None -> ""
                        TypeTag.Wildcard -> "*"
                        TypeTag.Null -> "null"
                    }

                    copy(value = transformed, valueSource = transformedValueSource)
                })
            } else emptyList()
        }
    }
}