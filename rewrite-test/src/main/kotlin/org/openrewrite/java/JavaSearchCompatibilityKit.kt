package org.openrewrite.java

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.search.*

@ExtendWith(JavaParserResolver::class)
abstract class JavaSearchCompatibilityKit {
    abstract fun javaParser(): JavaParser

    @Nested
    inner class FindAnnotationTck: FindAnnotationTest

    @Nested
    inner class FindFieldsTck: FindFieldsTest

    @Nested
    inner class FindInheritedFieldsTck: FindInheritedFieldsTest

    @Nested
    inner class FindMethodTck: FindMethodTest

    @Nested
    inner class FindReferencesToVariableTck: FindReferencesToVariableTest

    @Nested
    inner class FindTypeTck: FindTypeTest

    @Nested
    inner class HasImportTck: HasImportTest

    @Nested
    inner class HasTypeTck: HasTypeTest
}
