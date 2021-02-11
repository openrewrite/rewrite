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
package org.openrewrite.xml

import org.assertj.core.api.Assertions
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.TreePrinter
import org.openrewrite.xml.tree.Xml

open class XmlVisitorTest {

    private val parser = XmlParser()

    fun assertChanged(
            visitorMapped: (Xml.Document) -> XmlVisitor<ExecutionContext>,
            before: String,
            after: String
    ) {
        val source = parser.parse(*(arrayOf(before.trimIndent()))).first()
        val result = visitorMapped(source).visit(source,
                InMemoryExecutionContext { t: Throwable? -> Assertions.fail<Any>("Visitor threw an exception", t) })
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result!!.printTrimmed(TreePrinter.identity<Any>())).isEqualTo(after.trimIndent())
    }
}
