/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpecs
import org.openrewrite.test.SourceSpecs.text

class SetFilePermissionsTest : RewriteTest {

    @Test
    fun `Revoke read permissions`() = rewriteRun(
        {spec ->
            spec.recipe(SetFilePermissions("test.txt", false, true, true))
            spec.afterRecipe { run ->
                run.results.forEach {
                    assertThat(it.after!!.fileAttributes!!.isReadable).isFalse
                    assertThat(it.after!!.fileAttributes!!.isWritable).isTrue
                    assertThat(it.after!!.fileAttributes!!.isExecutable).isTrue
                }
            }},
        text("", "") { spec -> spec.path("test.txt") }
    )

    @Test
    fun `Revoke write permissions`() = rewriteRun(
        {spec ->
            spec.recipe(SetFilePermissions("test.txt", true, false, true))
            spec.afterRecipe { run ->
                run.results.forEach {
                    assertThat(it.after!!.fileAttributes!!.isReadable).isTrue
                    assertThat(it.after!!.fileAttributes!!.isWritable).isFalse
                    assertThat(it.after!!.fileAttributes!!.isExecutable).isTrue
                }
            }},
        text("", "") { spec -> spec.path("test.txt") }
    )

    @Test
    fun `Revoke executable permissions`() = rewriteRun(
        {spec ->
            spec.recipe(SetFilePermissions("test.txt", true, true, false))
            spec.afterRecipe { run ->
                run.results.forEach {
                    assertThat(it.after!!.fileAttributes!!.isReadable).isTrue
                    assertThat(it.after!!.fileAttributes!!.isWritable).isTrue
                    assertThat(it.after!!.fileAttributes!!.isExecutable).isFalse
                }
            }},
        text("", "") { spec -> spec.path("test.txt") }
    )
}
