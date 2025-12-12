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

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.classId
import org.jetbrains.kotlin.codegen.topLevelClassAsmType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.*
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.openrewrite.java.JavaTypeMapping
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.*
import org.openrewrite.kotlin2.Kotlin2TypeSignatureBuilder.Companion.convertClassIdToFqn
import org.openrewrite.kotlin2.Kotlin2TypeSignatureBuilder.Companion.methodName
import org.openrewrite.kotlin2.Kotlin2TypeSignatureBuilder.Companion.variableName
import kotlin.collections.ArrayList

/**
 * Type mapping for Kotlin 2 using the K2 compiler's FIR representation.
 * This class maps FIR types to OpenRewrite's JavaType system, leveraging
 * the enhanced type information available in the K2 compiler.
 */
@Suppress("DuplicatedCode")
class Kotlin2TypeMapping(
    private val typeCache: JavaTypeCache,
    val firSession: FirSession,
    private val firFile: FirFile
) : JavaTypeMapping<Any> {

    private val signatureBuilder: Kotlin2TypeSignatureBuilder = Kotlin2TypeSignatureBuilder(firSession, firFile)

    override fun type(type: Any?): JavaType {
        if (type == null || type is FirErrorTypeRef || (type is FirResolvedQualifier && type.classId == null)) {
            return Unknown.getInstance()
        }

        val signature = signatureBuilder.signature(type)
        val existing: JavaType? = typeCache.get(signature)
        if (existing != null) {
            return existing
        }

        return type(type, firFile, signature) ?: Unknown.getInstance()
    }

    fun type(type: Any?, parent: Any?): JavaType? {
        if (type == null || type is FirErrorTypeRef || (type is FirResolvedQualifier && type.classId == null)) {
            return Unknown.getInstance()
        }
        val signature = signatureBuilder.signature(type, parent)
        val existing = typeCache.get<JavaType>(signature)
        if (existing != null) {
            return existing
        }
        return type(type, parent, signature)
    }

    private fun type(type: Any, parent: Any?, signature: String): JavaType? {
        // Implementation for K2 compiler FIR type mapping
        // This will handle the new FIR types and enhanced type information
        // available in Kotlin 2.0
        
        return when (type) {
            is FirTypeRef -> mapFirTypeRef(type, signature)
            is FirClass -> mapFirClass(type, signature)
            is FirFunction -> mapFirFunction(type, signature)
            is FirProperty -> mapFirProperty(type, signature)
            is FirExpression -> mapFirExpression(type, signature)
            is FirResolvedQualifier -> mapFirResolvedQualifier(type, signature)
            else -> null
        }
    }

    private fun mapFirTypeRef(typeRef: FirTypeRef, signature: String): JavaType? {
        // Map FIR type references to JavaType
        return when (typeRef) {
            is FirResolvedTypeRef -> mapConeType(typeRef.type, signature)
            is FirImplicitNullableAnyTypeRef -> typeCache.get<JavaType>("java.lang.Object")
            is FirJavaTypeRef -> mapJavaTypeRef(typeRef, signature)
            else -> null
        }
    }

    private fun mapConeType(coneType: ConeKotlinType, signature: String): JavaType? {
        // Map Cone types (K2's internal type representation) to JavaType
        return when (coneType) {
            is ConeClassLikeType -> mapClassLikeType(coneType, signature)
            is ConeTypeParameterType -> mapTypeParameter(coneType, signature)
            is ConeFlexibleType -> mapFlexibleType(coneType, signature)
            is ConeIntersectionType -> mapIntersectionType(coneType, signature)
            is ConeDefinitelyNotNullType -> mapDefinitelyNotNullType(coneType, signature)
            else -> null
        }
    }

    private fun mapClassLikeType(type: ConeClassLikeType, signature: String): JavaType? {
        // Map class-like types to JavaType.Class
        val classId = type.classId ?: return null
        val fqn = convertClassIdToFqn(classId)
        
        // Check cache first
        val cached = typeCache.get<JavaType>(signature)
        if (cached != null) {
            return cached
        }

        // Create new Class type
        val classType = Class(
            null,
            0L, // flagsBitMap
            fqn,
            JavaType.FullyQualified.Kind.Class,
            emptyList(),
            null,
            null,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )

        typeCache.put(signature, classType)
        return classType
    }

    private fun mapTypeParameter(type: ConeTypeParameterType, signature: String): JavaType? {
        // Map type parameters to JavaType.GenericTypeVariable
        val name = type.lookupTag.name.asString()
        return GenericTypeVariable(null, name, GenericTypeVariable.Variance.INVARIANT, emptyList())
    }

    private fun mapFlexibleType(type: ConeFlexibleType, signature: String): JavaType? {
        // K2 compiler uses flexible types for platform types
        // Map to the upper bound for safety
        return mapConeType(type.upperBound, signature)
    }

    private fun mapIntersectionType(type: ConeIntersectionType, signature: String): JavaType? {
        // Map intersection types
        val types = type.intersectedTypes.mapNotNull { mapConeType(it, "${signature}_${it}") }
        return if (types.isNotEmpty()) types.first() else null
    }

    private fun mapDefinitelyNotNullType(type: ConeDefinitelyNotNullType, signature: String): JavaType? {
        // K2 specific: definitely non-nullable types
        // Map to the original type but mark as non-nullable
        return mapConeType(type.original, signature)
    }

    private fun mapJavaTypeRef(typeRef: FirJavaTypeRef, signature: String): JavaType? {
        // Map Java type references
        return null // TODO: Implement Java type mapping
    }

    private fun mapFirClass(firClass: FirClass, signature: String): JavaType? {
        // Map FIR class declarations to JavaType.Class
        val classId = firClass.symbol.classId
        val fqn = convertClassIdToFqn(classId)
        
        return Class(
            null,
            0L, // flagsBitMap
            fqn,
            when (firClass.classKind) {
                ClassKind.CLASS -> JavaType.FullyQualified.Kind.Class
                ClassKind.INTERFACE -> JavaType.FullyQualified.Kind.Interface
                ClassKind.ENUM_CLASS -> JavaType.FullyQualified.Kind.Enum
                ClassKind.ANNOTATION_CLASS -> JavaType.FullyQualified.Kind.Annotation
                else -> JavaType.FullyQualified.Kind.Class
            },
            emptyList(),
            null,
            null,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )
    }

    private fun mapFirFunction(function: FirFunction, signature: String): JavaType? {
        // Map FIR functions to JavaType.Method
        return null // TODO: Implement function mapping
    }

    private fun mapFirProperty(property: FirProperty, signature: String): JavaType? {
        // Map FIR properties to JavaType.Variable
        return null // TODO: Implement property mapping
    }

    private fun mapFirExpression(expression: FirExpression, signature: String): JavaType? {
        // Map FIR expressions based on their resolved type
        // Using resolvedType which is the proper API for resolved expressions
        return try {
            val coneType = expression.resolvedType
            mapConeType(coneType, signature)
        } catch (e: Exception) {
            // Expression type not yet resolved
            null
        }
    }

    private fun mapFirResolvedQualifier(qualifier: FirResolvedQualifier, signature: String): JavaType? {
        // Map resolved qualifiers to their class types
        val classId = qualifier.classId ?: return null
        val fqn = convertClassIdToFqn(classId)
        
        return Class(
            null,
            0L, // flagsBitMap
            fqn,
            JavaType.FullyQualified.Kind.Class,
            emptyList(),
            null,
            null,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )
    }
}