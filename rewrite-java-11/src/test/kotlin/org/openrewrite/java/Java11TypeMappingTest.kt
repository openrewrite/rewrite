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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.util.concurrent.atomic.AtomicReference

class Java11TypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val goat = Java11TypeMappingTest::class.java.getResourceAsStream("/JavaTypeGoat.java")!!
            .bufferedReader().readText()

        private val goatCu = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, goat)[0]
    }

    override fun classType(fqn: String): JavaType.FullyQualified {
        val type = AtomicReference<JavaType.FullyQualified>()
        object : JavaVisitor<Int>() {
            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: Int): J {
                if (classDecl.type?.fullyQualifiedName == fqn) {
                    type.set(classDecl.type)
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.visitNonNull(goatCu, 0)
        return type.get()!!
    }

    @Disabled("Move to JavaTypeGoat")
    @Issue("https://github.com/openrewrite/rewrite/issues/1318")
    @Test
    fun methodInvocationOnUnknownType() {
        val source = """
            import java.util.ArrayList;
            // do not import List to create an UnknownType
            
            class Test {
                class Base {
                    private int foo;
                    public boolean setFoo(int foo) {
                        this.foo = foo;
                    }
                    public int getFoo() {
                        return foo;
                    }
                }
                List<Base> createUnknownType(List<Integer> values) {
                    List<Base> bases = new ArrayList<>();
                    values.forEach((v) -> {
                        Base b = new Base();
                        b.setFoo(v);
                        bases.add(b);
                    });
                    return bases;
                }
            }
        """.trimIndent()
        val cu = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { t -> fail(t) }, source)
        assertThat(cu).isNotNull
    }
}
