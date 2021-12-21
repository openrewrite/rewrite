package org.openrewrite.java.internal

import org.openrewrite.java.JavaTypeGoat
import org.openrewrite.java.JavaTypeMappingTest
import org.openrewrite.java.asParameterized
import org.openrewrite.java.tree.JavaType

class JavaReflectionTypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val typeMapping = JavaReflectionTypeMapping(mutableMapOf())
        private val goat = typeMapping.type(JavaTypeGoat::class.java).asParameterized()!!
    }

    override fun goatType(): JavaType.Parameterized = goat
}
