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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.format.AutoFormat
import org.openrewrite.java.tree.J

/**
 * @author Alex Boyko
 */
interface JavaParserTest : JavaRecipeTest {

    @Test
    fun incompleteAssignment(jp: JavaParser) {

       val source =
       """
           @Deprecated(since=)
           public class A {}
       """.trimIndent();

        val cu = JavaParser.fromJavaVersion().build().parse(source).get(0)

        assertThat(cu.printAll()).isEqualTo(source)

        val newCu = JavaVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext()) as J.CompilationUnit

        assertThat(newCu.printAll()).isEqualTo(source)

    }

    @Test
    fun javaModulePrints1(jp: JavaParser) = assertUnchanged(
        recipe = AutoFormat(),
        before = """
            module com.example.foo {
                opens com.example.foo.internal to com.example.foo.network, com.example.foo.probe;
            }
        """
    )

    @Test
    fun printModule(jp: JavaParser) {
        val mod = """
            module com.example.foo {
                requires com.example.foo.http;
                requires java.logging;
                
                requires transitive com.example.foo.network;
                
                exports com.example.foo.bar;
                exports com.example.foo.internal to com.example.foo.probe;
                
                opens com.example.foo.quux;
                opens com.example.foo.internal to com.example.foo.network,
                                                  com.example.foo.probe;
                
                uses com.example.foo.spi.Intf;
                provides com.example.foo.spi.Intf with com.example.foo.Impl;
            }
        """.trimIndent()
        val cu = jp.parse(mod)
        val after = cu[0].printAll()
        assertThat(after).isEqualTo(mod)

    }
    @Test
    fun javaModulePrints(jp: JavaParser) = assertUnchanged(
        recipe = AutoFormat(),
        before = """
            module com.example.foo {
                requires com.example.foo.http;
                requires java.logging;
                
                requires transitive com.example.foo.network;
                
                exports com.example.foo.bar;
                exports com.example.foo.internal to com.example.foo.probe;
                
                opens com.example.foo.quux;
                opens com.example.foo.internal to com.example.foo.network,
                                                  com.example.foo.probe;
                
                uses com.example.foo.spi.Intf;
                provides com.example.foo.spi.Intf with com.example.foo.Impl;
            }
        """
    )
    @Test
    fun javaModuleTest(jp: JavaParser) {
        val source = """
            module com.example.foo {
                requires com.example.foo.http;
                requires java.logging;
                
                requires transitive com.example.foo.network;
                
                exports com.example.foo.bar;
                exports com.example.foo.internal to com.example.foo.probe;
                
                opens com.example.foo.quux;
                opens com.example.foo.internal to com.example.foo.network,
                                                  com.example.foo.probe;
                
                uses com.example.foo.spi.Intf;
                provides com.example.foo.spi.Intf with com.example.foo.Impl;
            }
        """
        val cu = JavaParser.fromJavaVersion().build().parse(source).get(0)
        assertThat(cu).isNotNull
        val jc = cu as J.CompilationUnit
        assertThat(jc).isNotNull
    }

}
