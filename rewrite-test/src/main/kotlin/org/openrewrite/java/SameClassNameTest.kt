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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile

interface SameClassNameTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = ExampleRecipe()

    @Disabled
    @Test
    fun canParseTheSameJavaClass(jp: JavaParser) =
        assertUnchanged(
            jp,
            before = """
                package com.foo;
                class A {}
            """,
            dependsOn = arrayOf("""
                package a.b.c;
                class Bar {}
            """, """
                package d.e.f;
                class Bar {}
            """
            )
        )

    class ExampleRecipe : Recipe() {
        override fun getDisplayName(): String {
            return "Do nothing"
        }

        override fun visit(before: MutableList<SourceFile>, ctx: ExecutionContext): MutableList<SourceFile> {
            return super.visit(before, ctx)
        }
    }


}