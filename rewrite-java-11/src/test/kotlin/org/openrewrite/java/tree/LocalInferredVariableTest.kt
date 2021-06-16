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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.*
import org.openrewrite.java.JavaTreeTest.NestingLevel.Block
import org.openrewrite.java.JavaTreeTest.NestingLevel.Class

@ExtendWith(JavaParserResolver::class)
class VariableDeclarations11Test : JavaTreeTest {
    fun javaParser(): Java11Parser.Builder = Java11Parser.builder()

    @Test
    fun implicitlyDeclaredLocalVariable(jp: JavaParser) {

        assertParsePrintAndProcess(
            jp, Block, """
                var a = "";
                var/* comment */b = "";
                /*comment*/var c = "";
                var     d = "";
                long /* yep */ i /* comments */, /*everywhere*/ j; 
            """
        )
    }

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
        assertThat((inferred.type as JavaType.FullyQualified).fullyQualifiedName).isEqualTo("java.lang.String")
        assertThat(inferred.kind).isEqualTo(J.InferredType.Kind.LocalVariable)
        inferred = typeTree(statements[1])
        assertThat(inferred.type as JavaType.Primitive).isEqualTo(JavaType.Primitive.Char)
        assertThat(inferred.kind).isEqualTo(J.InferredType.Kind.LocalVariable)
        inferred = typeTree(statements[2])
        assertThat((inferred.type as JavaType.FullyQualified).fullyQualifiedName).isEqualTo("java.util.Date")
        assertThat(inferred.kind).isEqualTo(J.InferredType.Kind.LocalVariable)
        inferred = typeTree(statements[3])
        assertThat(inferred.type as JavaType.Primitive).isEqualTo(JavaType.Primitive.Float)
        assertThat(inferred.kind).isEqualTo(J.InferredType.Kind.LocalVariable)
        val variableDeclarations = statements[4] as J.VariableDeclarations
        assertThat(variableDeclarations.typeExpression!!.type as JavaType.Primitive).isEqualTo(JavaType.Primitive.Long)
        assertThat(inferred.kind).isEqualTo(J.InferredType.Kind.LocalVariable)
        val secondVariable = variableDeclarations.variables[1]
        assertThat(secondVariable.type as JavaType.Primitive).isEqualTo(JavaType.Primitive.Long)
        assertThat(secondVariable.prefix.comments[0].text).isEqualTo(" hello ")
        assertThat(secondVariable.prefix.comments[0].suffix).isEqualTo("   ")
    }

    fun typeTree(statement : Statement) : J.InferredType {
        return (statement as J.VariableDeclarations).typeExpression as J.InferredType
    }
}
