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

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.fail
import org.openrewrite.java.tree.J
import java.io.IOException
import java.io.UncheckedIOException

/**
 * Diagnosing the cause of a recipe malfunction when a recipe makes
 * an incorrect change to a particular file on the Moderne SaaS.
 *
 * These tests are only ever meant to be temporary, as the bearer token
 * is both sensitive and temporary.
 *
 * Using this in a downstream project requires the inclusion of a runtime
 * dependency on com.squareup.okhttp3:okhttp:latest.release.
 */
abstract class RecipeDiagnosticTest {
    private val treeSerializer = TreeSerializer<J.CompilationUnit>()
    private val httpClient = OkHttpClient.Builder().build()

    fun assertChanged(
        recipe: Recipe,
        moderneAstLink: String,
        moderneApiBearerToken: String,
        after: String
    ) {
        try {
            val request = Request.Builder()
                .url(moderneAstLink)
                .header("Authorization", moderneApiBearerToken)
                .build()

            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected status $response" }
                val responseBody: ResponseBody = response.body ?:
                throw IllegalStateException("No response body")
                val source = treeSerializer.read(responseBody.byteStream())

                val results = recipe
                    .run(
                        listOf(source),
                        InMemoryExecutionContext { t -> Assertions.fail<Any>("Recipe threw an exception", t) },
                        2
                    )

                if (results.isEmpty()) {
                    Assertions.fail<Any>("The recipe must make changes")
                }

                val result = results.first()

                Assertions.assertThat(result).`as`("The recipe must make changes").isNotNull
                Assertions.assertThat(result!!.after).isNotNull
                Assertions.assertThat(result.after!!.print(null))
                    .isEqualTo(after.trimIndent())
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    fun assertUnchanged(
        recipe: Recipe,
        moderneAstLink: String,
        moderneApiBearerToken: String
    ) {
        try {
            val request = Request.Builder()
                .url(moderneAstLink)
                .header("Authorization", moderneApiBearerToken)
                .build()

            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected status $response" }
                val responseBody: ResponseBody = response.body ?:
                    throw IllegalStateException("No response body")
                val source = treeSerializer.read(responseBody.byteStream())

                val results = recipe
                    .run(
                        listOf(source),
                        InMemoryExecutionContext { t -> Assertions.fail<Any>("Recipe threw an exception", t) },
                        2
                    )

                results.forEach { result ->
                    if (result.diff().isEmpty()) {
                        fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
                    }
                }

                for (result in results) {
                    Assertions.assertThat(result.after?.print(null))
                        .`as`("The recipe must not make changes")
                        .isEqualTo(result.before?.print(null))
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}
