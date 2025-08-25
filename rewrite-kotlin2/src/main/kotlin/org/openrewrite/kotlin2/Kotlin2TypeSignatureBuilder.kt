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
package org.openrewrite.kotlin2

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaTypeParameter
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.Variance
import org.openrewrite.java.JavaTypeSignatureBuilder
import org.openrewrite.java.tree.JavaType
import java.util.*
import kotlin.collections.HashMap

/**
 * Type signature builder for Kotlin 2 using the K2 compiler's FIR representation.
 * This class generates unique signatures for FIR types to enable caching and comparison.
 */
@Suppress("DuplicatedCode")
class Kotlin2TypeSignatureBuilder(private val firSession: FirSession, private val firFile: FirFile) :
    JavaTypeSignatureBuilder {
    private var typeVariableNameStack: MutableSet<String>? = null

    override fun signature(type: Any?): String {
        return signature(type, firFile)
    }

    override fun classSignature(type: Any): String {
        return when (type) {
            is ConeClassLikeType -> classSignature(type)
            is FirClass -> classSignature(type)
            else -> "{undefined}"
        }
    }

    override fun parameterizedSignature(type: Any): String {
        return when (type) {
            is ConeClassLikeType -> parameterizedSignature(type)
            is FirClass -> parameterizedSignature(type)
            else -> "{undefined}"
        }
    }

    override fun arraySignature(type: Any): String {
        // TODO: Implement array signature for Kotlin arrays
        return "{array}"
    }

    override fun genericSignature(type: Any): String {
        return when (type) {
            is FirTypeParameter -> typeVariableSignature(type)
            else -> "{generic}"
        }
    }

    override fun primitiveSignature(type: Any): String {
        // Handle Kotlin primitive types
        return when (type) {
            is ConeClassLikeType -> {
                val classId = type.classId
                when (classId?.asFqNameString()) {
                    "kotlin.Boolean" -> "boolean"
                    "kotlin.Byte" -> "byte"
                    "kotlin.Char" -> "char"
                    "kotlin.Short" -> "short"
                    "kotlin.Int" -> "int"
                    "kotlin.Long" -> "long"
                    "kotlin.Float" -> "float"
                    "kotlin.Double" -> "double"
                    "kotlin.Unit" -> "void"
                    else -> "{primitive}"
                }
            }
            else -> "{primitive}"
        }
    }

    @OptIn(SymbolInternals::class)
    fun signature(type: Any?, parent: Any?): String {
        return when (type) {
            is ConeClassLikeType -> {
                if (type.typeArguments.isNotEmpty()) parameterizedSignature(type) else classSignature(type)
            }

            is ConeFlexibleType -> {
                signature(type.lowerBound)
            }

            is ConeStubTypeForChainInference -> {
                signature(type.constructor.variable)
            }

            is ConeTypeProjection -> {
                coneTypeProjectionSignature(type)
            }

            is ConeTypeParameterBasedTypeVariable -> {
                signature(type.typeParameterSymbol.fir)
            }

            is ConeDefinitelyNotNullType -> {
                // K2 specific: definitely non-nullable types
                "!" + signature(type.original)
            }

            is ConeIntersectionType -> {
                // K2 specific: intersection types
                type.intersectedTypes.joinToString("&") { signature(it) }
            }

            is FirAnonymousFunctionExpression -> {
                signature(type.anonymousFunction)
            }

            is FirBlock -> {
                "{undefined}"
            }

            is FirAnonymousObject -> {
                if (type.typeParameters.isNotEmpty()) anonymousParameterizedSignature(type) else anonymousClassSignature(type)
            }

            is FirClass -> {
                if (type.typeParameters.isNotEmpty()) parameterizedSignature(type) else classSignature(type)
            }

            is FirConstructor -> {
                constructorSignature(type)
            }

            is FirEnumEntry -> {
                signature(type.returnTypeRef, parent)
            }

            is FirErrorNamedReference -> {
                "{undefined}"
            }

            is FirExpression -> {
                // Use resolvedType for FIR expressions
                try {
                    signature(type.resolvedType, parent)
                } catch (e: Exception) {
                    "{undefined}"
                }
            }

            is FirFile -> {
                convertClassIdToFqn(type.packageFqName.asString())
            }

            is FirFunction -> {
                methodSignature(type, parent)
            }

            is FirImplicitNullableAnyTypeRef -> {
                "java.lang.Object"
            }

            is FirJavaTypeRef -> {
                // TODO: Implement Java type signature generation
                "{java-type}"
            }

            is FirProperty -> {
                variableSignature(type, parent)
            }

            is FirResolvedNamedReference -> {
                signature(type.toResolvedBaseSymbol()?.fir, parent)
            }

            is FirResolvedQualifier -> {
                signature(type.symbol?.fir, parent)
            }

            is FirResolvedTypeRef -> {
                signature(type.type, parent)
            }

            is FirSuperReference -> {
                "{super}"
            }

            is FirTypeAlias -> {
                signature(type.expandedTypeRef, parent)
            }

            is FirTypeParameter -> {
                typeVariableSignature(type)
            }

            is FirTypeProjectionWithVariance -> {
                signature(type.typeRef, parent)
            }

            is FirTypeRef -> {
                "{undefined}"
            }

            is FirVariable -> {
                variableSignature(type, parent)
            }

            else -> {
                "{undefined}"
            }
        }
    }

    private fun classSignature(type: ConeClassLikeType): String {
        return convertClassIdToFqn(type.classId!!)
    }

    private fun classSignature(type: FirClass): String {
        return convertClassIdToFqn(type.classId)
    }

    private fun parameterizedSignature(type: ConeClassLikeType): String {
        val baseType = classSignature(type)
        val params = type.typeArguments.joinToString(",", "<", ">") { 
            when (it) {
                is ConeStarProjection -> "*"
                is ConeKotlinTypeProjection -> signature(it.type)
                else -> "{undefined}"
            }
        }
        return baseType + params
    }

    private fun parameterizedSignature(type: FirClass): String {
        val baseType = classSignature(type)
        val params = type.typeParameters.joinToString(",", "<", ">") {
            signature(it)
        }
        return baseType + params
    }

    private fun anonymousClassSignature(type: FirAnonymousObject): String {
        return "Anonymous{" + type.symbol.toString() + "}"
    }

    private fun anonymousParameterizedSignature(type: FirAnonymousObject): String {
        val baseType = anonymousClassSignature(type)
        val params = type.typeParameters.joinToString(",", "<", ">") {
            signature(it)
        }
        return baseType + params
    }

    private fun coneTypeProjectionSignature(type: ConeTypeProjection): String {
        return when (type) {
            is ConeStarProjection -> "*"
            is ConeKotlinTypeProjection -> {
                val variance = when (type.kind) {
                    ProjectionKind.IN -> "in "
                    ProjectionKind.OUT -> "out "
                    ProjectionKind.INVARIANT -> ""
                    ProjectionKind.STAR -> "*"
                }
                variance + signature(type.type)
            }
        }
    }

    private fun constructorSignature(constructor: FirConstructor): String {
        val declaringType = constructor.symbol.containingClassLookupTag()?.classId
        val className = if (declaringType != null) convertClassIdToFqn(declaringType) else "{undefined}"
        val params = constructor.valueParameters.joinToString(",", "(", ")") {
            signature(it.returnTypeRef)
        }
        return "<constructor>$className$params"
    }

    private fun methodSignature(function: FirFunction, parent: Any?): String {
        val name = methodName(function)
        val declaringType = when (parent) {
            is FirClass -> convertClassIdToFqn(parent.classId)
            is FirFile -> convertClassIdToFqn(parent.packageFqName.asString())
            else -> "{undefined}"
        }
        val params = function.valueParameters.joinToString(",", "(", ")") {
            signature(it.returnTypeRef)
        }
        return "$declaringType.$name$params"
    }

    private fun variableSignature(variable: FirVariable, parent: Any?): String {
        val name = variableName(variable)
        val declaringType = when (parent) {
            is FirClass -> convertClassIdToFqn(parent.classId)
            is FirFile -> convertClassIdToFqn(parent.packageFqName.asString())
            else -> "{undefined}"
        }
        return "$declaringType.$name"
    }

    private fun typeVariableSignature(typeParameter: FirTypeParameter): String {
        val name = typeParameter.name.asString()
        if (typeVariableNameStack?.contains(name) == true) {
            return "Generic{$name}"
        }

        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }
        typeVariableNameStack!!.add(name)

        val bounds = typeParameter.bounds.joinToString("&") {
            signature(it)
        }

        typeVariableNameStack!!.remove(name)

        return if (bounds.isNotEmpty()) {
            "Generic{$name extends $bounds}"
        } else {
            "Generic{$name}"
        }
    }


    companion object {
        fun convertClassIdToFqn(classId: ClassId): String {
            return classId.asFqNameString().replace('/', '.')
        }

        fun convertClassIdToFqn(packageName: String): String {
            return packageName.replace('/', '.')
        }

        fun methodName(function: FirFunction): String {
            return when (function) {
                is FirConstructor -> "<init>"
                is FirSimpleFunction -> function.name.asString()
                is FirAnonymousFunction -> "<anonymous>"
                else -> "{undefined}"
            }
        }

        fun variableName(variable: FirVariable): String {
            return variable.name.asString()
        }
    }
}