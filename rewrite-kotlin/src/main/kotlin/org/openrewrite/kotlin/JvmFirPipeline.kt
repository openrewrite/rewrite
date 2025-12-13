/**
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.common.reportCompilationCancelled
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.defaultJvmPlatform
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PerformanceManagerImpl

/**
 * Pipeline to compile Kotlin code to FIR
 *
 * Inspired by [org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline]
 */
class JvmFirPipeline(
    override val defaultPerformanceManager: PerformanceManager = PerformanceManagerImpl(
        defaultJvmPlatform,
        "Kotlin to FIR compiler"
    ),
) : AbstractCliPipeline<K2JVMCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JVMCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmFrontendPipelineArtifact> {
        return JvmConfigurationPipelinePhase then JvmFrontendPipelinePhaseIgnoringCompilationErrors
    }

    /**
     * @see [org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline.isKaptMode]
     */
    override fun isKaptMode(arguments: K2JVMCompilerArguments): Boolean {
        return arguments.pluginOptions?.any { it.startsWith("plugin:org.jetbrains.kotlin.kapt3") } == true
    }

    fun execute(
        arguments: K2JVMCompilerArguments,
        originalMessageCollector: MessageCollector,
        disposable: Disposable
    ): JvmFrontendPipelineArtifact? {
        val messageCollector = GroupingMessageCollector(
            originalMessageCollector,
            arguments.allWarningsAsErrors,
            arguments.reportAllWarnings
        )
        val argumentsInput = ArgumentsPipelineArtifact(
            arguments,
            Services.EMPTY,
            disposable,
            messageCollector,
            defaultPerformanceManager
        )

        fun reportException(e: Throwable) {
            MessageCollectorUtil.reportException(
                messageCollector,
                e
            ) // TODO (KT-73575): investigate reporting in case of OOM
        }

        return try {
            runPhasedPipeline(argumentsInput)
        } catch (e: RuntimeException) {
            when (val cause = e.cause) {
                is CompilationCanceledException -> {
                    messageCollector.reportCompilationCancelled(cause)
                }
                else -> reportException(e)
            }
            throw e
        } catch (t: Throwable) {
            reportException(t)
            throw t
        } finally {
            messageCollector.flush()
        }
    }

    private fun runPhasedPipeline(input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>): JvmFrontendPipelineArtifact? {
        val compoundPhase = createCompoundPhase(input.arguments)
        val phaseConfig = PhaseConfig()
        val context = PipelineContext(
            input.messageCollector,
            input.diagnosticCollector,
            input.performanceManager,
            renderDiagnosticInternalName = input.arguments.renderInternalDiagnosticNames,
            kaptMode = isKaptMode(input.arguments)
        )
        try {
            return compoundPhase.invokeToplevel(
                phaseConfig,
                context,
                input
            )
        } finally {
            CheckCompilationErrors.CheckDiagnosticCollector.reportDiagnosticsToMessageCollector(context)
        }
    }
}

/**
 * [JvmFrontendPipelinePhase] but without [CheckCompilationErrors.CheckDiagnosticCollector] throwing Exception on compile errors.
 */
object JvmFrontendPipelinePhaseIgnoringCompilationErrors : PipelinePhase<ConfigurationPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmFrontendPipelinePhaseIgnoringCompilationErrors",
    postActions = setOf(PerformanceNotifications.AnalysisFinished)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): JvmFrontendPipelineArtifact? {
        return JvmFrontendPipelinePhase.executePhase(input)
    }
}
