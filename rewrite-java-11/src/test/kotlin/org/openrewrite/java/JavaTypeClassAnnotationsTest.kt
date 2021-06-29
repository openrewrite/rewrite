package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.NameTree
import kotlin.collections.HashSet

class JavaTypeClassAnnotationsTest {
    @Test
    fun test() {
        // create java parser with bytes of class read previously
        val javaSource = """
            import java.util.jar.Pack200;
            public class Dummy {
                private Pack200 p200;
            }
        """.trimIndent()


        val visitor = object: JavaIsoVisitor<HashSet<String>>() {
            override fun <N : NameTree?> visitTypeName(nameTree: N, p: HashSet<String>): N {
               val typeName = super.visitTypeName(nameTree, p)
                if(typeName.toString().contains("Pack200")) {
                    val type = typeName?.getType()
                    val javaType = JavaType.Class.build(type.asFullyQualified()?.fullyQualifiedName);
                    p.add(javaType.annotations[0].fullyQualifiedName);
                }
                return typeName
            }
        };

        val cus = JavaParser.fromJavaVersion().build().parse(javaSource);
        val executionContext = HashSet<String>()
        visitor.visit(cus.get(0), executionContext)
        assertThat(executionContext).hasSize(1)
        assertThat(executionContext).contains("java.lang.Deprecated")
    }



}