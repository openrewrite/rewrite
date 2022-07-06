package org.openrewrite.internal

import org.junit.jupiter.api.Test
import org.openrewrite.Option
import org.openrewrite.Recipe

class RecipeIntrospectionUtilsTest {

    @Test
    fun kotlinNonNullConstructorArgs() {
        RecipeIntrospectionUtils.constructRecipe(TestRecipe::class.java)
    }

    enum class EnumOption {
        Value
    }

    class TestRecipe constructor(
        @Option val option1: String,
        @Option val option2: EnumOption,
        @Option val option3: List<String>,
        @Option val option4: Boolean
    ) : Recipe() {
        override fun getDisplayName() = "Test"
    }
}
