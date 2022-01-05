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
package org.openrewrite.java.internal

import io.github.classgraph.ClassGraph
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.java.JavaTypeGoat
import org.openrewrite.java.JavaTypeVisitor
import org.openrewrite.java.tree.JavaType
import java.util.*

class TypeMappingUniquenessTest {
    private val uniqueTypes: MutableSet<JavaType> = Collections.newSetFromMap(IdentityHashMap())

    @Test
    fun noMoreTypesAdded() {
        val typeBySignature = mutableMapOf<String, Any>()

        val reflection = JavaReflectionTypeMapping(typeBySignature).type(JavaTypeGoat::class.java)
        newUniqueTypes(reflection)

        val classgraph = ClassgraphTypeMapping(typeBySignature, emptyMap()).type(
            ClassGraph()
                .filterClasspathElements { e -> !e.endsWith(".jar") }
                .enableAnnotationInfo()
                .enableMemoryMapping()
                .enableClassInfo()
                .enableFieldInfo()
                .enableMethodInfo()
                .ignoreClassVisibility()
                .acceptClasses("org.openrewrite.java.*")
                .scan()
                .getClassInfo("org.openrewrite.java.JavaTypeGoat")
        )
        newUniqueTypes(classgraph, true)
    }

    private fun newUniqueTypes(root: JavaType, report: Boolean = false) {
        var newUnique = false
        object : JavaTypeVisitor<Int>() {
            override fun visit(javaType: JavaType?, p: Int): JavaType? {
                // temporarily suppress failing if the _only_ difference is the presence of the Unknown type
                if (javaType is JavaType && javaType != JavaType.Unknown.getInstance()) {
                    if (uniqueTypes.add(javaType)) {
                        if(report) {
                            newUnique = true
                            println(javaType)
                        }
                        return super.visit(javaType, p)
                    }
                }
                return javaType
            }
        }.visit(root, 0)

        if(report && newUnique) {
            fail("Found new unique types there should have been none.")
        }
    }
}
