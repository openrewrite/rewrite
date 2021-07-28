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
package org.openrewrite.yaml.tree

import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.YamlParser

interface YamlParserTest {
    val parser: YamlParser
        get() = YamlParser()

    val onError: (Throwable) -> Unit
        get() = { t -> t.printStackTrace() }

    /**
     * Assert that the provided yaml source is parsed and printed back out unchanged.
     * Supply a documentsAssert to inspect the parsed AST
     */
    fun assertRoundTrip(
        @Language("yml") source: String,
        afterConditions: (Yaml.Documents)->Unit = { }
    ) {
        val trimmedSource = source.trimIndent()
        val ast = parser.parse(InMemoryExecutionContext(onError), trimmedSource).first()
        afterConditions(ast)
        val after = ast.print()
        Assertions.assertThat(after).isEqualTo(trimmedSource)
    }
}
