package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Expression
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Type
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
                        Type.Tag.Boolean -> transformed.toString()
                        Type.Tag.Byte -> transformed.toString()
                        Type.Tag.Char -> {
                            val escaped = StringEscapeUtils.escapeJavaScript(transformed.toString())

                            // there are two differences between javascript escaping and character escaping
                            "'" + when(escaped) {
                                "\\\"" -> "\""
                                "\\/" -> "/"
                                else -> escaped
                            } + "'"
                        }
                        Type.Tag.Double -> "${transformed}d"
                        Type.Tag.Float -> "${transformed}f"
                        Type.Tag.Int -> transformed.toString()
                        Type.Tag.Long -> "${transformed}L"
                        Type.Tag.Short -> transformed.toString()
                        Type.Tag.Void -> transformed.toString()
                        Type.Tag.String -> "\"${StringEscapeUtils.escapeJava(transformed.toString())}\""
                        Type.Tag.None -> ""
                        Type.Tag.Wildcard -> "*"
                        Type.Tag.Null -> "null"
                    }

                    copy(value = transformed, valueSource = transformedValueSource)
                })
            } else emptyList()
        }
    }
}