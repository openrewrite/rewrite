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

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
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
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaConstructor
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaMethod
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.StandardClassIds
import org.openrewrite.Incubating
import org.openrewrite.java.JavaTypeMapping
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.convertClassIdToFqn
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.convertFileNameToFqn
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.convertKotlinFqToJavaFq

@Incubating(since = "0.0")
class KotlinTypeMapping(typeCache: JavaTypeCache, firSession: FirSession) : JavaTypeMapping<Any> {
    private val signatureBuilder: KotlinTypeSignatureBuilder
    private val typeCache: JavaTypeCache
    private val firSession: FirSession

    init {
        signatureBuilder = KotlinTypeSignatureBuilder(firSession)
        this.typeCache = typeCache
        this.firSession = firSession
    }

    override fun type(type: Any?): JavaType {
        return type(type, null) ?: JavaType.Unknown.getInstance()
    }

    fun type(type: Any?, ownerFallBack: FirBasedSymbol<*>?): JavaType? {
        if (type == null) {
            return JavaType.Unknown.getInstance()
        }

        val signature = signatureBuilder.signature(type, ownerFallBack)
        val existing = typeCache.get<JavaType>(signature)
        if (existing != null) {
            return existing
        }

        when (type) {
            is String -> {
                // Kotlin only resolves the members necessary in a file like `Collection.kt` that wraps `listOf`, `mapOf`, etc.
                // The owner type may be constructed through a String and is represented with a ShallowClass.
                // type(..) handles the string value to reuse the same shallow class.
                val javaType: JavaType = JavaType.ShallowClass.build(type)
                typeCache.put(signature, javaType)
                return javaType
            }

            is FirClass -> {
                return classType(type, signature, ownerFallBack)
            }

            is FirFunction -> {
                return methodDeclarationType(type as FirFunction?, null, ownerFallBack)
            }

            is FirVariable -> {
                return variableType(type.symbol, null, ownerFallBack)
            }

            is FirFile -> {
                return JavaType.ShallowClass.build(convertFileNameToFqn(type.name))
            }

            is FirJavaTypeRef -> {
                return type(type.type, ownerFallBack)
            }

            is org.jetbrains.kotlin.load.java.structure.JavaType -> {
                return mapJavaType(type, signature)
            }

            is JavaElement -> {
                return mapJavaElementType(type, signature)
            }

            is FirResolvedQualifier -> {
                return classType(type, signature, ownerFallBack)
            }

            else -> return resolveType(type, signature, ownerFallBack)
        }
    }

