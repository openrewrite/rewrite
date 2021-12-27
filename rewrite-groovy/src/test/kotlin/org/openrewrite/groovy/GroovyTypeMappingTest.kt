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
package org.openrewrite.groovy

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaTypeMappingTest
import org.openrewrite.java.asParameterized
import org.openrewrite.java.tree.JavaType

@Disabled
class GroovyTypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val goat = GroovyTypeMappingTest::class.java.getResourceAsStream("/GroovyTypeGoat.groovy")!!
            .bufferedReader().readText()
    }

    override fun goatType(): JavaType.Parameterized = GroovyParser.builder()
        .logCompilationWarningsAndErrors(true)
        .build()
        .parse(InMemoryExecutionContext { t -> fail(t) }, goat)[0]
        .classes[0]
        .type
        .asParameterized()!!
}
