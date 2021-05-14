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