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
package org.openrewrite.protobuf

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.internal.StringUtils
import org.openrewrite.protobuf.tree.Proto
import org.openrewrite.protobuf.tree.Space

open class ProtoParserTest {
    private val parser: ProtoParser = ProtoParser()

    fun assertUnchanged(@Language("protobuf") before: String, withAst: (Proto.Document) -> Unit = {}) {
        val protoDocument = parser.parse(InMemoryExecutionContext { t -> t.printStackTrace() },
            StringUtils.trimIndent(before)).iterator().next()
        assertThat(protoDocument.printAll()).`as`("Source should not be changed").isEqualTo(
            StringUtils.trimIndent(before))
        val p2 = FindNonBlankWhitespace().visitNonNull(protoDocument, 0) as Proto.Document
        if(p2 !== protoDocument) {
            Assertions.fail<Any>("Found non-whitespace characters inside whitespace. Something didn't parse correctly:\n%s",
                p2.printAll())
        }
        withAst(protoDocument)
    }
}

class FindNonBlankWhitespace : ProtoVisitor<Int>() {
    override fun visitSpace(space: Space, p: Int): Space {
        if (!StringUtils.containsOnlyWhitespaceAndComments(space.whitespace)) {
            return space.withWhitespace("/*whitespace ->*/${space.whitespace}/*<-*/")
        }
        return space
    }
}
