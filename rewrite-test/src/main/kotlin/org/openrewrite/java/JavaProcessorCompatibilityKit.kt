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
package org.openrewrite.java

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.format.BlankLinesTest
import org.openrewrite.java.format.TabsAndIndentsTest
import org.openrewrite.java.search.*
import org.openrewrite.java.tree.TypeTreeTest

@ExtendWith(JavaParserResolver::class)
abstract class JavaProcessorCompatibilityKit {
    abstract fun javaParser(): JavaParser.Builder<*, *>

    @Nested
    inner class AddImportTck : AddImportTest

    @Nested
    inner class BlankLinesTck : BlankLinesTest

    @Nested
    inner class TabsAndIndentsTck : TabsAndIndentsTest

    @Nested
    inner class FindAnnotationTck : FindAnnotationTest

    @Nested
    inner class FindFieldTck : FindFieldTest

    @Nested
    inner class FindInheritedFieldTck : FindInheritedFieldTest

    @Nested
    inner class FindMethodTck : FindMethodTest

    @Nested
    inner class FindTypeTck : FindTypeTest

    @Nested
    inner class OrderImportsTck : OrderImportsTest

    @Nested
    inner class TypeTreeTck : TypeTreeTest
}
