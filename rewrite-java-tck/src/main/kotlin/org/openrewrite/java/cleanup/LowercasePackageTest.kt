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
package org.openrewrite.java.cleanup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.PathUtils
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.nio.file.Paths

interface LowercasePackageTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(LowercasePackage())
    }

    @Test
    fun lowerCasePackage(jp: JavaParser.Builder<*, *>) = rewriteRun(
        {spec -> spec.parser(jp) },
        java("""
            package com.UPPERCASE.CamelCase;
            class A {}
        """,
        """
            package com.uppercase.camelcase;
            class A {}
        """) { spec ->
            spec.afterRecipe { cu ->
                assertThat(PathUtils.equalIgnoringSeparators(cu.sourcePath, Paths.get("com/uppercase/camelcase/A.java"))).isTrue
            }
        }
    )
}
