/*
 * Copyright 2026 the original author or authors.
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
@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
package org.openrewrite.kotlin.recipe.internal

import com.tschuchort.compiletesting.KotlinCompilation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.kotlin.recipe.RecipePluginCompileFixture

/**
 * Registration fails with a raw `ClassCastException` when a consumer's `kotlin("jvm")`
 * version differs from the one rewrite-kotlin was built against
 * (https://github.com/openrewrite/rewrite/issues/8270); these pin the message replacing it.
 */
class RecipeCompilerPluginRegistrarTest {

    @ParameterizedTest
    @MethodSource("registrationFailures")
    fun `registration failure is reported as an actionable compiler error`(failure: Throwable) {
        val messageCollector = CapturingMessageCollector()

        registerRecipeExtensions(configurationWith(messageCollector)) { throw failure }

        assertThat(messageCollector.messages).singleElement().satisfies({ (severity, message) ->
            assertThat(severity).isEqualTo(CompilerMessageSeverity.ERROR)
            assertThat(message)
                .contains("Kotlin ${KotlinCompilerVersion.getVersion() ?: "unknown"}")
                .contains("built against Kotlin")
                .contains("kotlin(\"jvm\")")
                .contains(failure::class.java.name)
        })
    }

    @Test
    fun `the version rewrite-kotlin was built against comes from its own resource`() {
        val message = kotlinVersionMismatchMessage(ClassCastException("boom"))

        assertThat(message).doesNotContain("built against Kotlin unknown")
        assertThat(message).containsPattern("built against Kotlin \\d+\\.\\d+\\.\\d+")
    }

    @Test
    fun `successful registration reports nothing`() {
        val messageCollector = CapturingMessageCollector()

        registerRecipeExtensions(configurationWith(messageCollector)) { }

        assertThat(messageCollector.messages).isEmpty()
    }

    @Test
    fun `without a message collector the actionable message is thrown instead`() {
        assertThatIllegalStateException()
            .isThrownBy { registerRecipeExtensions(CompilerConfiguration()) { throw ClassCastException("boom") } }
            .withMessageContaining("kotlin(\"jvm\")")
            .withCauseInstanceOf(ClassCastException::class.java)
    }

    @Test
    fun `a discarding message collector throws rather than silently unregistering`() {
        assertThatIllegalStateException()
            .isThrownBy {
                registerRecipeExtensions(configurationWith(MessageCollector.NONE)) { throw ClassCastException("boom") }
            }
            .withMessageContaining("kotlin(\"jvm\")")
            .withCauseInstanceOf(ClassCastException::class.java)
    }

    @Test
    fun `the reported error fails compilation rather than crashing the compiler`() {
        val result = RecipePluginCompileFixture.compile("val answer = 42", "Empty.kt", FailingRegistrar())

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("kotlin(\"jvm\")")
    }

    private fun configurationWith(messageCollector: MessageCollector) = CompilerConfiguration().apply {
        put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    }

    companion object {
        @JvmStatic
        fun registrationFailures(): List<Throwable> = listOf(
            ClassCastException(
                "class org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter\$Companion cannot be cast to " +
                        "class org.jetbrains.kotlin.extensions.ExtensionPointDescriptor"
            ),
            NoSuchMethodError("org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter.registerExtension"),
            AbstractMethodError("org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension.generate"),
        )
    }
}

private class FailingRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "org.openrewrite.kotlin.recipe.test"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        registerRecipeExtensions(configuration) { throw ClassCastException("simulated version skew") }
    }
}

private class CapturingMessageCollector : MessageCollector {
    val messages: MutableList<Pair<CompilerMessageSeverity, String>> = mutableListOf()

    override fun clear() {
        messages.clear()
    }

    override fun hasErrors(): Boolean = messages.any { it.first.isError }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        messages += severity to message
    }
}