    @OptIn(SymbolInternals::class)
    private fun resolveType(
        type: Any,
        signature: String,
        ownerFallBack: FirBasedSymbol<*>?
    ): JavaType? {
        when (type) {
            is ConeTypeProjection -> {
                return resolveConeTypeProjection(type, signature, ownerFallBack)
            }

            is FirExpression -> {
                return type(type.typeRef, ownerFallBack)
            }

            is FirFunctionTypeRef -> {
                return type(type.returnTypeRef, ownerFallBack)
            }

            is FirResolvedNamedReference -> {
                when (val resolvedSymbol = type.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        return type(resolvedSymbol.resolvedReturnTypeRef, ownerFallBack)
                    }

                    is FirEnumEntrySymbol -> {
                        return type(resolvedSymbol.resolvedReturnTypeRef, ownerFallBack)
                    }

                    is FirNamedFunctionSymbol -> {
                        return type(resolvedSymbol.resolvedReturnTypeRef, ownerFallBack)
                    }

                    is FirPropertySymbol -> {
                        return type(resolvedSymbol.resolvedReturnTypeRef, ownerFallBack)
                    }

                    is FirValueParameterSymbol -> {
                        return type(resolvedSymbol.resolvedReturnType, ownerFallBack)
                    }

                    is FirFieldSymbol -> {
                        return type(resolvedSymbol.resolvedReturnType, ownerFallBack)
                    }
                }
            }

            is FirResolvedTypeRef -> {
                val coneKotlinType: ConeKotlinType = type.coneType
                if (coneKotlinType is ConeTypeParameterType) {
                    val classifierSymbol = coneKotlinType.lookupTag.toSymbol(firSession)
                    if (classifierSymbol != null && classifierSymbol.fir is FirTypeParameter) {
                        return resolveConeTypeProjection(classifierSymbol.fir as FirTypeParameter, signature)
                    }
                }
                return classType(type, signature, ownerFallBack)
            }

            is FirTypeParameter -> {
                return resolveConeTypeProjection(type, signature)
            }

            is FirVariableAssignment -> {
                return type(type.lValue, ownerFallBack)
            }
        }
        return null
    }

    private fun array(type: JavaArrayType, signature: String): JavaType {
        val arr = JavaType.Array(null, null)
        typeCache.put(signature, arr)
        arr.unsafeSet(type(type.componentType))
        return arr
    }

    @OptIn(SymbolInternals::class)
    private fun classType(
        classType: Any,
        signature: String,
        ownerFallBack: FirBasedSymbol<*>?
    ): JavaType.FullyQualified {
        val firClass: FirClass
        var resolvedTypeRef: FirResolvedTypeRef? = null
        if (classType is FirResolvedTypeRef) {
            // The resolvedTypeRef is used to create parameterized types.
            resolvedTypeRef = classType
            var type = resolvedTypeRef.type
            if (type is ConeFlexibleType) {
                // for platform types the lower bound is the nullable type
                type = type.lowerBound
            }
            val symbol = type.toRegularClassSymbol(firSession)
            if (symbol == null) {
                typeCache.put(signature, JavaType.Unknown.getInstance())
                return JavaType.Unknown.getInstance()
            }
            firClass = symbol.fir
        } else if (classType is FirResolvedQualifier) {
            when (classType.symbol) {
                is FirTypeAliasSymbol -> {
                    return classType(
                        (classType.symbol as FirTypeAliasSymbol).resolvedExpandedTypeRef,
                        signature,
                        ownerFallBack
                    )
                }

                is FirRegularClassSymbol -> {
                    firClass = classType.symbol!!.fir as FirClass
                }

                else -> {
                    return JavaType.Unknown.getInstance()
                }
            }
        } else {
            firClass = classType as FirClass
        }
        val sym = firClass.symbol
        val classFqn: String = convertClassIdToFqn(sym.classId)
        val fq: JavaType.FullyQualified? = typeCache.get<JavaType.FullyQualified>(classFqn)
        if (fq is JavaType.Unknown) {
            return fq
        }
        var clazz: JavaType.Class? = (if (fq is JavaType.Parameterized) fq.type else fq) as JavaType.Class?
        if (clazz == null) {
            clazz = JavaType.Class(
                null,
                convertToFlagsBitMap(firClass.status),
                classFqn,
                convertToClassKind(firClass.classKind),
                null, null, null, null, null, null, null
            )
            typeCache.put(classFqn, clazz)
            var superTypeRef: FirTypeRef? = null
            var interfaceTypeRefs: MutableList<FirTypeRef>? = null
            for (typeRef: FirTypeRef in firClass.superTypeRefs) {
                val symbol = typeRef.coneType.toRegularClassSymbol(firSession)
                if (symbol != null && ClassKind.CLASS == symbol.fir.classKind) {
                    superTypeRef = typeRef
                } else if (symbol != null && ClassKind.INTERFACE == symbol.fir.classKind) {
                    if (interfaceTypeRefs == null) {
                        interfaceTypeRefs = ArrayList()
                    }
                    interfaceTypeRefs.add(typeRef)
                }
            }
            val supertype =
                if (superTypeRef == null || "java.lang.Object" == signature) null else TypeUtils.asFullyQualified(
                    type(superTypeRef)
                )
            var owner: JavaType.FullyQualified? = null
            if (!firClass.isLocal && firClass.symbol.classId.isNestedClass) {
                val ownerSymbol = firClass.symbol.classId.outerClassId!!.toSymbol(firSession)
                if (ownerSymbol != null) {
                    owner = TypeUtils.asFullyQualified(type(ownerSymbol.fir))
                }
            } else if (firClass.symbol.classId.isNestedClass && ownerFallBack != null) {
                owner = TypeUtils.asFullyQualified(type(ownerFallBack.fir))
            }
            val properties: MutableList<FirProperty> = ArrayList(firClass.declarations.size)
            val javaFields: MutableList<FirJavaField> = ArrayList(firClass.declarations.size)
            val functions: MutableList<FirFunction> = ArrayList(firClass.declarations.size)
            val enumEntries: MutableList<FirEnumEntry> = ArrayList(firClass.declarations.size)
            for (declaration: FirDeclaration in firClass.declarations) {
                if (declaration is FirProperty) {
                    if (declaration.source == null || declaration.source!!.kind !is KtFakeSourceElementKind) {
                        properties.add(declaration)
                    }
                } else if (declaration is FirJavaField) {
                    javaFields.add(declaration)
                } else if (declaration is FirSimpleFunction) {
                    functions.add(declaration as FirFunction)
                } else if (declaration is FirConstructor) {
                    functions.add(declaration as FirFunction)
                } else if (declaration is FirEnumEntry) {
                    enumEntries.add(declaration)
                }
            }
            var fields: MutableList<JavaType.Variable>? = null
            if (enumEntries.isNotEmpty()) {
                fields = ArrayList(properties.size + enumEntries.size)
                for (enumEntry: FirEnumEntry in enumEntries) {
                    val vt = variableType(enumEntry.symbol, clazz, ownerFallBack)
                    if (vt != null) {
                        fields.add(vt)
                    }
                }
            }
            if (properties.isNotEmpty()) {
                if (fields == null) {
                    fields = ArrayList(properties.size)
                }
                for (property: FirProperty in properties) {
                    val vt = variableType(property.symbol, clazz, ownerFallBack)
                    if (vt != null) {
                        fields.add(vt)
                    }
                }
            }
            if (javaFields.isNotEmpty()) {
                if (fields == null) {
                    fields = ArrayList(javaFields.size)
                }
                for (field: FirJavaField in javaFields) {
                    val vt = variableType(field.symbol, clazz, ownerFallBack)
                    if (vt != null) {
                        fields.add(vt)
                    }
                }
            }
            var methods: MutableList<JavaType.Method>? = null
            if (functions.isNotEmpty()) {
                methods = ArrayList(functions.size)
                for (function: FirFunction in functions) {
                    val mt = methodDeclarationType(function, clazz, ownerFallBack)
                    if (mt != null) {
                        methods.add(mt)
                    }
                }
            }
            var interfaces: MutableList<JavaType.FullyQualified>? = null
            if (!interfaceTypeRefs.isNullOrEmpty()) {
                interfaces = ArrayList(interfaceTypeRefs.size)
                for (iParam: FirTypeRef? in interfaceTypeRefs) {
                    val javaType = TypeUtils.asFullyQualified(type(iParam))
                    if (javaType != null) {
                        interfaces.add(javaType)
                    }
                }
            }
            val annotations = listAnnotations(firClass.annotations)
            clazz.unsafeSet(null, supertype, owner, annotations, interfaces, fields, methods)
        }

        if (firClass.typeParameters.isNotEmpty()) {
            val jfq = typeCache.get<JavaType.FullyQualified>(signature)
            if (jfq is JavaType.Class) {
                throw IllegalStateException("Expected JavaType.Parameterized for signature : $signature")
            }
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                val typeParameters: MutableList<JavaType> = ArrayList(firClass.typeParameters.size)
                if (resolvedTypeRef != null && resolvedTypeRef.type.typeArguments.isNotEmpty()) {
                    for (typeArgument: ConeTypeProjection in resolvedTypeRef.type.typeArguments) {
                        typeParameters.add(type(typeArgument))
                    }
                } else {
                    for (tParam: FirTypeParameterRef in firClass.typeParameters) {
                        typeParameters.add(type(tParam))
                    }
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    private fun classType(classifier: BinaryJavaClass, signature: String): JavaType {
        var clazz = typeCache.get<JavaType.Class>(classifier.fqName.asString())
        if (clazz == null) {
            clazz = JavaType.Class(
                null,
                classifier.access.toLong(),
                classifier.fqName.asString(),
                convertToClassKind(classifier),
                null, null, null, null, null, null, null
            )
            typeCache.put(classifier.fqName.asString(), clazz)
            var supertype: JavaType.FullyQualified? = null
            var interfaces: MutableList<JavaType.FullyQualified>? = null
            for (classifierSupertype: JavaClassifierType in classifier.supertypes) {
                if (classifierSupertype.classifier is JavaClass) {
                    if ((classifierSupertype.classifier as JavaClass?)!!.isInterface) {
                        if (interfaces == null) {
                            interfaces = ArrayList()
                        }
                        interfaces.add(type(classifierSupertype) as JavaType.FullyQualified)
                    } else if ("java.lang.Object" != signature) {
                        supertype = type(classifierSupertype) as JavaType.FullyQualified
                    }
                }
            }
            var owner: JavaType.FullyQualified? = null
            if (classifier.outerClass != null) {
                owner = TypeUtils.asFullyQualified(type(classifier.outerClass))
            }
            var fields: MutableList<JavaType.Variable>? = null
            if (classifier.fields.isNotEmpty()) {
                fields = ArrayList(classifier.fields.size)
                for (field: JavaField in classifier.fields) {
                    val vt = variableType(field, clazz)
                    if (vt != null) {
                        fields.add(vt)
                    }
                }
            }
            var methods: MutableList<JavaType.Method>? = null
            if (classifier.methods.isNotEmpty()) {
                methods = ArrayList(classifier.methods.size)
                for (method: JavaMethod in classifier.methods) {
                    val mt = methodDeclarationType(method, clazz)
                    if (mt != null) {
                        methods.add(mt)
                    }
                }
            }
            if (classifier.constructors.isNotEmpty()) {
                for (method: JavaConstructor in classifier.constructors) {
                    if (method is BinaryJavaConstructor) {
                        if (methods == null) {
                            methods = ArrayList()
                        }
                        // Filter out the same methods as JavaTypeMapping: Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL | Flags.ANONCONSTR
                        if (method.access.toLong() and ((1 shl 12).toLong() or (1L shl 31) or (1L shl 37) or (1 shl 29).toLong()) == 0L) {
                            val ms = methodConstructorType(method, clazz)
                            if (ms != null) {
                                methods.add(ms)
                            }
                        }
                    }
                }
            }
            var typeParameters: MutableList<JavaType>? = null
            if (classifier.typeParameters.isNotEmpty()) {
                typeParameters = ArrayList(classifier.typeParameters.size)
                for (typeArgument: JavaTypeParameter in classifier.typeParameters) {
                    typeParameters.add(type(typeArgument))
                }
            }
            clazz.unsafeSet(
                typeParameters,
                supertype,
                owner,
                listAnnotations(classifier.annotations),
                interfaces,
                fields,
                methods
            )
        }
        if (!classifier.typeParameters.isEmpty()) {
            val jfq = typeCache.get<JavaType.FullyQualified>(signature)
            if (jfq is JavaType.Class) {
                throw IllegalStateException("Expected JavaType.Parameterized for signature : $signature")
            }
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                val typeParameters: MutableList<JavaType> = ArrayList(classifier.typeParameters.size)
                for (typeArgument: JavaTypeParameter in classifier.typeParameters) {
                    typeParameters.add(type(typeArgument))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    @OptIn(SymbolInternals::class)
    fun methodDeclarationType(
        function: FirFunction?,
        declaringType: JavaType.FullyQualified?,
        ownerFallBack: FirBasedSymbol<*>?
    ): JavaType.Method? {
        val methodSymbol = function?.symbol
        if (methodSymbol != null) {
            val signature = signatureBuilder.methodDeclarationSignature(function.symbol)
            val existing = typeCache.get<JavaType.Method>(signature)
            if (existing != null) {
                return existing
            }
            var paramNames: MutableList<String>? = null
            if (!methodSymbol.valueParameterSymbols.isEmpty()) {
                paramNames = ArrayList(methodSymbol.valueParameterSymbols.size)
                for (p: FirValueParameterSymbol in methodSymbol.valueParameterSymbols) {
                    val s = p.name.asString()
                    paramNames.add(s)
                }
            }
            val defaultValues: List<String>? = null
            val method = JavaType.Method(
                null,
                convertToFlagsBitMap(methodSymbol.resolvedStatus),
                null,
                if (methodSymbol is FirConstructorSymbol) "<constructor>" else methodSymbol.name.asString(),
                null,
                paramNames,
                null, null, null,
                defaultValues
            )
            typeCache.put(signature, method)
            var resolvedDeclaringType = declaringType
            if (declaringType == null) {
                if (methodSymbol is FirConstructorSymbol) {
                    resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.resolvedReturnType))
                } else if (methodSymbol.dispatchReceiverType != null) {
                    resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.dispatchReceiverType))
                } else if (ownerFallBack != null) {
                    resolvedDeclaringType = TypeUtils.asFullyQualified(type(ownerFallBack.fir))
                }
            }
            if (resolvedDeclaringType == null) {
                return null
            }
            val returnType =
                if (function is FirJavaMethod) type(methodSymbol.fir.returnTypeRef) else type(methodSymbol.resolvedReturnTypeRef)
            var parameterTypes: MutableList<JavaType>? = null
            if (methodSymbol.valueParameterSymbols.isNotEmpty()) {
                parameterTypes = ArrayList(methodSymbol.valueParameterSymbols.size)
                for (parameterSymbol: FirValueParameterSymbol in methodSymbol.valueParameterSymbols) {
                    val javaType: JavaType = if (parameterSymbol.fir is FirJavaValueParameter) {
                        type(parameterSymbol.fir.returnTypeRef)
                    } else {
                        type(parameterSymbol.resolvedReturnTypeRef)
                    }
                    parameterTypes.add(javaType)
                }
            }
            method.unsafeSet(
                resolvedDeclaringType,
                if (methodSymbol is FirConstructorSymbol) resolvedDeclaringType else returnType,
                parameterTypes, null, listAnnotations(methodSymbol.annotations)
            )
            return method
        }
        return null
    }

    fun methodDeclarationType(
        javaMethod: JavaMethod?,
        declaringType: JavaType.FullyQualified?
    ): JavaType.Method? {
        if (javaMethod != null) {
            val signature = signatureBuilder.methodDeclarationSignature(javaMethod)
            val existing = typeCache.get<JavaType.Method>(signature)
            if (existing != null) {
                return existing
            }
            var paramNames: MutableList<String>? = null
            if (!javaMethod.valueParameters.isEmpty()) {
                paramNames = ArrayList(javaMethod.valueParameters.size)
                val valueParameters = javaMethod.valueParameters
                // Generate names for parameters that match the output for the Java compiler.
                for (i in valueParameters.indices) {
                    paramNames.add("arg$i")
                }
            }
            var defaultValues: MutableList<String>? = null
            if (javaMethod.annotationParameterDefaultValue != null) {
                if (javaMethod.annotationParameterDefaultValue!!.name != null) {
                    defaultValues = ArrayList()
                    defaultValues.add(javaMethod.annotationParameterDefaultValue!!.name!!.asString())
                }
            }
            val method: JavaType.Method = JavaType.Method(
                null,
                (if (javaMethod is BinaryJavaMethod) {
                    javaMethod.access.toLong()
                } else {
                    convertToFlagsBitMap(
                        javaMethod.visibility,
                        javaMethod.isStatic,
                        javaMethod.isFinal,
                        javaMethod.isAbstract
                    )
                }),
                null,
                javaMethod.name.asString(),
                null,
                paramNames,
                null, null, null,
                defaultValues
            )
            typeCache.put(signature, method)
            val exceptionTypes: List<JavaType.FullyQualified>? = null
            var resolvedDeclaringType = declaringType
            if (declaringType == null) {
                resolvedDeclaringType = TypeUtils.asFullyQualified(type(javaMethod.containingClass))
            }
            if (resolvedDeclaringType == null) {
                return null
            }
            val returnType = type(javaMethod.returnType)
            var parameterTypes: MutableList<JavaType>? = null
            if (javaMethod.valueParameters.isNotEmpty()) {
                parameterTypes = ArrayList(javaMethod.valueParameters.size)
                for (parameterSymbol: JavaValueParameter in javaMethod.valueParameters) {
                    val javaType = type(parameterSymbol.type)
                    parameterTypes.add(javaType)
                }
            }
            method.unsafeSet(
                resolvedDeclaringType,
                returnType,
                parameterTypes, exceptionTypes, listAnnotations(javaMethod.annotations)
            )
            return method
        }
        return null
    }

    private fun methodConstructorType(
        constructor: JavaConstructor?,
        declaringType: JavaType.FullyQualified?
    ): JavaType.Method? {
        if (constructor != null) {
            val signature = signatureBuilder.methodConstructorSignature(constructor)
            val existing = typeCache.get<JavaType.Method>(signature)
            if (existing != null) {
                return existing
            }
            var paramNames: MutableList<String>? = null
            if (constructor.valueParameters.isNotEmpty()) {
                paramNames = ArrayList(constructor.valueParameters.size)
                val valueParameters = constructor.valueParameters
                for (i in valueParameters.indices) {
                    paramNames.add("arg$i")
                }
            }
            val defaultValues: List<String>? = null
            val method: JavaType.Method = JavaType.Method(
                null,
                (if (constructor is BinaryJavaConstructor) {
                    constructor.access.toLong()
                } else {
                    convertToFlagsBitMap(
                        constructor.visibility,
                        constructor.isStatic,
                        constructor.isFinal,
                        constructor.isAbstract
                    )
                }),
                null,
                "<constructor>",
                null,
                paramNames,
                null, null, null,
                defaultValues
            )
            typeCache.put(signature, method)
            val exceptionTypes: List<JavaType.FullyQualified>? = null
            var resolvedDeclaringType = declaringType
            if (declaringType == null) {
                resolvedDeclaringType = TypeUtils.asFullyQualified(type(constructor.containingClass))
            }
            if (resolvedDeclaringType == null) {
                return null
            }
            var parameterTypes: MutableList<JavaType>? = null
            if (constructor.valueParameters.isNotEmpty()) {
                parameterTypes = ArrayList(constructor.valueParameters.size)
                for (parameterSymbol: JavaValueParameter in constructor.valueParameters) {
                    val javaType = type(parameterSymbol.type)
                    parameterTypes.add(javaType)
                }
            }
            method.unsafeSet(
                resolvedDeclaringType,
                resolvedDeclaringType,
                parameterTypes, exceptionTypes, listAnnotations(constructor.annotations)
            )
            return method
        }
        return null
    }

    @OptIn(SymbolInternals::class)
    fun methodInvocationType(
        functionCall: FirFunctionCall?,
        ownerSymbol: FirBasedSymbol<*>?
    ): JavaType.Method? {
        if (functionCall == null || functionCall.calleeReference is FirErrorNamedReference) {
            return null
        }
        val signature = signatureBuilder.methodSignature(functionCall, ownerSymbol)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }
        val symbol = (functionCall.calleeReference as FirResolvedNamedReference).resolvedSymbol
        var constructor: FirConstructor? = null
        var simpleFunction: FirSimpleFunction? = null
        if (symbol is FirConstructorSymbol) {
            constructor = symbol.fir
        } else {
            simpleFunction = symbol.fir as FirSimpleFunction
        }
        var paramNames: MutableList<String>? = null
        if (simpleFunction?.receiverParameter != null) {
            paramNames = ArrayList(simpleFunction.valueParameters.size + 1)
            paramNames.add('$'+ "this" + '$')
        }
        if (simpleFunction != null && simpleFunction.valueParameters.isNotEmpty()) {
            paramNames = paramNames ?: ArrayList(simpleFunction.valueParameters.size)
            for (p: FirValueParameter in simpleFunction.valueParameters) {
                val s = p.name.asString()
                paramNames.add(s)
            }
        } else if (constructor != null && constructor.valueParameters.isNotEmpty()) {
            paramNames = paramNames ?: ArrayList(constructor.valueParameters.size)
            for (p: FirValueParameter in constructor.valueParameters) {
                val s = p.name.asString()
                paramNames.add(s)
            }
        }
        val method = JavaType.Method(
            null,
            convertToFlagsBitMap(constructor?.status ?: simpleFunction!!.status),
            null,
            if (constructor != null) "<constructor>" else simpleFunction!!.name.asString(),
            null,
            paramNames,
            null, null, null, null
        )
        typeCache.put(signature, method)
        var parameterTypes: MutableList<JavaType>? = null
        if (simpleFunction?.receiverParameter != null) {
            parameterTypes = ArrayList(simpleFunction.valueParameters.size + 1)
            parameterTypes.add(type(simpleFunction.receiverParameter!!.typeRef))
        }
        if (constructor != null && constructor.valueParameters.isNotEmpty()) {
            parameterTypes = ArrayList(constructor.valueParameters.size)
            for (argtype: FirValueParameter? in constructor.valueParameters) {
                if (argtype != null) {
                    val javaType = type(argtype)
                    parameterTypes.add(javaType)
                }
            }
        } else if (simpleFunction != null && simpleFunction.valueParameters.isNotEmpty()) {
            parameterTypes = parameterTypes ?: ArrayList(simpleFunction.valueParameters.size)
            for (parameter in simpleFunction.valueParameters) {
                val parameterSymbol = parameter.symbol
                val javaType: JavaType = if (parameterSymbol.fir is FirJavaValueParameter) {
                    type(parameterSymbol.fir.returnTypeRef)
                } else {
                    type(parameterSymbol.resolvedReturnTypeRef)
                }
                parameterTypes.add(javaType)
            }
        }

        var resolvedDeclaringType: JavaType.FullyQualified? = null
        if (functionCall.calleeReference is FirResolvedNamedReference) {
            if ((functionCall.calleeReference as FirResolvedNamedReference).resolvedSymbol is FirNamedFunctionSymbol) {
                val resolvedSymbol =
                    (functionCall.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirNamedFunctionSymbol
                if (resolvedSymbol.containingClassLookupTag() != null) {
                    val lookupTag: ConeClassLikeLookupTag = resolvedSymbol.containingClassLookupTag()!!
                    val classSymbol: FirRegularClassSymbol? = lookupTag.toFirRegularClassSymbol(firSession)
                    if (classSymbol != null) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type(classSymbol.fir))
                    }
                } else if (resolvedSymbol.origin === FirDeclarationOrigin.Library) {
                    if (resolvedSymbol.fir.containerSource is JvmPackagePartSource) {
                        val source: JvmPackagePartSource? = resolvedSymbol.fir.containerSource as JvmPackagePartSource?
                        if (source != null) {
                            if (source.facadeClassName != null) {
                                resolvedDeclaringType = TypeUtils.asFullyQualified(
                                    type(
                                        convertKotlinFqToJavaFq(
                                            source.facadeClassName.toString()
                                        )
                                    )
                                )
                            } else {
                                resolvedDeclaringType = TypeUtils.asFullyQualified(
                                    type(
                                        convertKotlinFqToJavaFq(
                                            source.className.toString()
                                        )
                                    )
                                )
                            }
                        }
                    } else if (!resolvedSymbol.fir.origin.generated &&
                        !resolvedSymbol.fir.origin.fromSupertypes &&
                        !resolvedSymbol.fir.origin.fromSource
                    ) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type("kotlin.Library"))
                    }
                } else if (resolvedSymbol.origin === FirDeclarationOrigin.Source && ownerSymbol != null) {
                    when (ownerSymbol) {
                        is FirFileSymbol -> {
                            resolvedDeclaringType = TypeUtils.asFullyQualified(type(ownerSymbol.fir))
                        }

                        is FirNamedFunctionSymbol -> {
                            resolvedDeclaringType = TypeUtils.asFullyQualified(type(ownerSymbol.fir))
                        }

                        is FirRegularClassSymbol -> {
                            resolvedDeclaringType = TypeUtils.asFullyQualified(type(ownerSymbol.fir))
                        }
                    }
                }
            }
        }
        if (resolvedDeclaringType == null) {
            return null
        }
        val returnType = type(functionCall.typeRef, ownerSymbol)
        method.unsafeSet(
            resolvedDeclaringType,
            if (constructor != null) resolvedDeclaringType else returnType,
            parameterTypes, null, listAnnotations(constructor?.annotations ?: simpleFunction!!.annotations)
        )
        return method
    }

    @OptIn(SymbolInternals::class)
    fun variableType(
        symbol: FirVariableSymbol<out FirVariable>?,
        owner: JavaType.FullyQualified?,
        ownerFallBack: FirBasedSymbol<*>?
    ): JavaType.Variable? {
        if (symbol == null) {
            return null
        }
        val signature = signatureBuilder.variableSignature(symbol, ownerFallBack)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        val variable = JavaType.Variable(
            null,
            convertToFlagsBitMap(symbol.rawStatus),
            symbol.name.asString(),
            null, null, null
        )
        typeCache.put(signature, variable)
        val annotations = listAnnotations(symbol.annotations)
        var resolvedOwner: JavaType? = owner
        if (owner == null && ownerFallBack != null) {
            // There isn't a way to link a Callable back to the owner unless it's a class member, but class members already set the owner.
            // The fallback isn't always safe and may result in type erasure.
            // We'll need to find the owner in the parser to set this on properties and variables in local scopes.
            resolvedOwner = type(ownerFallBack.fir)
        }
        if (resolvedOwner == null) {
            resolvedOwner = JavaType.Unknown.getInstance()
        }
        val typeRef =
            if (symbol.fir is FirJavaField || symbol.fir is FirEnumEntry) symbol.fir.returnTypeRef else symbol.resolvedReturnTypeRef
        variable.unsafeSet(resolvedOwner!!, type(typeRef), annotations)
        return variable
    }

    fun variableType(javaField: JavaField, owner: JavaType.FullyQualified?): JavaType.Variable? {
        val signature = signatureBuilder.variableSignature(javaField)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        val variable = JavaType.Variable(
            null,
            convertToFlagsBitMap(javaField.visibility, javaField.isStatic, javaField.isFinal, javaField.isAbstract),
            javaField.name.asString(),
            null, null, null
        )
        typeCache.put(signature, variable)
        var resolvedOwner: JavaType? = owner
        if (owner == null) {
            resolvedOwner = TypeUtils.asFullyQualified(type(javaField.containingClass))
            assert(resolvedOwner != null)
        }
        variable.unsafeSet(resolvedOwner!!, type(javaField.type), listAnnotations(javaField.annotations))
        return variable
    }

    fun primitive(type: ConeClassLikeType): JavaType.Primitive {
        // This may need to change in the future. The Kotlin primitives are converted to Java primitives, which is
        // correct for the resultant byte code that runs on the JVM, and helps to support `J`.
        // However, it is technically incorrect in terms of representing the types from the source code on the LST.
        // The transformation happens because `J.Literal` requires a `JavaType.Primitive`, and does not support
        // Kotlin's primitives. The result is Kotlin primitives are not represented in the type hierarchy, which may
        // cause issues as more Kotlin recipes are introduced.
        val classId = type.lookupTag.classId
        when (classId) {
            StandardClassIds.Byte -> {
                return JavaType.Primitive.Byte
            }
            StandardClassIds.Boolean -> {
                return JavaType.Primitive.Boolean
            }
            StandardClassIds.Char -> {
                return JavaType.Primitive.Char
            }
            StandardClassIds.Double -> {
                return JavaType.Primitive.Double
            }
            StandardClassIds.Float -> {
                return JavaType.Primitive.Float
            }
            StandardClassIds.Int -> {
                return JavaType.Primitive.Int
            }
            StandardClassIds.Long -> {
                return JavaType.Primitive.Long
            }
            StandardClassIds.Short -> {
                return JavaType.Primitive.Short
            }
            StandardClassIds.String -> {
                return JavaType.Primitive.String
            }
            StandardClassIds.Unit -> {
                return JavaType.Primitive.Void
            }
            StandardClassIds.Nothing -> {
                return JavaType.Primitive.Null
            }
            else -> throw IllegalArgumentException("Unsupported primitive type $type")
        }
    }

    private fun primitive(primitiveType: PrimitiveType?): JavaType.Primitive {
        if (primitiveType == null) {
            return JavaType.Primitive.Void
        }
        return when (primitiveType) {
            PrimitiveType.BOOLEAN -> JavaType.Primitive.Boolean
            PrimitiveType.BYTE -> JavaType.Primitive.Byte
            PrimitiveType.CHAR -> JavaType.Primitive.Char
            PrimitiveType.DOUBLE -> JavaType.Primitive.Double
            PrimitiveType.FLOAT -> JavaType.Primitive.Float
            PrimitiveType.INT -> JavaType.Primitive.Int
            PrimitiveType.LONG -> JavaType.Primitive.Long
            PrimitiveType.SHORT -> JavaType.Primitive.Short
            else -> throw IllegalArgumentException("Unsupported primitive type.")
        }
    }

    @OptIn(SymbolInternals::class)
    private fun resolveConeTypeProjection(
        type: ConeTypeProjection,
        signature: String,
        ownerSymbol: FirBasedSymbol<*>?
    ): JavaType? {
        var resolvedType: JavaType? = JavaType.Unknown.getInstance()

        // TODO: fix for multiple bounds.
        val isGeneric = type is ConeKotlinTypeProjectionIn ||
                type is ConeKotlinTypeProjectionOut ||
                type is ConeStarProjection ||
                type is ConeTypeParameterType
        if (isGeneric) {
            var variance: JavaType.GenericTypeVariable.Variance = JavaType.GenericTypeVariable.Variance.INVARIANT
            var bounds: MutableList<JavaType>? = null
            val name: String = when (type) {
                is ConeKotlinTypeProjectionIn, is ConeKotlinTypeProjectionOut -> {
                    ""
                }

                is ConeStarProjection -> {
                    "*"
                }

                else -> {
                    type.toString()
                }
            }
            val gtv = JavaType.GenericTypeVariable(null, name, JavaType.GenericTypeVariable.Variance.INVARIANT, null)
            typeCache.put(signature, gtv)
            if (type is ConeKotlinTypeProjectionIn) {
                variance = JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
                val classSymbol = type.type.toRegularClassSymbol(firSession)
                bounds = ArrayList(1)
                bounds.add(if (classSymbol != null) type(classSymbol.fir) else JavaType.Unknown.getInstance())
            } else if (type is ConeKotlinTypeProjectionOut) {
                variance = JavaType.GenericTypeVariable.Variance.COVARIANT
                val classSymbol = type.type.toRegularClassSymbol(firSession)
                bounds = ArrayList(1)
                bounds.add(if (classSymbol != null) type(classSymbol.fir) else JavaType.Unknown.getInstance())
            }
            gtv.unsafeSet(name, variance, bounds)
            resolvedType = gtv
        } else {
            // The ConeTypeProjection is not a generic type, so it must be a class type.
            if (type is ConeClassLikeType) {
                resolvedType = resolveConeLikeClassType(type, signature, ownerSymbol)
            } else if (type is ConeFlexibleType) {
                resolvedType = type(type.lowerBound)
            }
        }
        return resolvedType
    }

    @OptIn(SymbolInternals::class)
    private fun resolveConeLikeClassType(
        coneClassLikeType: ConeClassLikeType,
        signature: String,
        ownerSymbol: FirBasedSymbol<*>?
    ): JavaType? {
        val classSymbol = coneClassLikeType.toRegularClassSymbol(firSession)
        if (classSymbol == null) {
            typeCache.put(signature, JavaType.Unknown.getInstance())
            return JavaType.Unknown.getInstance()
        }
        return type(classSymbol.fir, ownerSymbol)
    }

    private fun resolveConeTypeProjection(typeParameter: FirTypeParameter, signature: String): JavaType {
        val gtv = JavaType.GenericTypeVariable(
            null,
            typeParameter.name.asString(),
            JavaType.GenericTypeVariable.Variance.INVARIANT,
            null
        )
        typeCache.put(signature, gtv)
        var bounds: MutableList<JavaType>? = null
        var variance: JavaType.GenericTypeVariable.Variance = JavaType.GenericTypeVariable.Variance.INVARIANT
        if (!(typeParameter.bounds.size == 1 && typeParameter.bounds[0] is FirImplicitNullableAnyTypeRef)) {
            bounds = ArrayList(typeParameter.bounds.size)
            for (bound: FirTypeRef in typeParameter.bounds) {
                bounds.add(type(bound))
            }
            if ("out" == typeParameter.variance.label) {
                variance = JavaType.GenericTypeVariable.Variance.COVARIANT
            } else if ("in" == typeParameter.variance.label) {
                variance = JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
            }
        }
        gtv.unsafeSet(gtv.name, variance, bounds)
        return gtv
    }

    private fun convertToFlagsBitMap(status: FirDeclarationStatus): Long {
        var bitMask: Long = 0
        val visibility = status.visibility
        when (visibility.name) {
            "public" -> bitMask += 1L
            "private" -> bitMask += 1L shl 1
            "protected" -> bitMask += 1L shl 2
            "internal" -> {}
            else -> {}
        }
        val modality = status.modality
        if (Modality.FINAL == modality) {
            bitMask += 1L shl 4
        } else if (Modality.ABSTRACT == modality) {
            bitMask += 1L shl 10
        }
        //        else if (Modality.OPEN == modality) {
        // Kotlin specific
//        } else if (Modality.SEALED == modality) {
        // Kotlin specific
//        }
        if (status.isStatic) {
            bitMask += 1L shl 3
        }
        return bitMask
    }

    private fun convertToFlagsBitMap(
        visibility: Visibility,
        isStatic: Boolean,
        isFinal: Boolean,
        isAbstract: Boolean
    ): Long {
        var bitMask: Long = 0
        when (visibility.name) {
            "public" -> bitMask += 1L
            "private" -> bitMask += 1L shl 1
            "protected" -> bitMask += 1L shl 2
            "internal" -> {}
            else -> {}
        }
        if (isStatic) {
            bitMask += 1L shl 3
        }
        if (isFinal) {
            bitMask += 1L shl 4
        }
        if (isAbstract) {
            bitMask += 1L shl 10
        }
        return bitMask
    }

    private fun convertToClassKind(classKind: ClassKind): JavaType.FullyQualified.Kind {
        val kind: JavaType.FullyQualified.Kind = when {
            ClassKind.CLASS == classKind -> {
                JavaType.FullyQualified.Kind.Class
            }
            ClassKind.ANNOTATION_CLASS == classKind -> {
                JavaType.FullyQualified.Kind.Annotation
            }
            ClassKind.ENUM_CLASS == classKind -> {
                JavaType.FullyQualified.Kind.Enum
            }
            ClassKind.INTERFACE == classKind -> {
                JavaType.FullyQualified.Kind.Interface
            }
            ClassKind.OBJECT == classKind -> {
                JavaType.FullyQualified.Kind.Class
            }
            else -> {
                throw IllegalArgumentException("Unsupported classKind: " + classKind.name)
            }
        }
        return kind
    }

    private fun convertToClassKind(clazz: BinaryJavaClass): JavaType.FullyQualified.Kind {
        if (clazz.isEnum) {
            return JavaType.FullyQualified.Kind.Enum
        } else if (clazz.isInterface) {
            return JavaType.FullyQualified.Kind.Interface
        }
        return JavaType.FullyQualified.Kind.Class
    }

    private fun mapJavaElementType(type: JavaElement, signature: String): JavaType {
        if (type is BinaryJavaClass) {
            return classType(type, signature)
        } else if (type is JavaTypeParameter) {
            return mapJavaTypeParameter(type, signature)
        } else if (type is JavaValueParameter) {
            return mapJavaValueParameter(type)
        } else if (type is JavaAnnotation && type.classId != null) {
            val c = type.resolve()
            if (c != null) {
                return type(c)
            }
        }
        return JavaType.Unknown.getInstance()
    }

    private fun mapJavaType(type: org.jetbrains.kotlin.load.java.structure.JavaType, signature: String): JavaType {
        when (type) {
            is JavaPrimitiveType -> {
                return primitive(type.type)
            }

            is JavaClassifierType -> {
                return mapClassifierType(type, signature)
            }

            is JavaArrayType -> {
                return array(type, signature)
            }

            is JavaWildcardType -> {
                return mapWildcardType(type, signature)
            }

            else -> return JavaType.Unknown.getInstance()
        }
    }

    private fun mapClassifierType(type: JavaClassifierType, signature: String): JavaType {
        val javaType = type(type.classifier)
        if (!type.typeArguments.isEmpty()) {
            var fq = TypeUtils.asFullyQualified(javaType)
            fq = if (fq is JavaType.Parameterized) fq.type else fq
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                val typeParameters: MutableList<JavaType> = ArrayList(type.typeArguments.size)
                for (typeArgument: org.jetbrains.kotlin.load.java.structure.JavaType? in type.typeArguments) {
                    typeParameters.add(type(typeArgument))
                }
                pt.unsafeSet(fq, typeParameters)
            }
            return pt
        }
        return javaType
    }

    private fun mapJavaTypeParameter(type: JavaTypeParameter, signature: String): JavaType {
        val name = type.name.asString()
        val gtv = JavaType.GenericTypeVariable(
            null,
            name, JavaType.GenericTypeVariable.Variance.INVARIANT, null
        )
        typeCache.put(signature, gtv)
        var bounds: List<JavaType>? = null
        if (type.upperBounds.size == 1) {
            val mappedBound = type(type.upperBounds.toTypedArray()[0])
            if (mappedBound !is JavaType.FullyQualified || "java.lang.Object" != mappedBound.fullyQualifiedName) {
                bounds = listOf(mappedBound)
            }
        } else {
            bounds = ArrayList(type.upperBounds.size)
            for (bound: org.jetbrains.kotlin.load.java.structure.JavaType in type.upperBounds) {
                bounds.add(type(bound))
            }
        }
        gtv.unsafeSet(
            gtv.name,
            if (bounds == null) JavaType.GenericTypeVariable.Variance.INVARIANT else JavaType.GenericTypeVariable.Variance.COVARIANT,
            bounds
        )
        return gtv
    }

    private fun mapJavaValueParameter(type: JavaValueParameter): JavaType {
        return type(type.type)
    }

    private fun mapWildcardType(wildcardType: JavaWildcardType, signature: String): JavaType {
        val gtv = JavaType.GenericTypeVariable(null, "?", JavaType.GenericTypeVariable.Variance.INVARIANT, null)
        typeCache.put(signature, gtv)
        val variance: JavaType.GenericTypeVariable.Variance
        var bounds: List<JavaType>?
        if (wildcardType.bound != null) {
            variance = if (wildcardType.isExtends) {
                JavaType.GenericTypeVariable.Variance.COVARIANT
            } else {
                JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
            }
            bounds = listOf(type(wildcardType.bound))
        } else {
            variance = JavaType.GenericTypeVariable.Variance.INVARIANT
            bounds = null
        }
        if (bounds != null && bounds[0] is JavaType.FullyQualified && "java.lang.Object" == (bounds[0] as JavaType.FullyQualified)
                .fullyQualifiedName
        ) {
            bounds = null
        }
        gtv.unsafeSet(gtv.name, variance, bounds)
        return gtv
    }

    private fun listAnnotations(firAnnotations: List<FirAnnotation>): List<JavaType.FullyQualified> {
        val annotations: MutableList<JavaType.FullyQualified> = ArrayList(firAnnotations.size)
        for (firAnnotation: FirAnnotation in firAnnotations) {
            val symbol = firAnnotation.typeRef.coneType.toRegularClassSymbol(firSession)
            if (skipAnnotation(symbol)) {
                continue
            }
            val fq = TypeUtils.asFullyQualified(type(firAnnotation.typeRef))
            if (fq != null) {
                annotations.add(fq)
            }
        }
        return annotations
    }

    private fun listAnnotations(javaAnnotations: Collection<JavaAnnotation>): List<JavaType.FullyQualified> {
        val annotations: MutableList<JavaType.FullyQualified> = ArrayList(javaAnnotations.size)
        for (javaAnnotation: JavaAnnotation in javaAnnotations) {
            val fq = TypeUtils.asFullyQualified(type(javaAnnotation))
            if (fq != null) {
                annotations.add(type(javaAnnotation) as JavaType.FullyQualified)
            }
        }
        return annotations
    }

    private fun skipAnnotation(symbol: FirClassLikeSymbol<*>?): Boolean {
        if (symbol != null) {
            for (annotation: FirAnnotation in symbol.annotations) {
                if (annotation is FirAnnotationCall && !annotation.argumentList.arguments.isEmpty()) {
                    for (argument: FirExpression in annotation.argumentList.arguments) {
                        if (argument is FirPropertyAccessExpression) {
                            val callRefSymbol = (argument.calleeReference as FirResolvedNamedReference).resolvedSymbol
                            if (callRefSymbol is FirEnumEntrySymbol) {
                                if (("kotlin.annotation.AnnotationRetention\$SOURCE" == convertKotlinFqToJavaFq(
                                        callRefSymbol.callableId.toString()
                                    ))
                                ) {
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        }
        return false
    }
}
