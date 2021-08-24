/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.Java11Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaParserResolver
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Block

@ExtendWith(JavaParserResolver::class)
class VariableDeclarationsJava11Test : JavaTreeTest, Java11Test {

    @Test
    fun implicitlyDeclaredLocalVariable(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            var a = "";
            var/* comment */b = "";
            /*comment*/var c = "";
            var     d = "";
            long /* yep */ i /* comments */, /*everywhere*/ j; 
        """
    )

    @Test
    fun implicitlyDeclaredLocalAstValidation(jp: JavaParser) {
        val statements = (jp.parse("""
            import java.util.Date;
            public class Sample {
                static {
                    var a = "";
                    var /* comment */ b = 'a';
                    /*comment*/var c = new Date();
                    var     d = 1f;
                    long e, /* hello */   f = 1L;
                }
            }
        """.trimIndent())[0].classes[0].body.statements[0] as J.Block).statements
        var inferred = typeTree(statements[0])
        assertThat(TypeUtils.isOfClassType(inferred.type, "java.lang.String")).isTrue()
        inferred = typeTree(statements[1])
        assertThat(TypeUtils.asPrimitive(inferred.type)).isEqualTo(JavaType.Primitive.Char)
        inferred = typeTree(statements[2])
        assertThat(TypeUtils.isOfClassType(inferred.type, "java.util.Date")).isTrue()
        inferred = typeTree(statements[3])
        assertThat(TypeUtils.asPrimitive(inferred.type)).isEqualTo(JavaType.Primitive.Float)
        val variableDeclarations = statements[4] as J.VariableDeclarations
        assertThat(TypeUtils.asPrimitive(variableDeclarations.typeExpression!!.type)).isEqualTo(JavaType.Primitive.Long)
        val secondVariable = variableDeclarations.variables[1]
        assertThat(TypeUtils.asPrimitive(secondVariable.type)).isEqualTo(JavaType.Primitive.Long)
        assertThat((secondVariable.prefix.comments[0] as TextComment).text).isEqualTo(" hello ")
        assertThat(secondVariable.prefix.comments[0].suffix).isEqualTo("   ")
    }

    private fun typeTree(statement : Statement) : J.VarType {
        return (statement as J.VariableDeclarations).typeExpression as J.VarType
    }
}
