package com.netflix.rewrite.gradle

import org.gradle.internal.service.ServiceRegistry

enum class Styling { Bold, Green, Yellow, Red }

class VersionNeutralTextOutput(val gradleTextOutput: Any) {
    fun text(v: Any) {
        gradleTextOutput.javaClass.getDeclaredMethod("text", Any::class.java).invoke(gradleTextOutput, v)
    }

    fun println(v: Any) {
        gradleTextOutput.javaClass.getDeclaredMethod("println", Any::class.java).invoke(gradleTextOutput, v)
    }
}

/**
 * Bridges the internal Gradle 2 and 3 APIs for styled text output to provide
 * a single backwards-compatible interface.
 */
class StyledTextService(registry: ServiceRegistry) {
    val textOutput: Any

    init {
        var factoryClass: Class<*>
        try {
            factoryClass = Class.forName("org.gradle.internal.logging.text.StyledTextOutputFactory")
        } catch(ignore: ClassNotFoundException) {
            factoryClass = Class.forName("org.gradle.logging.StyledTextOutputFactory")
        }

        val textOutputFactory = registry.get(factoryClass)
        textOutput = textOutputFactory.javaClass.getDeclaredMethod("create", String::class.java).invoke("rewrite")
    }

    fun withStyle(styling: Styling): VersionNeutralTextOutput {
        var styleClass: Class<*>
        try {
            styleClass = Class.forName("org.gradle.internal.logging.text.StyledTextOutput\$Style")
        } catch(ignore: ClassNotFoundException) {
            styleClass = Class.forName("org.gradle.logging.StyledTextOutput\$Style")
        }

        fun styleByName(name: String) =
            VersionNeutralTextOutput(styleClass.getDeclaredMethod("valueOf", String::class.java).invoke(null, name))

        return when(styling) {
            Styling.Bold -> styleByName("UserInput")
            Styling.Green -> styleByName("Identifier")
            Styling.Yellow -> styleByName("Description")
            Styling.Red -> styleByName("Failure")
        }
    }

    fun text(text: String): StyledTextService {
        textOutput.javaClass.getDeclaredMethod("text", Any::class.java).invoke(textOutput, text)
        return this
    }

    fun println(text: String): StyledTextService {
        textOutput.javaClass.getDeclaredMethod("println", Any::class.java).invoke(textOutput, text)
        return this
    }
}