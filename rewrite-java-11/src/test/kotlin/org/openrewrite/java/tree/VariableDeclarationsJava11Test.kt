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
import org.openrewrite.java.*
import org.openrewrite.java.tree.JavaTreeTest.NestingLevel.Block

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
    fun string(jp: JavaParser) {
        val statements = (jp.parse(
            """
                public class Test { 
                    static {
                        var a = "";
                    }
                }
            """
        )[0].classes[0].body.statements[0] as J.Block).statements
        assertThat(typeTree(statements[0]).type?.asFullyQualified()?.fullyQualifiedName).isEqualTo("java.lang.String")
    }

    @Test
    fun date(jp: JavaParser) {
        val statements = (jp.parse(
            """
                import java.util.Date;
                public class Test { 
                    static {
                        var a = new Date();
                    }
                }
            """
        )[0].classes[0].body.statements[0] as J.Block).statements
        assertThat(typeTree(statements[0]).type?.asFullyQualified()?.fullyQualifiedName).isEqualTo("java.util.Date")
    }

    @Test
    fun float(jp: JavaParser) {
        val statements = (jp.parse(
            """
                import java.util.Date;
                public class Test { 
                    static {
                        var a = 1f;
                    }
                }
            """
        )[0].classes[0].body.statements[0] as J.Block).statements
        assertThat(typeTree(statements[0]).type).isEqualTo(JavaType.Primitive.Float)
    }

    private fun typeTree(statement: Statement): J.Identifier {
        assertThat(statement.markers.findFirst(JavaVarKeyword::class.java).isPresent)
        return (statement as J.VariableDeclarations).typeExpression as J.Identifier
    }
}
