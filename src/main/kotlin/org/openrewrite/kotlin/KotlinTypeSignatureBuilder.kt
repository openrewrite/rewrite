/*
 * Copyright 2023 the original author or authors.
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

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.openrewrite.internal.lang.Nullable
import org.openrewrite.java.JavaTypeSignatureBuilder
import java.util.*

class KotlinTypeSignatureBuilder(private val firSession: FirSession) : JavaTypeSignatureBuilder {
    private var typeVariableNameStack: MutableSet<String>? = null
    override fun signature(type: @Nullable Any?): String {
        return signature(type, null)
    }

    @OptIn(SymbolInternals::class)
    fun signature(type: @Nullable Any?, ownerSymbol: @Nullable FirBasedSymbol<*>?): String {
        if (type == null) {
            return "{undefined}"
        }

        if (type is String) {
            return type
        }

        when (type) {
            is FirClass -> {
                return if (type.typeParameters.isNotEmpty()) {
                    parameterizedSignature(type)
                } else {
                    classSignature(type)
                }
            }

            is FirFunction -> {
                return methodDeclarationSignature(type.symbol, ownerSymbol)
            }

            is FirVariable -> {
                return variableSignature(type.symbol, ownerSymbol)
            }

            is FirBasedSymbol<*> -> {
                return signature(type.fir, ownerSymbol)
            }

            is FirFile -> {
                return convertFileNameToFqn(type.name)
            }

            is FirJavaTypeRef -> {
                return signature(type.type)
            }

            is JavaType -> {
                return mapJavaTypeSignature(type)
            }

            is JavaElement -> {
                return mapJavaElementSignature(type)
            }

            else -> return resolveSignature(type, ownerSymbol)
        }
    }

    /**
     * Interpret various parts of the Kotlin tree for type attribution.
     * This method should only be called by signature.
     */
    @OptIn(SymbolInternals::class)
    private fun resolveSignature(type: Any, ownerSymbol: @Nullable FirBasedSymbol<*>?): String {
        when (type) {
            is ConeTypeProjection -> {
                return coneTypeProjectionSignature(type)
            }

            is FirResolvedQualifier -> {
                return signature(type.symbol)
            }

            is FirExpression -> {
                return signature(type.typeRef, ownerSymbol)
            }

            is FirFunctionTypeRef -> {
                return signature(type.returnTypeRef, ownerSymbol)
            }

            is FirResolvedNamedReference -> {
                when (val resolvedSymbol = type.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        return signature(resolvedSymbol.resolvedReturnTypeRef, ownerSymbol)
                    }

                    is FirEnumEntrySymbol -> {
                        return signature(resolvedSymbol.resolvedReturnTypeRef, ownerSymbol)
                    }

                    is FirNamedFunctionSymbol -> {
                        return signature(resolvedSymbol.resolvedReturnTypeRef, ownerSymbol)
                    }

                    is FirPropertySymbol -> {
                        return signature(resolvedSymbol.resolvedReturnTypeRef, ownerSymbol)
                    }

                    is FirValueParameterSymbol -> {
                        return signature(resolvedSymbol.resolvedReturnType, ownerSymbol)
                    }

                    is FirFieldSymbol -> {
                        return signature(resolvedSymbol.resolvedReturnType, ownerSymbol)
                    }
                }
            }

            is FirResolvedTypeRef -> {
                val coneKotlinType = type.type
                if (coneKotlinType is ConeTypeParameterType) {
                    val classifierSymbol = coneKotlinType.lookupTag.toSymbol(
                        firSession
                    )
                    if (classifierSymbol != null && classifierSymbol.fir is FirTypeParameter) {
                        return genericSignature(classifierSymbol.fir)
                    }
                } else if (coneKotlinType is ConeFlexibleType) {
                    return if (coneKotlinType.lowerBound.typeArguments.isEmpty()) {
                        typeRefClassSignature(coneKotlinType.lowerBound)
                    } else {
                        parameterizedTypeRef(coneKotlinType.lowerBound)
                    }
                }
                return if (coneKotlinType.typeArguments.isNotEmpty()) {
                    parameterizedTypeRef(coneKotlinType)
                } else {
                    typeRefClassSignature(coneKotlinType)
                }
            }

            is FirTypeParameter -> {
                return genericSignature(type)
            }

            is FirValueParameterSymbol -> {
                return signature(type.resolvedReturnType, ownerSymbol)
            }

            is FirVariableAssignment -> {
                return signature(type.lValue, ownerSymbol)
            }

            is FirOuterClassTypeParameterRef -> {
                return signature(type.symbol)
            }
        }
        return "{undefined}"
    }

    /**
     * Kotlin does not support dimensioned arrays.
     */
    override fun arraySignature(type: Any): String {
        throw UnsupportedOperationException("This should never happen.")
    }

    /**
     * Build a class signature for a FirClass.
     */
    @OptIn(SymbolInternals::class)
    override fun classSignature(type: Any): String {
        var resolveType: FirClass? = null
        if (type is FirClass) {
            resolveType = type
        } else if (type is FirFunction) {
            resolveType = if (type is FirConstructor) {
                convertToRegularClass((type.returnTypeRef as FirResolvedTypeRef).type)
            } else {
                convertToRegularClass(if (type.dispatchReceiverType != null) {
                    type.dispatchReceiverType
                } else {
                    (type.returnTypeRef as FirResolvedTypeRef).type
                })
            }
        } else if (type is FirResolvedTypeRef) {
            val symbol = type.type.toRegularClassSymbol(
                firSession
            )
            if (symbol != null) {
                resolveType = symbol.fir
            }
        } else if (type is ConeClassLikeType) {
            val symbol = type.toRegularClassSymbol(
                firSession
            )
            if (symbol != null) {
                resolveType = symbol.fir
            }
        } else if (type is ConeClassLikeLookupTag) {
            val symbol = type.toFirRegularClassSymbol(
                firSession
            )
            if (symbol != null) {
                resolveType = symbol.fir
            }
        } else if (type is FirFile) {
            return type.name
        }
        if (resolveType == null) {
            return "{undefined}"
        }
        val symbol = resolveType.symbol
        return convertClassIdToFqn(symbol.classId)
    }

    /**
     * Build a class signature for a parameterized FirClass.
     */
    override fun parameterizedSignature(type: Any): String {
        val s = StringBuilder(classSignature(type))
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in (type as FirClass).typeParameters) {
            val signature = signature(tp, type.symbol)
            joiner.add(signature)
        }
        s.append(joiner)
        return s.toString()
    }

    /**
     * Convert the ConeKotlinType to a [org.openrewrite.java.tree.JavaType] style FQN.
     */
    private fun typeRefClassSignature(type: ConeKotlinType): String {
        val classId: ClassId? = if (type is ConeFlexibleType) {
            type.lowerBound.classId
        } else {
            type.classId
        }
        return if (classId == null) {
            "{undefined}"
        } else {
            convertClassIdToFqn(classId)
        }
    }

    /**
     * Convert the ConeKotlinType to a [org.openrewrite.java.tree.JavaType] style FQN.
     */
    private fun parameterizedTypeRef(type: ConeKotlinType): String {
        val classId = type.classId
        val fq = if (classId == null) {
            "{undefined}"
        } else {
            convertClassIdToFqn(classId)
        }
        val s = StringBuilder(fq)
        val joiner = StringJoiner(", ", "<", ">")
        for (argument in type.typeArguments) {
            val signature = coneTypeProjectionSignature(argument)
            joiner.add(signature)
        }
        s.append(joiner)
        return s.toString()
    }

    /**
     * Generate a generic type signature from a FirElement.
     */
    override fun genericSignature(type: Any): String {
        val typeParameter = type as FirTypeParameter
        val name = typeParameter.name.asString()
        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }
        if (!typeVariableNameStack!!.add(name)) {
            return "Generic{$name}"
        }
        val s = StringBuilder("Generic{").append(name)
        val boundSigs = StringJoiner(" & ")
        for (bound in typeParameter.bounds) {
            if (bound !is FirImplicitNullableAnyTypeRef) {
                boundSigs.add(signature(bound))
            }
        }
        val boundSigStr = boundSigs.toString()
        if (!boundSigStr.isEmpty()) {
            s.append(" extends ").append(boundSigStr)
        }
        typeVariableNameStack!!.remove(name)
        return s.append("}").toString()
    }

    /**
     * Generate a ConeTypeProject signature.
     */
    private fun coneTypeProjectionSignature(type: ConeTypeProjection): String {
        val s = StringBuilder()
        if (type is ConeKotlinTypeProjectionIn) {
            val (type1) = type
            s.append("Generic{in ")
            s.append(signature(type1))
            s.append("}")
        } else if (type is ConeCapturedType) {
            val (type1) = type
            s.append("Generic{in ")
            s.append(signature(type1))
            s.append("}")
        } else if (type is ConeKotlinTypeProjectionOut) {
            val (type1) = type
            s.append("Generic{out ")
            s.append(signature(type1))
            s.append("}")
        } else if (type is ConeStarProjection) {
            s.append("Generic{*}")
        } else if (type is ConeClassLikeType) {
            s.append(convertClassIdToFqn(type.lookupTag.classId))
            if (type.typeArguments.isNotEmpty()) {
                s.append("<")
                val typeArguments: Array<out ConeTypeProjection> = type.typeArguments
                for (i in typeArguments.indices) {
                    val typeArgument = typeArguments[i]
                    s.append(signature(typeArgument))
                    if (i < typeArguments.size - 1) {
                        s.append(", ")
                    }
                }
                s.append(">")
            }
        } else if (type is ConeTypeParameterType) {
            val symbol: FirClassifierSymbol<*>? = type.lookupTag.toSymbol(firSession)
            if (symbol != null) {
                s.append(signature(symbol))
            } else {
                s.append("Generic{")
                s.append(convertKotlinFqToJavaFq(type.toString()))
                s.append("}")
            }
        } else if (type is ConeFlexibleType) {
            s.append(signature(type.lowerBound))
        } else if (type is ConeDefinitelyNotNullType) {
            s.append("Generic{")
            s.append(type)
            s.append("}")
        } else if (type is ConeIntersectionType) {
            s.append("Generic{")
            val boundSigs = StringJoiner(" & ")
            for (coneKotlinType in type.intersectedTypes) {
                boundSigs.add(signature(coneKotlinType))
            }
            s.append(boundSigs)
            s.append("}")
        } else if (type is ConeCapturedType && type.lowerType == null) {
            s.append("*")
        } else {
            throw IllegalArgumentException("Unsupported ConeTypeProjection " + type.javaClass.getName())
        }
        return s.toString()
    }

    private fun mapJavaElementSignature(type: JavaElement): String {
        when (type) {
            is BinaryJavaClass -> {
                return if (type.typeParameters.isEmpty()) {
                    javaClassSignature(type as JavaClass)
                } else {
                    javaParameterizedSignature(type as JavaClass)
                }
            }

            is JavaTypeParameter -> {
                return mapJavaTypeParameter(type)
            }

            is JavaValueParameter -> {
                return mapJavaValueParameter(type)
            }

            is JavaAnnotation -> {
                return convertClassIdToFqn(type.classId)
            }
            // This should never happen unless a new JavaElement is added.
            else -> throw UnsupportedOperationException("Unsupported JavaElement type: " + type.javaClass.getName())
        }
    }

    private fun mapJavaTypeSignature(type: JavaType): String {
        when (type) {
            is JavaPrimitiveType -> {
                return primitive(type.type)
            }

            is JavaClassifierType -> {
                return mapClassifierType(type)
            }

            is JavaArrayType -> {
                return array(type)
            }

            is JavaWildcardType -> {
                return mapWildCardType(type)
            }
            // This should never happen unless a new JavaType is added.
            else -> throw UnsupportedOperationException("Unsupported kotlin structure JavaType: " + type.javaClass.getName())
        }
    }

    private fun mapClassifierType(type: JavaClassifierType): String {
        if (type.typeArguments.isEmpty()) {
            return type.classifierQualifiedName
        }
        val s = StringBuilder(type.classifierQualifiedName)
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeArguments) {
            val signature = signature(tp)
            joiner.add(signature)
        }
        s.append(joiner)
        return s.toString()
    }

    private fun mapWildCardType(type: JavaWildcardType): String {
        val s = StringBuilder("Generic{?")
        if (type.bound != null) {
            s.append(if (type.isExtends) " extends " else " super ")
            s.append(signature(type.bound))
        }
        return s.append("}").toString()
    }

    private fun array(type: JavaArrayType): String {
        return signature(type.componentType) + "[]"
    }

    private fun javaClassSignature(type: JavaClass?): String {
        if (type!!.fqName == null) {
            return "{undefined}"
        }
        return if (type.outerClass != null) {
            javaClassSignature(type.outerClass) + "$" + type.name
        } else type.fqName!!.asString()
    }

    private fun javaParameterizedSignature(type: JavaClass): String {
        val s = StringBuilder(javaClassSignature(type))
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeParameters) {
            val signature = signature(tp)
            joiner.add(signature)
        }
        s.append(joiner)
        return s.toString()
    }

    private fun mapJavaTypeParameter(typeParameter: JavaTypeParameter): String {
        val name = typeParameter.name.asString()
        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }
        if (!typeVariableNameStack!!.add(name)) {
            return "Generic{$name}"
        }
        val s = StringBuilder("Generic{").append(name)
        val boundSigs = StringJoiner(" & ")
        for (type in typeParameter.upperBounds) {
            if (type.classifier != null && "java.lang.Object" != type.classifierQualifiedName) {
                boundSigs.add(signature(type))
            }
        }
        val boundSigStr = boundSigs.toString()
        if (boundSigStr.isNotEmpty()) {
            s.append(": ").append(boundSigStr)
        }
        typeVariableNameStack!!.remove(name)
        return s.append("}").toString()
    }

    private fun mapJavaValueParameter(type: JavaValueParameter): String {
        return mapJavaTypeSignature(type.type)
    }

    private fun primitive(type: @Nullable PrimitiveType?): String {
        return if (type == null) {
            "void"
        } else when (type) {
            PrimitiveType.BOOLEAN -> "java.lang.boolean"
            PrimitiveType.BYTE -> "java.lang.byte"
            PrimitiveType.CHAR -> "java.lang.char"
            PrimitiveType.DOUBLE -> "java.lang.double"
            PrimitiveType.FLOAT -> "java.lang.float"
            PrimitiveType.INT -> "java.lang.int"
            PrimitiveType.LONG -> "java.lang.long"
            PrimitiveType.SHORT -> "java.lang.short"
            else -> throw IllegalArgumentException("Unsupported primitive type.")
        }
    }

    /**
     * Kotlin does not support primitives.
     */
    override fun primitiveSignature(type: Any): String {
        throw UnsupportedOperationException("This should never happen.")
    }

    /**
     * Generate a unique variable type signature.
     */
    @OptIn(SymbolInternals::class)
    fun variableSignature(
        symbol: FirVariableSymbol<out FirVariable>,
        ownerSymbol: @Nullable FirBasedSymbol<*>?
    ): String {
        var owner = "{undefined}"
        val kotlinType = symbol.dispatchReceiverType
        if (kotlinType is ConeClassLikeType) {
            val regularClass = convertToRegularClass(kotlinType)
            if (regularClass != null) {
                owner = signature(regularClass)
                if (owner.contains("<")) {
                    owner = owner.substring(0, owner.indexOf('<'))
                }
            }
        } else if (symbol.callableId.classId != null) {
            owner = convertClassIdToFqn(symbol.callableId.classId)
            if (owner.contains("<")) {
                owner = owner.substring(0, owner.indexOf('<'))
            }
        } else if (ownerSymbol is FirFunctionSymbol<*>) {
            owner = methodDeclarationSignature(ownerSymbol, null)
        } else if (ownerSymbol != null) {
            owner = classSignature(ownerSymbol.fir)
        }
        val typeSig =
            if (symbol.fir is FirJavaField || symbol.fir is FirEnumEntry) {
                signature(symbol.fir.returnTypeRef)
            } else {
                signature(symbol.resolvedReturnTypeRef)
            }
        return owner + "{name=" + symbol.name.asString() + ",type=" + typeSig + '}'
    }

    fun variableSignature(javaField: JavaField): String {
        var owner = signature(javaField.containingClass)
        if (owner.contains("<")) {
            owner = owner.substring(0, owner.indexOf('<'))
        }
        return owner + "{name=" + javaField.name.asString() + ",type=" + signature(javaField.type) + '}'
    }

    @OptIn(SymbolInternals::class)
    fun methodSignature(functionCall: FirFunctionCall, ownerSymbol: @Nullable FirBasedSymbol<*>?): String {
        var owner = "{undefined}"
        if (functionCall.explicitReceiver != null) {
            owner = signature(functionCall.explicitReceiver!!.typeRef)
        } else if (functionCall.calleeReference is FirResolvedNamedReference) {
            if ((functionCall.calleeReference as FirResolvedNamedReference).resolvedSymbol is FirNamedFunctionSymbol) {
                val resolvedSymbol =
                    (functionCall.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirNamedFunctionSymbol
                if (resolvedSymbol.getOwnerLookupTag() != null) {
                    owner = signature(resolvedSymbol.getOwnerLookupTag()?.toFirRegularClassSymbol(firSession), ownerSymbol)
                } else if (resolvedSymbol.origin === FirDeclarationOrigin.Library) {
                    if (resolvedSymbol.fir.containerSource is JvmPackagePartSource) {
                        val source = resolvedSymbol.fir.containerSource as JvmPackagePartSource?
                        owner = if (source!!.facadeClassName != null) {
                            convertKotlinFqToJavaFq(
                                source.facadeClassName.toString()
                            )
                        } else {
                            convertKotlinFqToJavaFq(
                                source.className.toString()
                            )
                        }
                    } else if (!resolvedSymbol.fir.origin.fromSource &&
                        !resolvedSymbol.fir.origin.fromSupertypes &&
                        !resolvedSymbol.fir.origin.generated
                    ) {
                        owner = "kotlin.Library"
                    }
                } else if (resolvedSymbol.origin === FirDeclarationOrigin.Source && ownerSymbol != null) {
                    when (ownerSymbol) {
                        is FirFileSymbol -> {
                            owner = ownerSymbol.fir.name
                        }

                        is FirNamedFunctionSymbol -> {
                            owner = signature(ownerSymbol.fir)
                        }

                        is FirRegularClassSymbol -> {
                            owner = signature(ownerSymbol.fir)
                        }
                    }
                }
            }
        }
        var s = owner
        val namedReference = functionCall.calleeReference
        s += if (namedReference is FirResolvedNamedReference &&
            namedReference.resolvedSymbol is FirConstructorSymbol
        ) {
            "{name=<constructor>,return=$s"
        } else {
            "{name=" + functionCall.calleeReference.name.asString() +
                    ",return=" + signature(functionCall.typeRef)
        }
        return s + ",parameters=" + methodArgumentSignature((functionCall.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirFunctionSymbol<out FirFunction>) + '}'
    }

    /**
     * Generate the method declaration signature.
     */
    @OptIn(SymbolInternals::class)
    fun methodDeclarationSignature(symbol: FirFunctionSymbol<out FirFunction>,
                                   ownerSymbol: @Nullable FirBasedSymbol<*>?): String {
        var s: String =
            when {
                symbol is FirConstructorSymbol -> {
                    classSignature(symbol.resolvedReturnTypeRef)
                }
                symbol.dispatchReceiverType != null -> {
                    classSignature(symbol.dispatchReceiverType!!)
                }
                symbol.getOwnerLookupTag() != null && symbol.getOwnerLookupTag()!!.toFirRegularClassSymbol(firSession) != null -> {
                    classSignature(symbol.getOwnerLookupTag()!!.toFirRegularClass(firSession)!!)
                }
                ownerSymbol != null -> {
                    signature(ownerSymbol.fir)
                }
                else -> {
                    "{undefined}"
                }
            }
        s += if (symbol is FirConstructorSymbol) {
            "{name=<constructor>,return=$s"
        } else {
            val returnSignature: String = if (symbol.fir is FirJavaMethod) {
                signature(symbol.fir.returnTypeRef)
            } else {
                signature(symbol.resolvedReturnTypeRef)
            }
            "{name=" + symbol.name.asString() +
                    ",return=" + returnSignature
        }
        return s + ",parameters=" + methodArgumentSignature(symbol) + '}'
    }

    fun methodDeclarationSignature(method: JavaMethod): String {
        var s = javaClassSignature(method.containingClass)
        val returnSignature = signature(method.returnType)
        s += "{name=" + method.name.asString() +
                ",return=" + returnSignature
        return s + ",parameters=" + javaMethodArgumentSignature(method.valueParameters) + '}'
    }

    fun methodConstructorSignature(method: JavaConstructor): String {
        var s = javaClassSignature(method.containingClass)
        s += "{name=<constructor>,return=$s"
        return s + ",parameters=" + javaMethodArgumentSignature(method.valueParameters) + '}'
    }

    /**
     * Generate the method argument signature.
     */
    @OptIn(SymbolInternals::class)
    private fun methodArgumentSignature(sym: FirFunctionSymbol<out FirFunction>): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        if (sym.receiverParameter != null) {
            genericArgumentTypes.add(signature(sym.receiverParameter!!.typeRef))
        }
        for (parameterSymbol in sym.valueParameterSymbols) {
            val paramSignature: String = if (parameterSymbol.fir is FirJavaValueParameter) {
                signature(parameterSymbol.fir.returnTypeRef, sym)
            } else {
                signature(parameterSymbol.resolvedReturnType, sym)
            }
            genericArgumentTypes.add(paramSignature)
        }
        return genericArgumentTypes.toString()
    }

    private fun javaMethodArgumentSignature(valueParameters: List<JavaValueParameter>): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        for (valueParameter in valueParameters) {
            genericArgumentTypes.add(signature(valueParameter))
        }
        return genericArgumentTypes.toString()
    }

    /**
     * Converts the ConeKotlinType to it's FirRegularClass.
     */
    @OptIn(SymbolInternals::class)
    fun convertToRegularClass(kotlinType: @Nullable ConeKotlinType?): @Nullable FirRegularClass? {
        if (kotlinType != null) {
            val symbol = kotlinType.toRegularClassSymbol(
                firSession
            )
            if (symbol != null) {
                return symbol.fir
            }
        }
        return null
    }

    companion object {
        /**
         * Converts the Kotlin ClassId to a [org.openrewrite.java.tree.J] style FQN.
         */
        fun convertClassIdToFqn(classId: @Nullable ClassId?): String {
            return if (classId == null) "{undefined}" else convertKotlinFqToJavaFq(classId.toString())
        }

        fun convertFileNameToFqn(name: String): String {
            return name.replace("/", ".").replace("\\", ".").replace(".kt", "Kt")
        }

        /**
         * Converts the Kotlin FQN to a [org.openrewrite.java.tree.J] style FQN.
         */
        fun convertKotlinFqToJavaFq(kotlinFqn: String): String {
            val cleanedFqn = kotlinFqn
                .replace(".", "$")
                .replace("/", ".")
                .replace("?", "")
            return if (cleanedFqn.startsWith(".")) cleanedFqn.replaceFirst(".".toRegex(), "") else cleanedFqn
        }
    }
}
