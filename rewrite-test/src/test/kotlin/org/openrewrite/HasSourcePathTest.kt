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
package org.openrewrite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.text.ChangeText
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextParser
import java.nio.file.Path

class HasSourcePathTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(object : ChangeText("hello jon") {
            override fun getSingleSourceApplicableTest() =
                HasSourcePath<ExecutionContext>("**/hello.txt")
        })
    }

    @Example
    @Test
    fun hasFileMatch() = rewriteRun(
        text("hello world", "hello jon") { spec -> spec.path("a/b/hello.txt") },
        text("hello world", "hello jon") { spec -> spec.path("hello.txt") }
    )

    @Test
    fun hasNoFileMatch() = rewriteRun(
        text("hello world") { spec -> spec.path("a/b/goodbye.txt") }
    )

    @Test
    fun regexMatch() = rewriteRun(
        { spec ->
            spec.recipe(object : ChangeText("hello jon") {
                override fun getSingleSourceApplicableTest() =
                    HasSourcePath<ExecutionContext>("regex", ".+\\.gradle(\\.kts)?$")
            })
        },
        text("", "hello jon") { spec -> spec.path("build.gradle") },
        text("", "hello jon") { spec -> spec.path("build.gradle.kts") },
        text("") { spec -> spec.path("pom.xml") }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1878")
    @Test
    fun githubYaml() = rewriteRun(
        { spec ->
            spec.recipe(object : ChangeText("hello jon") {
                override fun getSingleSourceApplicableTest() =
                    HasSourcePath<ExecutionContext>(".github/workflows/*.yml")
            })
        },
        text("", "hello jon") { spec -> spec.path(".github/workflows/ci.yml") },
    )
}
