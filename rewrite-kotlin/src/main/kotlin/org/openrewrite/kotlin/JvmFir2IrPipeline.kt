/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.openrewrite.kotlin

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.common.profiling.ProfilingCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.reportCompilationCancelled
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.AbstractConfigurationPhase
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelineStepException
import org.jetbrains.kotlin.cli.pipeline.SuccessfulPipelineExecutionException
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.util.CompilerType
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.forEachStringMeasurement

/**
 * Inspired by [org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline]
 */
class JvmFir2IrPipeline(
    override val defaultPerformanceManager: PerformanceManager,
//    private val kotlinSourceRoots: List<String>
) : AbstractCliPipeline<K2JVMCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JVMCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmFir2IrPipelineArtifact> {
        return createRegularPipeline()
//        return when {
//            arguments.scriptingModeEnabled -> createScriptPipeline()
//            else -> createRegularPipeline()
//        }
    }

    private fun createRegularPipeline(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmFir2IrPipelineArtifact> =
        JvmConfigurationPipelinePhase then JvmFrontendPipelinePhase then JvmFir2IrPipelinePhase

//    private fun createScriptPipeline(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmScriptPipelineArtifact> =
//        JvmConfigurationPipelinePhase then
//                JvmScriptPipelinePhase

//    private val K2JVMCompilerArguments.scriptingModeEnabled: Boolean
//        get() = buildFile == null &&
//                !version &&
//                !allowNoSourceFiles &&
//                (script || expression != null || repl || freeArgs.isEmpty())

    override fun isKaptMode(arguments: K2JVMCompilerArguments): Boolean {
        return arguments.pluginOptions?.any { it.startsWith("plugin:org.jetbrains.kotlin.kapt3") } == true
    }

    override fun createPerformanceManager(
        arguments: K2JVMCompilerArguments,
        services: Services,
    ): PerformanceManager {
        val externalManager = services[PerformanceManager::class.java]
        if (externalManager != null) return externalManager
        val argument = arguments.profileCompilerCommand ?: return defaultPerformanceManager
        return ProfilingCompilerPerformanceManager.create(argument)
    }

    fun executeWithOutputArtifcat(
        arguments: K2JVMCompilerArguments,
        services: Services,
        originalMessageCollector: MessageCollector,
        disposable: Disposable
    ): JvmFir2IrPipelineArtifact? {
        val canceledStatus = services[CompilationCanceledStatus::class.java]
        ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus)
//        val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
        setIdeaIoUseFallback() // TODO (KT-73573): probably could be removed
        val performanceManager = createPerformanceManager(arguments, services).apply { compilerType = CompilerType.K2 }
        if (arguments.reportPerf || arguments.dumpPerf != null) {
            performanceManager.enableExtendedStats()
        }

        val messageCollector = GroupingMessageCollector(
            originalMessageCollector,
            arguments.allWarningsAsErrors,
            arguments.reportAllWarnings
        )
        val argumentsInput = ArgumentsPipelineArtifact(
            arguments,
            services,
            disposable,
            messageCollector,
            performanceManager
        )

        fun reportException(e: Throwable): JvmFir2IrPipelineArtifact? {
            MessageCollectorUtil.reportException(
                messageCollector,
                e
            ) // TODO (KT-73575): investigate reporting in case of OOM
            return null
        }

        fun reportCompilationCanceled(e: CompilationCanceledException): JvmFir2IrPipelineArtifact? {
            messageCollector.reportCompilationCancelled(e)
            return null
        }

        return try {
            val code = runPhasedPipeline(argumentsInput)
            performanceManager.notifyCompilationFinished()
            if (arguments.reportPerf) {
                messageCollector.report(CompilerMessageSeverity.LOGGING, "PERF: " + performanceManager.getTargetInfo())
                performanceManager.unitStats.forEachStringMeasurement {
                    messageCollector.report(CompilerMessageSeverity.LOGGING, "PERF: $it", null)
                }
            }

            if (arguments.dumpPerf != null) {
                performanceManager.dumpPerformanceReport(arguments.dumpPerf!!)
            }
            code
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } catch (_: CompilationErrorException) {
            null
        } catch (e: RuntimeException) {
            when (val cause = e.cause) {
                is CompilationCanceledException -> reportCompilationCanceled(cause)
                else -> reportException(e)
            }
        } catch (t: Throwable) {
            reportException(t)
        } finally {
            messageCollector.flush()
//            disposeRootInWriteAction(disposable)
        }
    }

    private fun runPhasedPipeline(input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>): JvmFir2IrPipelineArtifact? {
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
            val result = compoundPhase.invokeToplevel(
                phaseConfig,
                context,
                input
            )
            return result
        } catch (e: PipelineStepException) {
            e.printStackTrace()
            return null
        } catch (_: SuccessfulPipelineExecutionException) {
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            CheckCompilationErrors.CheckDiagnosticCollector.reportDiagnosticsToMessageCollector(context)
        }
    }
}
//
//class JvmConfigurationPipelinePhase(kotlinSourceRoots: List<String>) : AbstractConfigurationPhase<K2JVMCompilerArguments>(
//    name = "JvmConfigurationPipelinePhase",
//    postActions = setOf(CheckCompilationErrors.CheckMessageCollector),
//    configurationUpdaters = listOf(JvmConfigurationUpdaterWithKotlinSources(kotlinSourceRoots))
//) {
//    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
//        return MetadataVersion(*versionArray)
//    }
//
//    override fun provideCustomScriptingPluginOptions(arguments: K2JVMCompilerArguments): List<String> {
//        return buildList {
//            if (arguments.scriptTemplates?.isNotEmpty() == true) {
//                add("plugin:kotlin.scripting:script-templates=${arguments.scriptTemplates!!.joinToString(",")}")
//            }
//            if (arguments.scriptResolverEnvironment?.isNotEmpty() == true) {
//                add("plugin:kotlin.scripting:script-resolver-environment=${arguments.scriptResolverEnvironment!!.joinToString(",")}")
//            }
//        }
//    }
//}
//
//
//class JvmConfigurationUpdaterWithKotlinSources(private val kotlinSourceRoots: List<String>) : ConfigurationUpdater<K2JVMCompilerArguments>() {
//
//    override fun fillConfiguration(
//        input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>,
//        configuration: CompilerConfiguration
//    ) {
//        JvmConfigurationUpdater.fillConfiguration(input,configuration)
////        configuration.addKotlinSourceRoots(kotlinSourceRoots)
//    }
//
//}