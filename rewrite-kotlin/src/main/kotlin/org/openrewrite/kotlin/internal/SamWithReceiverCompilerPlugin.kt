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

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.resolve.FirSamConversionTransformerExtension
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.specialFunctionTypeKind

/**
 * The Kotlin compiler's sam-with-receiver plugin, for the SAM interfaces annotated with one of [annotations].
 */
@OptIn(ExperimentalCompilerApi::class)
class SamWithReceiverCompilerPlugin(private val annotations: List<String>) : CompilerPluginRegistrar() {

    override val pluginId: String = "org.openrewrite.kotlin.SamWithReceiverCompilerPlugin"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(object : FirExtensionRegistrar() {
            override fun ExtensionRegistrarContext.configurePlugin() {
                +FirSamConversionTransformerExtension.Factory { session -> SamWithReceiverTransformer(session, annotations) }
            }
        })
    }
}

private class SamWithReceiverTransformer(
    session: FirSession,
    private val annotations: List<String>
) : FirSamConversionTransformerExtension(session) {

    override fun getCustomFunctionTypeForSamConversion(function: FirNamedFunction): ConeLookupTagBasedType? {
        val samInterface = function.containingClassLookupTag()?.toRegularClassSymbol(session) ?: return null
        if (samInterface.resolvedAnnotationClassIds.none { it.asSingleFqName().asString() in annotations }) {
            return null
        }
        val parameterTypes = function.valueParameters.map { it.returnTypeRef.coneType }
        if (parameterTypes.isEmpty()) {
            return null
        }
        return createFunctionType(
            function.specialFunctionTypeKind(session) ?: FunctionTypeKind.Function,
            parameterTypes.subList(1, parameterTypes.size),
            parameterTypes[0],
            function.returnTypeRef.coneType,
            emptyList()
        )
    }
}
