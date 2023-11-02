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
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
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
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.openrewrite.java.JavaTypeMapping
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.GenericTypeVariable
import org.openrewrite.java.tree.JavaType.ShallowClass
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.convertClassIdToFqn
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.convertKotlinFqToJavaFq
import kotlin.collections.ArrayList

class KotlinTypeMapping(
    private val typeCache: JavaTypeCache,
    private val firSession: FirSession,
    private val firFile: FirFile
) : JavaTypeMapping<Any> {

    private val signatureBuilder: KotlinTypeSignatureBuilder = KotlinTypeSignatureBuilder(firSession, firFile)

    override fun type(type: Any?): JavaType {
        if (type == null || type is FirErrorTypeRef || type is FirExpression && type.typeRef is FirErrorTypeRef) {
            return JavaType.Unknown.getInstance()
        }

        val signature = signatureBuilder.signature(type)
        val existing: JavaType? = typeCache.get(signature)
        if (existing != null) {
            return existing
        }

        return type(type, firFile, signature) ?: JavaType.Unknown.getInstance()
    }

    fun type(type: Any?, parent: Any?): JavaType? {
        if (type == null || type is FirErrorTypeRef || type is FirExpression && type.typeRef is FirErrorTypeRef) {
            return JavaType.Unknown.getInstance()
        }
        val signature = signatureBuilder.signature(type, parent)
        val existing = typeCache.get<JavaType>(signature)
        if (existing != null) {
            return existing
        }
        return type(type, parent, signature)
    }


    @OptIn(SymbolInternals::class)
    fun type(type: Any?, parent: Any?, signature: String): JavaType? {
        return when (type) {
            is ConeClassLikeType, is FirClass, is FirResolvedQualifier -> {
                classType(type, parent, signature)
            }

            is ConeFlexibleType -> {
                type(type.lowerBound, signature)
            }

            is ConeTypeProjection -> {
                coneTypeProjectionType(type, signature)
            }

            is FirAnonymousFunctionExpression -> {
                type(type.anonymousFunction, parent, signature)
            }

            is FirBlock -> {
                // There is an issue in the KotlinTreeParserVisitor, PsiElementVisitor,
                // or no FIR element associated to the Kt that requested a type.
                // Example: AssignmentOperationTest#augmentedAssignmentAnnotation
                JavaType.Unknown.getInstance()
            }

            is FirErrorNamedReference -> {
                JavaType.Unknown.getInstance()
            }

            is FirFile -> {
                fileType(signature)
            }

            is FirFunction -> {
                methodDeclarationType(type, parent, signature)
            }

            is FirFunctionCall -> {
                methodInvocationType(type, signature)
            }

            is FirJavaTypeRef -> {
                type(type.type, parent, signature)
            }

            is FirOuterClassTypeParameterRef -> {
                type(type.symbol.fir, parent, signature)
            }

            is FirPackageDirective -> {
                packageDirective(signature)
            }

            is FirResolvedNamedReference -> {
                resolvedNameReferenceType(type, parent, signature)
            }

            is FirResolvedTypeRef -> {
                type(type.coneType, parent, signature)
            }

            is FirSafeCallExpression -> {
                type(type.selector, parent, signature)
            }

            is FirTypeParameter -> {
                typeParameterType(type, signature)
            }

            is FirVariable -> {
                variableType(type, parent, signature)
            }

            is FirVariableAssignment -> {
                type(type.lValue.typeRef, parent, signature)
            }

            is FirExpression -> {
                type(type.typeRef, parent, signature)
            }

            is JavaElement -> {
                javaElement(type, signature)
            }

            else -> {
                JavaType.Unknown.getInstance()
            }
        }
    }

    private fun packageDirective(signature: String): JavaType? {
        val jt = ShallowClass.build(signature)
        typeCache.put(signature, jt)
        return jt
    }

    private fun fileType(signature: String): JavaType {
        val fileType = ShallowClass.build(signature)
        typeCache.put(signature, fileType)
        return fileType
    }

    private fun coneTypeProjectionType(type: ConeTypeProjection, signature: String): JavaType {
        var variance: GenericTypeVariable.Variance = JavaType.GenericTypeVariable.Variance.INVARIANT
        var bounds: MutableList<JavaType>? = null
        val name: String = when (type) {
            is ConeKotlinTypeProjectionIn, is ConeKotlinTypeProjectionOut, is ConeStarProjection, is ConeCapturedType -> {
                "?"
            }

            is ConeIntersectionType -> {
                ""
            }

            else -> {
                type.toString()
            }
        }
        val gtv = GenericTypeVariable(null, name, JavaType.GenericTypeVariable.Variance.INVARIANT, null)
        typeCache.put(signature, gtv)
        if (type is ConeKotlinTypeProjectionIn) {
            variance = JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
            bounds = ArrayList(1)
            bounds.add(type(type.type))
        } else if (type is ConeKotlinTypeProjectionOut) {
            variance = JavaType.GenericTypeVariable.Variance.COVARIANT
            bounds = ArrayList(1)
            bounds.add(type(type.type))
        } else if (type is ConeTypeParameterType) {
            val classifierSymbol: FirClassifierSymbol<*>? = type.lookupTag.toSymbol(firSession)
            if (classifierSymbol is FirTypeParameterSymbol) {
                for (bound: FirResolvedTypeRef in classifierSymbol.resolvedBounds) {
                    if (bound !is FirImplicitNullableAnyTypeRef) {
                        if (bounds == null) {
                            bounds = ArrayList()
                        }
                        bounds.add(type(bound))
                    }
                }
                variance = when {
                    classifierSymbol.variance == Variance.INVARIANT -> {
                        if (bounds == null) JavaType.GenericTypeVariable.Variance.INVARIANT else JavaType.GenericTypeVariable.Variance.COVARIANT
                    }

                    classifierSymbol.variance == Variance.IN_VARIANCE && bounds != null -> {
                        JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
                    }

                    classifierSymbol.variance == Variance.OUT_VARIANCE && bounds != null -> {
                        JavaType.GenericTypeVariable.Variance.COVARIANT
                    }

                    else -> GenericTypeVariable.Variance.INVARIANT
                }
            }
        } else if (type is ConeIntersectionType) {
            bounds = ArrayList(type.intersectedTypes.size)
            for (t: ConeTypeProjection in type.intersectedTypes) {
                bounds.add(type(t))
            }
        }
        gtv.unsafeSet(name, variance, bounds)
        return gtv
    }

    @OptIn(SymbolInternals::class)
    private fun classType(type: Any, parent: Any?, signature: String): JavaType.FullyQualified {
        val fqn = signatureBuilder.classSignature(type)
        val fq: JavaType.FullyQualified? = typeCache.get(fqn)
        var params: List<*>? = null
        val firClass = when (type) {
            is FirClass -> type
            is FirResolvedQualifier -> {
                val ref = type.typeRef.toRegularClassSymbol(firSession)
                if (type.typeArguments.isNotEmpty()) {
                    params = type.typeArguments
                }
                if (ref == null) {
                    typeCache.put(signature, JavaType.Unknown.getInstance())
                    return JavaType.Unknown.getInstance()
                }
                ref.fir
            }

            is ConeClassLikeType -> {
                if (type.toSymbol(firSession) is FirTypeAliasSymbol) {
                    return classType((type.toSymbol(firSession) as FirTypeAliasSymbol).resolvedExpandedTypeRef.type, parent, signature)
                }
                val ref = type.toRegularClassSymbol(firSession)
                if (type.typeArguments.isNotEmpty()) {
                    params = type.typeArguments.toList()
                }
                if (ref == null) {
                    typeCache.put(signature, JavaType.Unknown.getInstance())
                    return JavaType.Unknown.getInstance()
                }
                ref.fir
            }

            else -> throw UnsupportedOperationException("Unexpected classType: ${type.javaClass}")
        }
        var clazz: JavaType.Class? = (if (fq is JavaType.Parameterized) fq.type else fq) as JavaType.Class?
        if (clazz == null) {
            clazz = JavaType.Class(
                null,
                mapToFlagsBitmap(firClass.visibility, firClass.modality()),
                fqn,
                mapKind(firClass.classKind),
                null, null, null, null, null, null, null
            )

            typeCache.put(fqn, clazz)

            var superTypeRef: FirTypeRef? = null
            var interfaceTypeRefs: MutableList<FirTypeRef>? = null
            for (t in firClass.superTypeRefs) {
                val sym = t.coneType.toRegularClassSymbol(firSession)
                when (sym?.fir?.classKind) {
                    ClassKind.CLASS -> superTypeRef = t
                    ClassKind.INTERFACE -> {
                        if (interfaceTypeRefs == null) {
                            interfaceTypeRefs = ArrayList()
                        }
                        interfaceTypeRefs.add(t)
                    }

                    else -> {}
                }
            }
            val supertype =
                if (superTypeRef == null || "java.lang.Object" == signature) null else TypeUtils.asFullyQualified(
                    type(superTypeRef)
                )
            var declaringType: JavaType.FullyQualified? = null
            if (!firClass.isLocal && firClass.symbol.classId.isNestedClass) {
                val parentSymbol = firClass.symbol.classId.outerClassId!!.toSymbol(firSession)
                if (parentSymbol != null) {
                    declaringType = TypeUtils.asFullyQualified(type(parentSymbol.fir))
                }
            } else if (firClass.symbol.classId.isNestedClass) {
                declaringType = TypeUtils.asFullyQualified(type(parent))
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
                } else if (declaration is FirAnonymousInitializer) {
                    // TODO: MethodInvocationTest#anonymousLambdaInSuperConstructorCall
                } else if (declaration is FirField) {
                    // TODO: ClassDeclarationTest#explicitDelegation
                } else if (declaration !is FirRegularClass) {
                    TODO()
                }
            }

            var fields: MutableList<JavaType.Variable>? = null
            if (enumEntries.isNotEmpty()) {
                fields = ArrayList(properties.size + enumEntries.size)
                for (enumEntry: FirEnumEntry in enumEntries) {
                    fields.add(variableType(enumEntry, firClass))
                }
            }
            if (properties.isNotEmpty()) {
                if (fields == null) {
                    fields = ArrayList(properties.size)
                }
                for (property: FirProperty in properties) {
                    fields.add(variableType(property, firClass))
                }
            }
            if (javaFields.isNotEmpty()) {
                if (fields == null) {
                    fields = ArrayList(javaFields.size)
                }
                for (field: FirJavaField in javaFields) {
                    fields.add(variableType(field, firClass))
                }
            }
            var methods: MutableList<JavaType.Method>? = null
            if (functions.isNotEmpty()) {
                methods = ArrayList(functions.size)
                for (function: FirFunction in functions) {
                    val mt = methodDeclarationType(function, firClass)
                    methods.add(mt)
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
            var typeParameters: MutableList<JavaType>? = null
            if (firClass.typeParameters.isNotEmpty()) {
                typeParameters = ArrayList(firClass.typeParameters.size)
                for (tParam in firClass.typeParameters) {
                    typeParameters.add(type(tParam))
                }
            }
            clazz.unsafeSet(
                typeParameters,
                supertype,
                declaringType,
                listAnnotations(firClass.annotations),
                interfaces,
                fields,
                methods
            )
        }

        // The signature for a ConeClassLikeType may be aliases without type parameters.
        if (firClass.typeParameters.isNotEmpty() && signature.contains("<")) {
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                val typeParameters: MutableList<JavaType> = ArrayList(firClass.typeParameters.size)
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                if (params == null) {
                    params = firClass.typeParameters
                }
                for (tp in params) {
                    typeParameters.add(type(tp))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    fun methodDeclarationType(function: FirFunction, parent: Any?): JavaType.Method {
        val signature = signatureBuilder.methodSignature(function, parent)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }
        return methodDeclarationType(function, parent, signature)
    }

    @OptIn(SymbolInternals::class)
    private fun methodDeclarationType(function: FirFunction, parent: Any?, signature: String): JavaType.Method {
        var paramNames: MutableList<String>? = null
        if (function.valueParameters.isNotEmpty()) {
            paramNames = ArrayList(function.valueParameters.size)
            for (p in function.valueParameters) {
                paramNames.add(p.name.asString())
            }
        }
        val method = JavaType.Method(
            null,
            mapToFlagsBitmap(function.visibility, function.modality),
            null,
            if (function.symbol is FirConstructorSymbol) "<constructor>" else function.symbol.name.asString(),
            null,
            paramNames,
            null, null, null,
            null
        )
        typeCache.put(signature, method)
        var parentType = when {
            function.symbol is FirConstructorSymbol -> type(function.returnTypeRef)
            function.dispatchReceiverType != null -> type(function.dispatchReceiverType!!)
            function.symbol.getOwnerLookupTag()?.toFirRegularClass(firSession) != null -> {
                type(function.symbol.getOwnerLookupTag()!!.toFirRegularClass(firSession)!!)
            }
            parent is FirRegularClass || parent != null -> type(parent)
            else -> type(firFile)
        }
        if (parentType is JavaType.Method) {
            parentType = parentType.declaringType
        }
        if (parentType is JavaType.Parameterized) {
            parentType = parentType.type
        }
        val resolvedDeclaringType = TypeUtils.asFullyQualified(parentType)
        val returnType = type(function.returnTypeRef)
        val parameterTypes: MutableList<JavaType>? = when {
            function.receiverParameter != null || function.valueParameters.isNotEmpty() -> {
                ArrayList(function.valueParameters.size + (if (function.receiverParameter != null) 1 else 0))
            }
            else -> null
        }
        if (function.receiverParameter != null) {
            parameterTypes!!.add(type(function.receiverParameter!!.typeRef))
        }
        if (function.valueParameters.isNotEmpty()) {
            for (p in function.valueParameters) {
                val t = type(p.returnTypeRef, function)
                if (t != null) {
                    parameterTypes!!.add(t)
                }
            }
        }
        method.unsafeSet(
            resolvedDeclaringType,
            returnType,
            parameterTypes, null, listAnnotations(function.annotations)
        )
        return method
    }

    private fun methodDeclarationType(
        javaMethod: JavaMethod,
        declaringType: JavaType.FullyQualified?
    ): JavaType.Method? {
        val signature = signatureBuilder.javaMethodSignature(javaMethod)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }
        return methodDeclarationType(javaMethod, declaringType, signature)
    }

    private fun methodDeclarationType(
        javaMethod: JavaMethod,
        declaringType: JavaType.FullyQualified?,
        signature: String
    ): JavaType.Method? {
        var paramNames: MutableList<String>? = null
        if (javaMethod.valueParameters.isNotEmpty()) {
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

    fun methodInvocationType(fir: FirFunctionCall): JavaType.Method? {
        if (fir.typeRef is FirErrorTypeRef) {
            return null
        }
        val signature = signatureBuilder.methodCallSignature(fir)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }
        return methodInvocationType(fir, signature)
    }

    @OptIn(SymbolInternals::class)
    fun methodInvocationType(function: FirFunctionCall, signature: String): JavaType.Method? {
        val sym = function.calleeReference.toResolvedBaseSymbol() ?: return null
        val receiver = if (sym is FirFunctionSymbol<*>) sym.receiverParameter else null
        var paramNames: MutableList<String>? = when {
            sym is FirFunctionSymbol<*> && receiver != null ||
            function.arguments.isNotEmpty() -> {
                ArrayList(function.arguments.size + (if (receiver != null) 1 else 0))
            }
            else -> null
        }
        val paramTypes: MutableList<JavaType>? = if (paramNames != null) ArrayList(paramNames.size) else null
        if (receiver != null) {
            paramNames!!.add('$'+ "this" + '$')
        }
        if (function.arguments.isNotEmpty()) {
            when (sym) {
                is FirFunctionSymbol<*> -> {
                    for (p in sym.valueParameterSymbols) {
                        paramNames!!.add(p.name.asString())
                    }
                }
            }
        }
        val method = JavaType.Method(
            null,
            when (sym) {
                is FirConstructorSymbol -> mapToFlagsBitmap(sym.visibility, sym.modality)
                is FirNamedFunctionSymbol -> mapToFlagsBitmap(sym.visibility, sym.modality)
                else -> {
                    0 // TODO REMOVE.
                }
            },
            null,
            when (sym) {
                is FirConstructorSymbol -> "<constructor>"
                is FirNamedFunctionSymbol -> sym.name.asString()
                else -> {
                    ""
                }
            },
            null,
            paramNames,
            null, null, null,
            null
        )
        typeCache.put(signature, method)
        var declaringType: JavaType.FullyQualified? = null
        if (function.calleeReference is FirResolvedNamedReference &&
            (function.calleeReference as FirResolvedNamedReference).resolvedSymbol is FirNamedFunctionSymbol
        ) {
            val resolvedSymbol =
                (function.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirNamedFunctionSymbol
            if (resolvedSymbol.dispatchReceiverType is ConeClassLikeType) {
                declaringType = TypeUtils.asFullyQualified(type(resolvedSymbol.dispatchReceiverType))
            } else if (resolvedSymbol.containingClassLookupTag() != null &&
                resolvedSymbol.containingClassLookupTag()!!.toFirRegularClass(firSession) != null
            ) {
                declaringType = TypeUtils.asFullyQualified(
                    type(
                        resolvedSymbol.containingClassLookupTag()!!.toFirRegularClass(firSession)
                    )
                )
            } else if (resolvedSymbol.origin == FirDeclarationOrigin.Library) {
                if (resolvedSymbol.fir.containerSource is JvmPackagePartSource) {
                    val source: JvmPackagePartSource? = resolvedSymbol.fir.containerSource as JvmPackagePartSource?
                    if (source != null) {
                        declaringType = if (source.facadeClassName != null) {
                            createShallowClass(convertKotlinFqToJavaFq(source.facadeClassName.toString()))
                        } else {
                            createShallowClass(convertKotlinFqToJavaFq(source.className.toString()))
                        }
                    }
                }
            } else if (!resolvedSymbol.fir.origin.generated &&
                !resolvedSymbol.fir.origin.fromSupertypes &&
                !resolvedSymbol.fir.origin.fromSource) {
                declaringType = createShallowClass("kotlin.Library")
            }
        } else if (sym is FirFunctionSymbol<*>) {
            declaringType = TypeUtils.asFullyQualified(type(function.typeRef))
        }
        if (declaringType == null) {
            declaringType = TypeUtils.asFullyQualified(type(firFile))
        }
        val returnType = type(function.typeRef)

        if (function.toResolvedCallableSymbol()?.receiverParameter != null) {
            paramTypes!!.add(type(function.toResolvedCallableSymbol()?.receiverParameter!!.typeRef))
        }
        for (param: FirExpression? in function.arguments) {
            if (param != null) {
                paramTypes!!.add(type(param.typeRef))
            }
        }
        method.unsafeSet(
            declaringType,
            returnType,
            paramTypes, null, listAnnotations(function.annotations)
        )
        return method
    }

    private fun createShallowClass(name: String): JavaType.FullyQualified {
        val c = ShallowClass.build(name)
        typeCache.put(name, c)
        return c
    }

    @OptIn(SymbolInternals::class)
    private fun resolvedNameReferenceType(type: FirResolvedNamedReference, parent: Any?, signature: String): JavaType? {
        return when (val sym = type.resolvedSymbol) {
            is FirBackingFieldSymbol -> type(sym.fir, parent, signature)
            is FirConstructorSymbol -> type(sym.fir, parent, signature)
            is FirEnumEntrySymbol -> type(sym.fir, parent, signature)
            is FirFieldSymbol -> type(sym.fir, parent, signature)
            is FirNamedFunctionSymbol -> type(sym.fir, parent, signature)
            is FirPropertySymbol -> type(sym.fir, parent, signature)
            is FirValueParameterSymbol -> type(sym.fir, parent, signature)
            else -> {
                null
            }
        }
    }

    private fun typeParameterType(type: FirTypeParameter, signature: String): JavaType {
        val gtv = GenericTypeVariable(
            null,
            type.name.asString(),
            JavaType.GenericTypeVariable.Variance.INVARIANT,
            null
        )
        typeCache.put(signature, gtv)
        var bounds: MutableList<JavaType>? = null
        var variance: GenericTypeVariable.Variance = JavaType.GenericTypeVariable.Variance.INVARIANT
        if (!(type.bounds.size == 1 && type.bounds[0] is FirImplicitNullableAnyTypeRef)) {
            bounds = ArrayList(type.bounds.size)
            for (bound: FirTypeRef in type.bounds) {
                bounds.add(type(bound))
            }
            if (type.variance == Variance.IN_VARIANCE) {
                variance = JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
            } else if (bounds.isNotEmpty()) {
                variance = JavaType.GenericTypeVariable.Variance.COVARIANT
            }
        }
        gtv.unsafeSet(gtv.name, variance, bounds)
        return gtv
    }

    fun variableType(variable: FirVariable, parent: Any?): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(variable, parent)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        return variableType(variable, parent, signature)
    }

    @OptIn(SymbolInternals::class)
    fun variableType(variable: FirVariable, parent: Any?, signature: String): JavaType.Variable {
        val vt = JavaType.Variable(
            null,
            mapToFlagsBitmap(variable.visibility, variable.modality),
            variable.name.asString(),
            null, null, null
        )
        typeCache.put(signature, vt)
        val annotations = listAnnotations(variable.annotations)
        var declaringType: JavaType? = null
        when {
            variable.symbol.dispatchReceiverType != null -> {
                declaringType = type(variable.symbol.dispatchReceiverType)
            }
            variable.symbol.getContainingClassSymbol(firSession) != null -> {
                if (variable.symbol.getContainingClassSymbol(firSession) !is FirAnonymousObjectSymbol) {
                    declaringType = type(variable.symbol.getContainingClassSymbol(firSession)!!.fir)
                }
            }
            parent is FirClass -> {
                declaringType = type(parent)
            }
            parent is FirFunction -> {
                declaringType = methodDeclarationType(parent, null)
            }
            else -> declaringType = TypeUtils.asFullyQualified(type(firFile))
        }

        if (declaringType is JavaType.Parameterized) {
            declaringType = declaringType.type
        }

        val typeRef = type(variable.returnTypeRef)
        vt.unsafeSet(declaringType!!, typeRef, annotations)
        return vt
    }

    @OptIn(SymbolInternals::class)
    private fun javaElement(type: JavaElement, signature: String): JavaType? {
        return when (type) {
            is JavaArrayType -> javaArrayType(type, signature)
            is JavaPrimitiveType -> javaPrimitiveType(type)
            is JavaClassifierType -> javaClassType(type, signature)
            is BinaryJavaAnnotation -> type(type.classId.toSymbol(firSession)?.fir, signature)
            is BinaryJavaClass -> javaClassType(type, signature)
            is BinaryJavaTypeParameter -> javaTypeParameter(type, signature)
            is JavaWildcardType -> javaWildCardType(type, signature)
            else -> null
        }
    }

    private fun javaArrayType(type: JavaArrayType, signature: String): JavaType {
        val arrayType = JavaType.Array(
            null,
            null
        )
        typeCache.put(signature, arrayType)
        val classType = type(type.componentType)
        arrayType.unsafeSet(classType)
        return arrayType
    }

    private fun javaClassType(type: JavaClassifier, signature: String): JavaType {
        if (type !is BinaryJavaClass) {
            TODO()
        }
        val fqn = type.fqName.asString()
        var clazz = typeCache.get<JavaType.Class>(fqn)
        if (clazz == null) {
            clazz = JavaType.Class(
                null,
                type.access.toLong(),
                type.fqName.asString(),
                when {
                    type.isAnnotationType -> JavaType.FullyQualified.Kind.Annotation
                    type.isEnum -> JavaType.FullyQualified.Kind.Enum
                    type.isInterface -> JavaType.FullyQualified.Kind.Interface
                    type.isRecord -> JavaType.FullyQualified.Kind.Record
                    else -> JavaType.FullyQualified.Kind.Class
                },
                null, null, null, null, null, null, null
            )
            typeCache.put(fqn, clazz)
            var supertype: JavaType.FullyQualified? = null
            var interfaces: MutableList<JavaType.FullyQualified>? = null
            for (classifierSupertype: JavaClassifierType in type.supertypes) {
                if (classifierSupertype.classifier is JavaClass) {
                    if ((classifierSupertype.classifier as JavaClass?)!!.isInterface) {
                        if (interfaces == null) {
                            interfaces = ArrayList()
                        }
                        interfaces.add(type(classifierSupertype) as JavaType.FullyQualified)
                    } else if ("java.lang.Object" != fqn) {
                        supertype = type(classifierSupertype) as JavaType.FullyQualified
                    }
                } else {
                    TODO()
                }
            }
            var owner: JavaType.FullyQualified? = null
            if (type.outerClass != null) {
                owner = TypeUtils.asFullyQualified(type(type.outerClass))
            }
            var fields: MutableList<JavaType.Variable>? = null
            if (type.fields.isNotEmpty()) {
                fields = ArrayList(type.fields.size)
                for (field: JavaField in type.fields) {
                    fields.add(javaVariableType(field, clazz))
                }
            }
            var methods: MutableList<JavaType.Method>? = null
            if (type.methods.isNotEmpty()) {
                methods = ArrayList(type.methods.size)
                for (method: JavaMethod in type.methods) {
                    val mt = methodDeclarationType(method, clazz)
                    if (mt != null) {
                        methods.add(mt)
                    }
                }
            }
            if (type.constructors.isNotEmpty()) {
                for (method: JavaConstructor in type.constructors) {
                    if (method is BinaryJavaConstructor) {
                        if (methods == null) {
                            methods = ArrayList()
                        }
                        // Filter out the same methods as JavaTypeMapping: Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL | Flags.ANONCONSTR
                        if (method.access.toLong() and ((1 shl 12).toLong() or (1L shl 31) or (1L shl 37) or (1 shl 29).toLong()) == 0L) {
                            val ms = javaConstructorType(method, clazz)
                            if (ms != null) {
                                methods.add(ms)
                            }
                        }
                    }
                }
            }
            var typeParameters: MutableList<JavaType>? = null
            if (type.typeParameters.isNotEmpty()) {
                typeParameters = ArrayList(type.typeParameters.size)
                for (typeArgument: JavaTypeParameter in type.typeParameters) {
                    typeParameters.add(type(typeArgument))
                }
            }
            clazz.unsafeSet(
                typeParameters,
                supertype,
                owner,
                listAnnotations(type.annotations),
                interfaces,
                fields,
                methods
            )
        }
        if (type.typeParameters.isNotEmpty()) {
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                val typeParameters: MutableList<JavaType> = ArrayList(type.typeParameters.size)
                for (typeArgument: JavaTypeParameter in type.typeParameters) {
                    typeParameters.add(type(typeArgument))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    private fun javaClassType(type: JavaClassifierType, signature: String): JavaType? {
        if (type.classifier == null) {
            TODO()
        }
        var clazz = TypeUtils.asFullyQualified(type(type.classifier!!))
        if (type.typeArguments.isNotEmpty()) {
            val ptSig = signatureBuilder.signature(type)
            var pt = typeCache.get<JavaType.Parameterized>(ptSig)
            if (pt == null) {
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                val typeParameters: MutableList<JavaType> = ArrayList(type.typeArguments.size)
                for (typeArgument: org.jetbrains.kotlin.load.java.structure.JavaType? in type.typeArguments) {
                    typeParameters.add(type(typeArgument))
                }
                if (clazz is JavaType.Parameterized) {
                    clazz = clazz.type
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    private fun javaConstructorType(
        constructor: JavaConstructor,
        declaringType: JavaType.FullyQualified?
    ): JavaType.Method? {
        val signature = signatureBuilder.javaConstructorSignature(constructor)
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

    private fun javaPrimitiveType(type: JavaPrimitiveType): JavaType {
        return when (type.type) {
            PrimitiveType.BOOLEAN -> JavaType.Primitive.Boolean
            PrimitiveType.BYTE -> JavaType.Primitive.Byte
            PrimitiveType.CHAR -> JavaType.Primitive.Char
            PrimitiveType.DOUBLE -> JavaType.Primitive.Double
            PrimitiveType.FLOAT -> JavaType.Primitive.Float
            PrimitiveType.INT -> JavaType.Primitive.Int
            PrimitiveType.LONG -> JavaType.Primitive.Long
            PrimitiveType.SHORT -> JavaType.Primitive.Short
            null -> JavaType.Primitive.Null
        }
    }

    private fun javaTypeParameter(type: JavaTypeParameter, signature: String): JavaType {
        val name = type.name.asString()

        val gtv = GenericTypeVariable(
            null,
            name, GenericTypeVariable.Variance.INVARIANT, null
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

    private fun javaWildCardType(type: JavaWildcardType, signature: String): JavaType {
        val name = "?"
        var variance = GenericTypeVariable.Variance.INVARIANT
        val gtv = GenericTypeVariable(null, name, variance, null)
        typeCache.put(signature, gtv)
        var bounds: MutableList<JavaType>? = null
        if (type.bound != null) {
            variance = if (type.isExtends) {
                GenericTypeVariable.Variance.COVARIANT
            } else {
                GenericTypeVariable.Variance.CONTRAVARIANT
            }
            bounds = ArrayList(1)
            bounds.add(type(type.bound))
        }
        gtv.unsafeSet(name, variance, bounds)
        return gtv
    }

    private fun javaVariableType(javaField: JavaField, owner: JavaType?): JavaType.Variable {
        val signature = signatureBuilder.javaVariableSignature(javaField)
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

    @OptIn(SymbolInternals::class)
    private fun listAnnotations(firAnnotations: List<FirAnnotation>): MutableList<JavaType.FullyQualified>? {
        var annotations: MutableList<JavaType.FullyQualified>? = null
        for (firAnnotation in firAnnotations) {
            val fir = firAnnotation.typeRef.toRegularClassSymbol(firSession)?.fir
            if (fir != null && isNotSourceRetention(fir.annotations)) {
                if (annotations == null) {
                    annotations = ArrayList()
                }
                val fq = TypeUtils.asFullyQualified(type(firAnnotation))
                if (fq != null) {
                    annotations.add(fq)
                }
            }
        }
        return annotations
    }

    @OptIn(SymbolInternals::class)
    private fun listAnnotations(javaAnnotations: Collection<JavaAnnotation>): List<JavaType.FullyQualified>? {
        var annotations: MutableList<JavaType.FullyQualified>? = null
        for (javaAnnotation: JavaAnnotation in javaAnnotations) {
            val fir = javaAnnotation.classId?.toSymbol(firSession)?.fir
            if (fir != null && isNotSourceRetention(fir.annotations)) {
                if (annotations == null) {
                    annotations = ArrayList()
                }
                val fq = TypeUtils.asFullyQualified(type(javaAnnotation))
                if (fq != null) {
                    annotations.add(fq)
                }
            }
        }
        return annotations
    }

    private fun isNotSourceRetention(annotations: List<FirAnnotation>): Boolean {
        for (ann in annotations) {
            if ("kotlin.annotation.Retention" == convertClassIdToFqn(ann.typeRef.coneType.classId)) {
                for (v in ann.argumentMapping.mapping.values) {
                    if (v.calleeReference is FirResolvedNamedReference && (v.calleeReference as FirResolvedNamedReference).name.asString() == "SOURCE") {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun mapKind(kind: ClassKind): JavaType.FullyQualified.Kind {
        return when (kind) {
            ClassKind.INTERFACE -> JavaType.FullyQualified.Kind.Interface
            ClassKind.ENUM_CLASS -> JavaType.FullyQualified.Kind.Enum
            // ClassKind.ENUM_ENTRY is compiled to a class.
            ClassKind.ENUM_ENTRY -> JavaType.FullyQualified.Kind.Class
            ClassKind.ANNOTATION_CLASS -> JavaType.FullyQualified.Kind.Annotation
            else -> JavaType.FullyQualified.Kind.Class
        }
    }

    private fun mapToFlagsBitmap(visibility: Visibility, modality: Modality?): Long {
        var bitMask: Long = 0
        when (visibility.externalDisplayName.lowercase()) {
            "public" -> bitMask += 1L
            "private" -> bitMask += 1L shl 1
            "protected" -> bitMask += 1L shl 2
            "internal", "package-private", "local" -> {}
            else -> throw UnsupportedOperationException("Unsupported visibility: ${visibility.name.lowercase()}")
        }
        if (modality != null) {
            bitMask += when (modality.name.lowercase()) {
                "final" -> 1L shl 4
                "abstract" -> 1L shl 10
                "sealed" -> 1L shl 62
                "open" -> 0
                else -> throw UnsupportedOperationException("Unsupported modality: ${modality.name.lowercase()}")
            }
        }
        return bitMask
    }

    private fun convertToFlagsBitMap(visibility: Visibility, isStatic: Boolean, isFinal: Boolean, isAbstract: Boolean): Long {
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

    fun primitive(type: FirElement): JavaType.Primitive {
        return when (type) {
            is FirConstExpression<*> -> {
                when (type.kind) {
                    ConstantValueKind.Boolean -> JavaType.Primitive.Boolean
                    ConstantValueKind.Byte, ConstantValueKind.UnsignedByte -> JavaType.Primitive.Byte
                    ConstantValueKind.Char -> JavaType.Primitive.Char
                    ConstantValueKind.Double -> JavaType.Primitive.Double
                    ConstantValueKind.Float -> JavaType.Primitive.Float
                    ConstantValueKind.Int, ConstantValueKind.IntegerLiteral,
                    ConstantValueKind.UnsignedInt, ConstantValueKind.UnsignedIntegerLiteral -> JavaType.Primitive.Int

                    ConstantValueKind.Long, ConstantValueKind.UnsignedLong -> JavaType.Primitive.Long
                    ConstantValueKind.Null -> JavaType.Primitive.Null
                    ConstantValueKind.Short, ConstantValueKind.UnsignedShort -> JavaType.Primitive.Short
                    ConstantValueKind.String -> JavaType.Primitive.String
                    ConstantValueKind.Error -> JavaType.Primitive.None
                    else -> throw UnsupportedOperationException("Unexpected constant value kind: ${type.kind}")
                }
            }

            else -> {
                JavaType.Primitive.None
            }
        }
    }
}
