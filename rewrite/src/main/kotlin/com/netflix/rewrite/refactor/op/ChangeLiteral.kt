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
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.AstTransform
import com.netflix.rewrite.ast.Expression
import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.ast.Type
import com.netflix.rewrite.refactor.RefactorVisitor
import org.apache.commons.lang.StringEscapeUtils

class ChangeLiteral(val transform: (Any?) -> Any?,
                    override val ruleName: String = "change-literal"): RefactorVisitor<Tr.Literal>() {

    override fun visitExpression(expr: Expression): List<AstTransform<Tr.Literal>> {
        return LiteralVisitor(ruleName).visit(expr)
    }

    private inner class LiteralVisitor(override val ruleName: String): RefactorVisitor<Tr.Literal>() {

        override fun visitLiteral(literal: Tr.Literal): List<AstTransform<Tr.Literal>> {
            val transformed = transform.invoke(literal.value)
            return if(transformed != literal.value) {
                transform(literal) {
                    val transformedValueSource = when(literal.type) {
                        Type.Primitive.Boolean -> transformed.toString()
                        Type.Primitive.Byte -> transformed.toString()
                        Type.Primitive.Char -> {
                            val escaped = StringEscapeUtils.escapeJavaScript(transformed.toString())

                            // there are two differences between javascript escaping and character escaping
                            "'" + when(escaped) {
                                "\\\"" -> "\""
                                "\\/" -> "/"
                                else -> escaped
                            } + "'"
                        }
                        Type.Primitive.Double -> "${transformed}d"
                        Type.Primitive.Float -> "${transformed}f"
                        Type.Primitive.Int -> transformed.toString()
                        Type.Primitive.Long -> "${transformed}L"
                        Type.Primitive.Short -> transformed.toString()
                        Type.Primitive.Void -> transformed.toString()
                        Type.Primitive.String -> "\"${StringEscapeUtils.escapeJava(transformed.toString())}\""
                        Type.Primitive.None -> ""
                        Type.Primitive.Wildcard -> "*"
                        Type.Primitive.Null -> "null"
                        else -> error("Undefined primitive")
                    }

                    copy(value = transformed, valueSource = transformedValueSource)
                }
            } else emptyList()
        }
    }
}