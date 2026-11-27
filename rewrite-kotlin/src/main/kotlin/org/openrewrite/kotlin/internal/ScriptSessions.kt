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
package org.openrewrite.kotlin.internal

import org.jetbrains.kotlin.cli.common.SessionWithSources
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.KmpModuleKind
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile

/**
 * Creates the FIR sessions of one compilation like `JvmFrontendPipelinePhase.prepareJvmSessions`, with a source session
 * per script. A script sees the classpath and the non-script sources but not the declarations of other scripts, as when
 * it is compiled alone. All sessions share one library session, so the classpath is loaded once per compilation.
 *
 * The non-script session comes first because the scripts depend on it and must be resolved after it.
 */
fun prepareSessionsWithIsolatedScripts(
    files: List<KtFile>,
    rootModuleName: Name,
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    librariesScope: AbstractProjectFileSearchScope,
    libraryList: DependencyListForCliModule,
): List<SessionWithSources<KtFile>> {
    val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
    val javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources()
    val context = FirJvmSessionFactory.Context(configuration, projectEnvironment, librariesScope)
    val platform = JvmPlatforms.unspecifiedJvmPlatform

    val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
        rootModuleName, extensionRegistrars, configuration.languageVersionSettings, context
    )
    FirJvmSessionFactory.createLibrarySession(
        sharedLibrarySession, libraryList.moduleDataProvider, extensionRegistrars,
        configuration.languageVersionSettings, context
    )

    fun createSourceSession(moduleData: FirModuleData, registerJavaElementFinder: Boolean): FirSession =
        FirJvmSessionFactory.createSourceSession(
            moduleData,
            javaSourcesScope,
            createIncrementalCompilationSymbolProviders = { null },
            extensionRegistrars,
            configuration,
            context,
            needRegisterJavaElementFinder = registerJavaElementFinder,
            kmpModuleKind = KmpModuleKind.SingleModule,
        ) {
            registerComponent(
                FirBuiltinSyntheticFunctionInterfaceProvider::class,
                sharedLibrarySession.syntheticFunctionInterfacesSymbolProvider
            )
        }

    val (scripts, nonScripts) = files.partition { it.isScript() }
    val nonScriptModuleData = FirSourceModuleData(
        rootModuleName,
        libraryList.regularDependencies,
        emptyList(),
        libraryList.friendDependencies,
        platform,
    )
    val sessions = mutableListOf(SessionWithSources(createSourceSession(nonScriptModuleData, true), nonScripts))
    scripts.forEachIndexed { i, script ->
        val scriptModuleData = FirSourceModuleData(
            Name.identifier("${rootModuleName.asString()}-script-$i"),
            libraryList.regularDependencies,
            listOf(nonScriptModuleData),
            libraryList.friendDependencies,
            platform,
        )
        // Registering an element finder unregisters the previous one, and Java sources only need the non-script one.
        sessions += SessionWithSources(createSourceSession(scriptModuleData, false), listOf(script))
    }
    return sessions
}
