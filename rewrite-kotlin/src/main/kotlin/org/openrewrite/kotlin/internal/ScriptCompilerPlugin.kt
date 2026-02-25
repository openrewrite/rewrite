/*
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
package org.openrewrite.kotlin.internal

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirImportBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptReceiverParameterBuilder
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.FirScriptResolutionConfigurationExtension
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.builder.FirResolvedTypeRefBuilder
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * A Kotlin compiler plugin that configures script files with implicit receivers
 * and default imports. Used by GradleParser to provide type information for
 * build.gradle.kts and settings.gradle.kts files.
 *
 * @param implicitReceivers FQNs of types to add as implicit receivers (e.g., "org.gradle.api.Project")
 * @param defaultImports package names to add as star imports (e.g., "org.gradle.api")
 */
@OptIn(ExperimentalCompilerApi::class)
class ScriptCompilerPlugin(
    private val implicitReceivers: List<String>,
    private val defaultImports: List<String>
) : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            ScriptExtensionRegistrar(implicitReceivers, defaultImports)
        )
    }
}

private class ScriptExtensionRegistrar(
    private val implicitReceivers: List<String>,
    private val defaultImports: List<String>
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +ScriptReceiverExtension.factory(implicitReceivers)
        +ScriptImportsExtension.factory(defaultImports)
    }
}

private class ScriptReceiverExtension(
    session: FirSession,
    private val implicitReceivers: List<String>
) : FirScriptConfiguratorExtension(session) {

    companion object {
        fun factory(implicitReceivers: List<String>): Factory {
            return Factory { session -> ScriptReceiverExtension(session, implicitReceivers) }
        }
    }

    override fun accepts(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean = true

    override fun FirScriptBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
        // No file-level configuration needed
    }

    override fun FirScriptBuilder.configure(sourceFile: KtSourceFile?, context: Context<PsiElement>) {
        for (fqn in implicitReceivers) {
            val classId = ClassId.topLevel(FqName(fqn))
            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            val coneType = ConeClassLikeTypeImpl(
                lookupTag,
                emptyArray(),
                false,
                ConeAttributes.Empty
            )

            val receiverParameter = FirScriptReceiverParameterBuilder().apply {
                this.moduleData = this@configure.moduleData
                this.origin = FirDeclarationOrigin.ScriptCustomization.Default
                this.symbol = FirReceiverParameterSymbol()
                this.containingDeclarationSymbol = this@configure.symbol
                this.typeRef = FirResolvedTypeRefBuilder().apply {
                    this.coneType = coneType
                }.build()
                this.isBaseClassReceiver = true
            }.build()

            receivers.add(receiverParameter)
        }
    }
}

private class ScriptImportsExtension(
    session: FirSession,
    private val defaultImports: List<String>
) : FirScriptResolutionConfigurationExtension(session) {

    companion object {
        fun factory(defaultImports: List<String>): Factory {
            return Factory { session -> ScriptImportsExtension(session, defaultImports) }
        }
    }

    private val imports: List<FirImport> by lazy {
        defaultImports.map { pkg ->
            FirImportBuilder().apply {
                importedFqName = FqName(pkg)
                isAllUnder = true
            }.build()
        }
    }

    override fun getScriptDefaultImports(script: FirScript): List<FirImport> = imports
}
