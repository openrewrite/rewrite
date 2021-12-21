package org.openrewrite.java.internal

import io.github.classgraph.ClassGraph
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaTypeMappingTest
import org.openrewrite.java.asParameterized
import org.openrewrite.java.tree.JavaType

class ClassgraphTypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val typeMapping = ClassgraphTypeMapping(mutableMapOf(), emptyMap(), InMemoryExecutionContext())

        private val goat = typeMapping.type(
            ClassGraph()
                .filterClasspathElements { e -> !e.endsWith(".jar") }
                .enableAnnotationInfo()
                .enableMemoryMapping()
                .enableClassInfo()
                .enableFieldInfo()
                .enableMethodInfo()
                .acceptClasses("org.openrewrite.java.*")
                .scan()
                .getClassInfo("org.openrewrite.java.JavaTypeGoat")
        ).asParameterized()!!
    }

    override fun goatType(): JavaType.Parameterized = goat
}
