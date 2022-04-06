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
package org.openrewrite.groovy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.util.*

class DelegatingGroovyVisitorTest {

    @Test
    fun reachesGroovyTypeViaJavaAst() {
        val cu = GroovyParser.builder().build()
            .parse("""
                class A{
                    void foo(Map<String, String> a) {
                        foo("a" : "b")
                    }
                }
            """)[0]!!

        val refs = Collections.newSetFromMap(IdentityHashMap<JavaType, Boolean>())

        val typeVisitorWrapper = object : JavaIsoVisitor<Int>() {
            override fun visitType(javaType: JavaType?, p: Int): JavaType? {
                if(cursor.parent != null && cursor.parent!!.getValue<J>() is J.MethodInvocation) {
                    refs.add(javaType)
                }
                return javaType
            }
        }

        val groovyTypeVisitor = DelegatingGroovyVisitor<JavaIsoVisitor<Int>, Int>(typeVisitorWrapper)

        groovyTypeVisitor.visit(cu, 0)

        assertThat(refs.size).isEqualTo(1)
    }
}
