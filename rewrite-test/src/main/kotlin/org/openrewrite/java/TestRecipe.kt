package org.openrewrite.java

import org.openrewrite.Recipe

open class TestRecipe : Recipe() {
    override fun getDisplayName(): String {
        return name
    }
}