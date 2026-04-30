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
import org.jetbrains.kotlin.codegen.classId
import org.jetbrains.kotlin.codegen.topLevelClassAsmType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.openrewrite.java.internal.JavaTypeFactory
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.*
import org.openrewrite.java.tree.JavaType.Array
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.convertClassIdToFqn
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.methodName
import org.openrewrite.kotlin.KotlinTypeSignatureBuilder.Companion.variableName

@Suppress("DuplicatedCode")
class KotlinTypeMapping(
    val firSession: FirSession,
    private val firFile: FirFile,
    private val typeFactory: JavaTypeFactory
) : JavaTypeMapping<Any> {

    private val signatureBuilder: KotlinTypeSignatureBuilder = KotlinTypeSignatureBuilder(firSession, firFile)

    override fun type(type: Any?): JavaType {
        if (type == null || type is FirErrorTypeRef || type is FirExpression && type.resolvedType is ConeErrorType || type is FirResolvedQualifier && type.classId == null) {
            return Unknown.getInstance()
        }

        val signature = signatureBuilder.signature(type)
        val existing: JavaType? = typeFactory.get(signature)
        if (existing != null) {
            return existing
        }

        return type(type, firFile, signature) ?: Unknown.getInstance()
    }

    fun type(type: Any?, parent: Any?): JavaType? {
        if (type == null || type is FirErrorTypeRef || type is FirExpression && type.resolvedType is ConeErrorType || type is FirResolvedQualifier && type.classId == null) {
            return Unknown.getInstance()
        }
        val signature = signatureBuilder.signature(type, parent)
        val existing = typeFactory.get<JavaType>(signature)
        if (existing != null) {
            return existing
        }
        return type(type, parent, signature)
    }

    @OptIn(SymbolInternals::class)
    fun type(classId: ClassId?, parent: Any?): JavaType? {
        if (classId == null) {
            return Unknown.getInstance()
        }
        val fir = classId.toSymbol(firSession)?.fir
        val signature = signatureBuilder.signature(fir, parent)
        val existing = typeFactory.get<JavaType>(signature)
        if (existing != null) {
            return existing
        }
        return type(fir, parent, signature)
    }

    @OptIn(SymbolInternals::class)
    fun type(type: Any?, parent: Any?, signature: String): JavaType? {
        return when (type) {
            is ConeClassLikeType, is FirClass, is FirResolvedQualifier -> {
                // Kotlin primitives compile to JVM primitives (for non-nullable uses) or
                // to boxed classes (for nullable uses / generic arguments). The parser
                // surfaces the JVM primitive form so dedup/recipes match what the Java
                // parser would produce. Class-definition contexts (where methods are being
                // declared on kotlin.Int itself) still resolve through `classType()` —
                // they don't reach `type()` for the declaring class.
                val nonNullable = when (type) {
                    is ConeClassLikeType -> !type.isMarkedNullable
                    else -> true
                }
                if (nonNullable) {
                    val primitive = kotlinPrimitiveFromFqn(signature)
                    if (primitive != null) {
                        return primitive
                    }
                }
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
                Unknown.getInstance()
            }

            is FirErrorNamedReference -> {
                Unknown.getInstance()
            }

            is FirSuperReference -> {
                type(type.superTypeRef, signature)
            }

            is FirFile -> {
                fileType(type, signature)
            }

            is FirFunction -> {
                methodDeclarationType(type, parent, signature)
            }

            is FirFunctionCall -> {
                methodInvocationType(type, signature)
            }

            is FirImport -> {
                resolveImport(type, signature)
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

            is FirPropertyAccessExpression -> {
                type(type.calleeReference, signature)
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
                type(type.lValue.resolvedType, parent, signature)
            }

            is FirExpression -> {
                type(type.resolvedType, parent, signature)
            }

            is JavaElement -> {
                javaElement(type, signature)
            }

            else -> {
                Unknown.getInstance()
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun resolveImport(type: FirImport, signature: String): JavaType? {
        if (type.importedFqName == null || type.importedFqName!!.isOneSegmentFQN()) {
            return null
        }

        // If the symbol is not resolvable, we return a NEW ShallowClass to prevent caching on a potentially resolvable class type.
        return type.importedFqName!!.topLevelClassAsmType().classId.toSymbol(firSession)
            ?.let { type(it.fir, signature) }
            ?: ShallowClass.build(signature)
                .withOwningClass(
                    (type as? FirResolvedImport)?.resolvedParentClassId?.toSymbol(firSession)
                        ?.let { it as? FirRegularClassSymbol }
                        ?.let { TypeUtils.asFullyQualified(type(it.fir, signature)) }
                )
    }

    private fun packageDirective(signature: String): JavaType? {
        val jt = ShallowClass.build(signature)
        typeFactory.put(signature, jt)
        return jt
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun fileType(file: FirFile, signature: String): JavaType {
        val functions = buildList {
            file.declarations.forEach {
                when (it) {
                    is FirNamedFunction -> add(it)
                    is FirScript -> it.declarations.filterIsInstance<FirNamedFunction>().forEach(::add)
                    else -> {}
                }
            }
        }
        val fileType = ShallowClass.build(signature)
            .withMethods(functions.map { methodDeclarationType(it, null) })
        typeFactory.put(signature, fileType)
        return fileType
    }

    private fun coneTypeProjectionType(type: ConeTypeProjection, signature: String): JavaType {
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
        return typeFactory.computeGenericTypeVariable(signature, name, JavaType.GenericTypeVariable.Variance.INVARIANT, type) { gtv ->
        var variance: GenericTypeVariable.Variance = JavaType.GenericTypeVariable.Variance.INVARIANT
        var bounds: MutableList<JavaType>? = null
        if (type is ConeKotlinTypeProjectionIn) {
            variance = GenericTypeVariable.Variance.CONTRAVARIANT
            val bound = type(type.type)
            // Match the Java parser: for `? super Object`, drop the `java.lang.Object`
            // bound so the wildcard surfaces as `Generic{? super }` (bound elided, variance
            // preserved). Without this, Kotlin-produced wildcards carry an explicit Object
            // bound and never dedup against the Java parser's elided form.
            if (bound !is FullyQualified || bound.fullyQualifiedName != "java.lang.Object") {
                bounds = ArrayList(1)
                bounds.add(bound)
            }
        } else if (type is ConeKotlinTypeProjectionOut) {
            variance = GenericTypeVariable.Variance.COVARIANT
            val bound = type(type.type)
            if (bound !is FullyQualified || bound.fullyQualifiedName != "java.lang.Object") {
                bounds = ArrayList(1)
                bounds.add(bound)
            }
        } else if (type is ConeTypeParameterType) {
            val classifierSymbol: FirClassifierSymbol<*>? = type.lookupTag.toSymbol(firSession)
            if (classifierSymbol is FirTypeParameterSymbol) {
                // Java-origin type parameters (e.g. java.util.Optional<T>) have their
                // kotlin.Any bound stripped (matching Java's unbounded `<T>`). Kotlin-source
                // `<T : Any>` keeps an explicit bound, remapped to java.lang.Object.
                val paramFromJava = classifierSymbol.containingDeclarationSymbol.origin is FirDeclarationOrigin.Java
                for (bound: FirResolvedTypeRef in classifierSymbol.resolvedBounds) {
                    if (bound !is FirImplicitNullableAnyTypeRef) {
                        var mapped = type(bound)
                        val fq = TypeUtils.asFullyQualified(mapped)
                        val originalWasKotlinAny = fq != null && "kotlin.Any" == fq.fullyQualifiedName
                        if (originalWasKotlinAny) {
                            if (paramFromJava) {
                                continue
                            }
                            mapped = remapKotlinBuiltin(fq)
                        } else if (fq != null) {
                            mapped = remapKotlinBuiltin(fq)
                        }
                        // When the original bound was `java.lang.Object` (not `kotlin.Any`),
                        // strip it to match the Java parser's unbounded `<T>` form. This
                        // handles Java-origin type parameters whose containing declaration's
                        // `origin` may surface as `Library` rather than `Java` (e.g. JDK
                        // classes loaded via Kotlin's classfile loader). Kotlin-source
                        // `<T : Any>` explicitly names `kotlin.Any` and is kept (remapped
                        // to `java.lang.Object`) to preserve the author's intent.
                        if (!originalWasKotlinAny) {
                            val mappedFq = TypeUtils.asFullyQualified(mapped)
                            if (mappedFq != null && "java.lang.Object" == mappedFq.fullyQualifiedName) {
                                continue
                            }
                        }
                        if (bounds == null) {
                            bounds = ArrayList()
                        }
                        bounds.add(mapped)
                    }
                }
                variance = when {
                    classifierSymbol.variance == Variance.INVARIANT -> {
                        if (bounds == null) GenericTypeVariable.Variance.INVARIANT else GenericTypeVariable.Variance.COVARIANT
                    }

                    classifierSymbol.variance == Variance.IN_VARIANCE && bounds != null -> {
                        GenericTypeVariable.Variance.CONTRAVARIANT
                    }

                    classifierSymbol.variance == Variance.OUT_VARIANCE && bounds != null -> {
                        GenericTypeVariable.Variance.COVARIANT
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
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun classType(type: Any, parent: Any?, signature: String): FullyQualified {
        val fqn = signatureBuilder.classSignature(type)
        var params: List<*>? = null
        val firClass = when (type) {
            is FirClass -> type
            is FirResolvedQualifier -> {
                val ref = type.resolvedType.toRegularClassSymbol(firSession)
                if (type.typeArguments.isNotEmpty()) {
                    params = type.typeArguments
                }
                if (ref == null) {
                    typeFactory.put(signature, Unknown.getInstance())
                    return Unknown.getInstance()
                }
                ref.fir
            }

            is ConeClassLikeType -> {
                if (type.toSymbol(firSession) is FirTypeAliasSymbol) {
                    return classType(
                        (type.toSymbol(firSession) as FirTypeAliasSymbol).resolvedExpandedTypeRef.coneType,
                        parent,
                        signature
                    )
                }
                var sym: Any? = type.toRegularClassSymbol(firSession)
                if (type.typeArguments.isNotEmpty()) {
                    params = type.typeArguments.toList()
                }

                if (sym is FirRegularClassSymbol) {
                    sym.fir
                } else {
                    sym = type.toSymbol(firSession)
                    if (sym is FirClassLikeSymbol<*>) {
                        sym.fir as FirClass
                    } else {
                        typeFactory.put(signature, Unknown.getInstance())
                        return Unknown.getInstance()
                    }
                }
            }

            else -> throw UnsupportedOperationException("Unexpected classType: ${type.javaClass}")
        }
        // Defensive: the cache may already hold a Parameterized at the fqn key. Unwrap
        // and short-circuit so we don't trip over the type cast inside computeClass.
        val cachedAtFqn: FullyQualified? = typeFactory.get(fqn)
        val clazz: Class = if (cachedAtFqn is Parameterized) {
            cachedAtFqn.type as Class
        } else if (cachedAtFqn is Class) {
            cachedAtFqn
        } else {
            var flags = mapToFlagsBitmap(firClass.visibility, firClass.modality(), firClass.isStatic)
            when (firClass.classKind) {
                ClassKind.INTERFACE, ClassKind.ANNOTATION_CLASS -> {
                    flags = flags or (1L shl 9)  // Interface
                    flags = flags or (1L shl 10) // Abstract
                    flags = flags and (1L shl 4).inv() // not Final
                }
                ClassKind.ENUM_CLASS -> {
                    flags = flags or (1L shl 14) // Enum
                }
                else -> {}
            }
            if (firClass.symbol.classId.isNestedClass &&
                (firClass.classKind == ClassKind.INTERFACE || firClass.classKind == ClassKind.ANNOTATION_CLASS)) {
                flags = flags or (1L shl 3) // Static
            }
            typeFactory.computeClass(fqn, fqn, flags, mapKind(firClass.classKind), firClass) { c ->
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
                            interfaceTypeRefs!!.add(t)
                        }

                        else -> {}
                    }
                }
                var supertype =
                    if (superTypeRef == null || "java.lang.Object" == signature) null else TypeUtils.asFullyQualified(
                        type(superTypeRef)
                    )
                if (supertype != null) {
                    supertype = remapKotlinBuiltin(supertype)
                }
                var declaringType: FullyQualified? = null
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
                    } else if (declaration is FirNamedFunction) {
                        functions.add(declaration as FirFunction)
                    } else if (declaration is FirConstructor) {
                        if (firClass.classKind != ClassKind.ANNOTATION_CLASS) {
                            functions.add(declaration as FirFunction)
                        }
                    } else if (declaration is FirEnumEntry) {
                        enumEntries.add(declaration)
                    } else if (declaration is FirAnonymousInitializer) {
                        // TODO
                    } else if (declaration is FirField) {
                        // TODO
                    } else if (declaration !is FirRegularClass) {
                        throw UnsupportedOperationException("Unsupported FirDeclaration: ${declaration.javaClass.name}")
                    }
                }

                var fields: MutableList<Variable>? = null
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
                var methods: MutableList<Method>? = null
                if (functions.isNotEmpty()) {
                    methods = ArrayList(functions.size)
                    for (function: FirFunction in functions) {
                        val mt = methodDeclarationType(function, firClass)
                        methods.add(mt)
                    }
                }
                var interfaces: MutableList<FullyQualified>? = null
                if (!interfaceTypeRefs.isNullOrEmpty()) {
                    interfaces = ArrayList(interfaceTypeRefs!!.size)
                    for (iParam: FirTypeRef? in interfaceTypeRefs!!) {
                        var javaType = TypeUtils.asFullyQualified(type(iParam))
                        if (javaType != null) {
                            javaType = remapKotlinBuiltin(javaType)
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
                c.unsafeSet(typeParameters,
                    supertype,
                    declaringType,
                    listAnnotations(firClass.annotations),
                    interfaces,
                    fields,
                    methods
                )
            }
        }

        // The signature for a ConeClassLikeType may be aliases without type parameters.
        if (firClass.typeParameters.isNotEmpty() && signature.contains("<")) {
            return typeFactory.computeParameterized(signature, type) { pt ->
                // Seed the Parameterized with its raw class before recursive lookups so
                // recursive resolution during typeParameter resolution observes a usable
                // base type rather than the all-null stub.
                pt.unsafeSet(clazz, null as List<JavaType>?)
                val typeParameters: MutableList<JavaType> = ArrayList(firClass.typeParameters.size)
                val resolvedParams = params ?: firClass.typeParameters
                for (tp in resolvedParams) {
                    typeParameters.add(type(tp))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
        }
        return clazz
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    fun methodDeclarationType(enumEntry: FirEnumEntry): Method? {
        val type = when (val fir = enumEntry.symbol.getContainingClassSymbol()?.fir) {
            is FirClass -> {
                when (val primary = fir.declarations.firstOrNull { it is FirPrimaryConstructor }) {
                    is FirPrimaryConstructor -> type(primary as FirFunction)
                    else -> null
                }
            }
            else -> null
        }
        return when (type) {
            is Method -> type
            else -> null
        }
    }

    fun methodDeclarationType(function: FirFunction, parent: Any?): Method {
        val signature = signatureBuilder.methodSignature(function, parent)
        return methodDeclarationType(function, parent, signature)
    }

    @OptIn(SymbolInternals::class)
    private fun methodDeclarationType(function: FirFunction, parent: Any?, signature: String): Method {
        val paramNamesArr: kotlin.Array<String>? = if (function.valueParameters.isNotEmpty()) {
            kotlin.Array(function.valueParameters.size) { function.valueParameters[it].name.asString() }
        } else null
        var methodFlags = mapToFlagsBitmap(function.visibility, function.modality, function.isStatic)
        if (function.symbol is FirConstructorSymbol) {
            methodFlags = methodFlags and (1L shl 4).inv()
        }
        val parentClass = if (parent is FirRegularClass) parent else null
        val isPrivate = methodFlags and (1L shl 1) != 0L
        if (parentClass != null &&
            parentClass.classKind == ClassKind.INTERFACE &&
            !function.isStatic &&
            !isPrivate &&
            function.symbol !is FirConstructorSymbol) {
            methodFlags = methodFlags or (1L shl 10) // Abstract
            if (function.modality != Modality.ABSTRACT) {
                methodFlags = methodFlags or (1L shl 43) // Default
            }
        }
        val name = if (function.symbol is FirConstructorSymbol) "<constructor>" else methodName(function)
        return typeFactory.computeMethod(signature, methodFlags, name, paramNamesArr, null, null, function) { method ->
            var parentType: JavaType? = when {
                function.symbol is FirConstructorSymbol -> type(function.returnTypeRef)
                function.dispatchReceiverType is ConeClassLikeType ->
                    asDeclaringType(function.dispatchReceiverType as ConeClassLikeType)
                function.dispatchReceiverType != null -> type(function.dispatchReceiverType!!)
                function.symbol.getOwnerLookupTag()?.toRegularClassSymbol(firSession)?.fir != null -> {
                    val fir = function.symbol.getOwnerLookupTag()!!.toRegularClassSymbol(firSession)!!.fir
                    TypeUtils.asFullyQualified(classType(fir, firFile, signatureBuilder.signature(fir)))
                }

                parent is FirRegularClass || parent != null -> type(parent)
                else -> type(firFile)
            }
            if (parentType is Method) {
                parentType = parentType.declaringType
            }
            if (parentType is Parameterized) {
                parentType = parentType.type
            }
            val resolvedDeclaringType = TypeUtils.asFullyQualified(parentType)
            // Only remap kotlin.* builtins to JVM FQNs for methods declared on Java-origin
            // classes (third-party Java libraries loaded as FirJavaClass). The Kotlin compiler's
            // FIR renders a Java method's `String` parameter as `kotlin.String` even though the
            // bytecode signature is `java.lang.String`; remapping puts the parser's output back
            // in line with the JVM signature so MethodMatcher patterns written against Java
            // FQNs match. Kotlin-declared methods are left alone — `kotlin.Any` there is the
            // author's intent, and Kotlin-aware matching belongs in KotlinTypeUtils.
            val javaOrigin = parent is FirJavaClass
            var returnType = if (javaOrigin) remapKotlinBuiltin(type(function.returnTypeRef)) else type(function.returnTypeRef)
            if (function.symbol is FirConstructorSymbol && returnType is Parameterized) {
                returnType = returnType.type
            }
            val parameterTypes: MutableList<JavaType>? = when {
                function.receiverParameter != null || function.valueParameters.isNotEmpty() -> {
                    ArrayList(function.valueParameters.size + (if (function.receiverParameter != null) 1 else 0))
                }

                else -> null
            }
            if (function.receiverParameter != null) {
                val rt = type(function.receiverParameter!!.typeRef)
                parameterTypes!!.add(if (javaOrigin) remapKotlinBuiltin(rt)!! else rt)
            }
            if (function.valueParameters.isNotEmpty()) {
                for (p in function.valueParameters) {
                    val t = type(p.returnTypeRef, function)
                    if (t != null) {
                        parameterTypes!!.add(if (javaOrigin) remapKotlinBuiltin(t)!! else t)
                    }
                }
            }
            method.unsafeSet(resolvedDeclaringType,
                returnType,
                parameterTypes, null, listAnnotations(function.annotations)
            )
        }
    }

    private fun methodDeclarationType(
        javaMethod: JavaMethod,
        declaringType: FullyQualified?
    ): Method? {
        val signature = signatureBuilder.javaMethodSignature(javaMethod)
        return methodDeclarationType(javaMethod, declaringType, signature)
    }

    private fun methodDeclarationType(
        javaMethod: JavaMethod,
        declaringType: FullyQualified?,
        signature: String
    ): Method? {
        // Resolve declaring type before computing so we don't strand a stub in the cache
        // when the resolution fails.
        val resolvedDeclaringType = declaringType ?: TypeUtils.asFullyQualified(type(javaMethod.containingClass))
        if (resolvedDeclaringType == null) {
            return null
        }
        val paramNamesArr: kotlin.Array<String>? = if (javaMethod.valueParameters.isNotEmpty()) {
            kotlin.Array(javaMethod.valueParameters.size) { i -> "arg$i" }
        } else null
        var defaultValues: MutableList<String>? = null
        if (javaMethod.annotationParameterDefaultValue != null) {
            if (javaMethod.annotationParameterDefaultValue!!.name != null) {
                defaultValues = ArrayList()
                defaultValues.add(javaMethod.annotationParameterDefaultValue!!.name!!.asString())
            }
        }
        var methodFlags = if (javaMethod is BinaryJavaMethod) {
            javaMethod.access.toLong()
        } else {
            convertToFlagsBitMap(
                javaMethod.visibility,
                javaMethod.isStatic,
                javaMethod.isFinal,
                javaMethod.isAbstract
            )
        }
        if (methodFlags and (1L shl 7) != 0L) {
            methodFlags = methodFlags and (1L shl 7).inv()
            methodFlags = methodFlags or (1L shl 34) // Varargs
        }
        val isPrivate = methodFlags and (1L shl 1) != 0L
        if (javaMethod.containingClass.isInterface && !javaMethod.isStatic && !isPrivate) {
            methodFlags = methodFlags or (1L shl 10) // Abstract
            if (!javaMethod.isAbstract) {
                methodFlags = methodFlags or (1L shl 43) // Default
            }
        }
        for (ann in javaMethod.annotations) {
            val classId = ann.classId
            if (classId != null && "java.lang.invoke.MethodHandle.PolymorphicSignature" == classId.asSingleFqName().asString()) {
                methodFlags = methodFlags or (1L shl 46) // SignaturePolymorphic
                break
            }
        }
        return typeFactory.computeMethod(signature, methodFlags, javaMethod.name.asString(), paramNamesArr, defaultValues, null, javaMethod) { method ->
            val exceptionTypes: List<JavaType>? = null
            val returnType = type(javaMethod.returnType)
            var parameterTypes: MutableList<JavaType>? = null
            if (javaMethod.valueParameters.isNotEmpty()) {
                parameterTypes = ArrayList(javaMethod.valueParameters.size)
                for (parameterSymbol: JavaValueParameter in javaMethod.valueParameters) {
                    val javaType = type(parameterSymbol.type)
                    parameterTypes.add(javaType)
                }
            }
            method.unsafeSet(resolvedDeclaringType,
                returnType,
                parameterTypes, exceptionTypes, listAnnotations(javaMethod.annotations)
            )
        }
    }

    fun methodInvocationType(fir: FirFunctionCall): Method? {
        if (fir.resolvedType is ConeErrorType) {
            return null
        }
        val signature = signatureBuilder.methodCallSignature(fir)
        return methodInvocationType(fir, signature)
    }

    @OptIn(SymbolInternals::class)
    fun methodInvocationType(function: FirFunctionCall, signature: String): Method? {
        val sym = function.calleeReference.toResolvedBaseSymbol() ?: return null
        val receiver = if (sym is FirFunctionSymbol<*>) sym.receiverParameterSymbol else null
        val totalParams = if (sym is FirFunctionSymbol<*>) {
            sym.valueParameterSymbols.size + (if (receiver != null) 1 else 0)
        } else 0
        val paramNamesArr: kotlin.Array<String>? = if (totalParams > 0) {
            val arr = kotlin.Array(totalParams) { "" }
            var idx = 0
            if (receiver != null) {
                arr[idx++] = '$' + "this" + '$'
            }
            if (function.arguments.isNotEmpty() && sym is FirFunctionSymbol<*>) {
                for (p in sym.valueParameterSymbols) {
                    arr[idx++] = p.name.asString()
                }
            }
            // Trim if not all slots were filled (when arguments empty but valueParameters present)
            if (idx < totalParams) arr.copyOf(idx).requireNoNulls() else arr
        } else null
        var invocationFlags = when (sym) {
            is FirConstructorSymbol -> mapToFlagsBitmap(sym.visibility, sym.modality, sym.isStatic)
            is FirNamedFunctionSymbol -> mapToFlagsBitmap(sym.visibility, sym.modality, sym.isStatic)
            else -> {
                throw UnsupportedOperationException("Unsupported method symbol: ${sym.javaClass.name}")
            }
        }
        if (sym is FirNamedFunctionSymbol) {
            val invocationContainer = sym.containingClassLookupTag()?.toRegularClassSymbol(firSession)?.fir
            if (invocationContainer != null &&
                invocationContainer.classKind == ClassKind.INTERFACE &&
                !sym.isStatic) {
                invocationFlags = invocationFlags or (1L shl 10) // Abstract
                if (sym.modality != Modality.ABSTRACT) {
                    invocationFlags = invocationFlags or (1L shl 43) // Default
                }
            }
        }
        val name = when {
            sym is FirConstructorSymbol ||
                    sym is FirSyntheticFunctionSymbol && sym.origin == FirDeclarationOrigin.SamConstructor -> "<constructor>"

            else -> (sym as FirNamedFunctionSymbol).name.asString()
        }
        return typeFactory.computeMethod(signature, invocationFlags, name, paramNamesArr, null, null, function) { method ->
            var paramTypes: MutableList<JavaType>? = if (paramNamesArr != null) ArrayList(paramNamesArr.size) else null
            var declaringType: FullyQualified? = null
            if (function.calleeReference is FirResolvedNamedReference &&
                (function.calleeReference as FirResolvedNamedReference).resolvedSymbol is FirNamedFunctionSymbol
            ) {
                val resolvedSymbol =
                    (function.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirNamedFunctionSymbol
                if (resolvedSymbol.dispatchReceiverType is ConeClassLikeType) {
                    declaringType = asDeclaringType(resolvedSymbol.dispatchReceiverType as ConeClassLikeType)
                } else if (resolvedSymbol.containingClassLookupTag() != null &&
                    resolvedSymbol.containingClassLookupTag()!!.toRegularClassSymbol(firSession)?.fir != null
                ) {
                    declaringType = TypeUtils.asFullyQualified(
                        type(
                            resolvedSymbol.containingClassLookupTag()!!.toRegularClassSymbol(firSession)!!.fir
                        )
                    )
                } else if (resolvedSymbol.origin == FirDeclarationOrigin.Library) {
                    if (resolvedSymbol.fir.containerSource is JvmPackagePartSource) {
                        val source: JvmPackagePartSource? = resolvedSymbol.fir.containerSource as JvmPackagePartSource?
                        if (source != null) {
                            declaringType = if (source.facadeClassName != null) {
                                createShallowClass((source.facadeClassName as JvmClassName).fqNameForTopLevelClassMaybeWithDollars.asString())
                            } else {
                                createShallowClass(source.className.fqNameForTopLevelClassMaybeWithDollars.asString())
                            }
                        }
                    } else if (!resolvedSymbol.fir.origin.generated &&
                        !resolvedSymbol.fir.origin.fromSupertypes &&
                        !resolvedSymbol.fir.origin.fromSource
                    ) {
                        declaringType = createShallowClass("kotlin.Library")
                    }
                } else if (resolvedSymbol.origin == FirDeclarationOrigin.SamConstructor) {
                    declaringType = when(val type = type(function.resolvedType)) {
                        is Class -> type
                        is Parameterized -> type.type
                        else -> Unknown.getInstance()
                    }
                } else {
                    declaringType = TypeUtils.asFullyQualified(type(resolvedSymbol.getContainingFile()))
                }
            } else {
                declaringType = TypeUtils.asFullyQualified(type(function.resolvedType))
            }
            if (declaringType == null) {
                declaringType = TypeUtils.asFullyQualified(type(firFile))
            }
            // See methodDeclarationType: only remap for Java-origin methods so Kotlin-declared
            // signatures (e.g. `kotlin.io.ConsoleKt.println(kotlin.Any)`) are preserved.
            // The discriminator is the containing class FIR — Java-origin classes (third-party
            // Java libraries on the classpath) are loaded as FirJavaClass even when the
            // synthesized member FIR is FirConstructorImpl with an Enhancement origin. JDK
            // classes are special-cased by Kotlin's classfile loader and aren't FirJavaClass —
            // recipes targeting JDK signatures need a Kotlin-aware matcher instead.
            val javaOrigin = (sym as? FirCallableSymbol<*>)
                ?.containingClassLookupTag()?.toRegularClassSymbol(firSession)?.fir is FirJavaClass
            val returnType = if (javaOrigin) remapKotlinBuiltin(type(function.resolvedType)) else type(function.resolvedType)

            if (function.toResolvedCallableSymbol()?.receiverParameterSymbol != null) {
                if (paramTypes == null) paramTypes = ArrayList()
                val rt = type(function.toResolvedCallableSymbol()?.receiverParameterSymbol!!.fir.typeRef)
                paramTypes.add(if (javaOrigin) remapKotlinBuiltin(rt)!! else rt)
            }
            val paramToArg: Map<String, FirExpression>? =
                (function.argumentList as? FirResolvedArgumentList)?.mapping
                    ?.entries?.associate { (arg, param) -> param.name.asString() to arg }

            val valueParams = (function.toResolvedCallableSymbol()?.fir as FirFunction).valueParameters
            for ((_, p) in valueParams.withIndex()) {
                if (paramTypes == null) {
                    paramTypes = ArrayList()
                }
                val t = type(p.returnTypeRef)
                if (t is GenericTypeVariable) {
                    val arg = paramToArg?.get(p.name.asString())
                    if (arg != null) {
                        val argType = type(arg.resolvedType, function)!!
                        paramTypes.add(if (javaOrigin) remapKotlinBuiltin(argType)!! else argType)
                    } else {
                        paramTypes.add(t)
                    }
                } else {
                    paramTypes.add(if (javaOrigin) remapKotlinBuiltin(t)!! else t)
                }
            }
            method.unsafeSet(declaringType,
                returnType,
                paramTypes, null, listAnnotations(function.annotations)
            )
        }
    }

    /**
     * Kotlin builtins like kotlin.Any, kotlin.Annotation, kotlin.String, kotlin.Int, etc.
     * compile to specific JVM types (java.lang.Object, java.lang.annotation.Annotation, etc.).
     * The Java parser produces the JVM FQNs directly, so when Kotlin's FIR resolves a type to
     * a Kotlin builtin, we remap it to its JVM equivalent so cross-parser dedup can match them.
     */
    /**
     * Synthesize the enum `static T[] values()` method that the Java compiler generates
     * for every enum class. The Kotlin parser only sees source-declared methods, so
     * cross-parser dedup of enum types needs these added back.
     */
    private fun enumValuesMethod(declaringType: Class): Method {
        // ACC_PUBLIC | ACC_STATIC (matches the Java parser's output)
        val flags = 1L or (1L shl 3)
        val method = Method(
            null, flags, null, "values", null,
            null as MutableList<String>?,
            null as MutableList<JavaType>?,
            null as MutableList<JavaType>?,
            null as MutableList<FullyQualified>?,
            null as MutableList<String>?,
            null as MutableList<String>?
        )
        val array = Array(null, null, null)
        array.unsafeSet(declaringType, null)
        method.unsafeSet(
            declaringType, array as JavaType,
            null as MutableList<JavaType>?,
            null as MutableList<JavaType>?,
            null as MutableList<FullyQualified>?
        )
        return method
    }

    /**
     * Synthesize the enum `static T valueOf(String)` method that the Java compiler
     * generates for every enum class.
     */
    private fun enumValueOfMethod(declaringType: Class): Method {
        // ACC_PUBLIC | ACC_STATIC
        val flags = 1L or (1L shl 3)
        val method = Method(
            null, flags, null, "valueOf", null,
            mutableListOf("name"),
            null as MutableList<JavaType>?,
            null as MutableList<JavaType>?,
            null as MutableList<FullyQualified>?,
            null as MutableList<String>?,
            null as MutableList<String>?
        )
        val stringType: JavaType = typeFactory.get<FullyQualified>("java.lang.String") ?: ShallowClass.build("java.lang.String")
        val params: MutableList<JavaType> = mutableListOf(stringType)
        method.unsafeSet(
            declaringType, declaringType as JavaType, params,
            null as MutableList<JavaType>?,
            null as MutableList<FullyQualified>?
        )
        return method
    }

    /**
     * Resolve a Kotlin cone type to its declaring-class form (always {@link FullyQualified}),
     * bypassing the primitive remap that {@link #type(Any?, Any?, String)} normally applies
     * to non-nullable kotlin primitives. Method declaring types are class instances even
     * when the receiver value is a JVM primitive — `kotlin.Char.toInt()` is a method on
     * the kotlin.Char class, not on the JVM `char` primitive.
     */
    private fun asDeclaringType(coneType: ConeClassLikeType): FullyQualified? {
        val signature = signatureBuilder.signature(coneType)
        val cached = typeFactory.get<FullyQualified>(signature)
        if (cached != null) {
            return cached
        }
        return TypeUtils.asFullyQualified(classType(coneType, firFile, signature))
    }

    private fun kotlinPrimitiveFromFqn(fqn: String): Primitive? {
        return when (fqn) {
            "kotlin.Int" -> Primitive.Int
            "kotlin.Long" -> Primitive.Long
            "kotlin.Short" -> Primitive.Short
            "kotlin.Byte" -> Primitive.Byte
            "kotlin.Float" -> Primitive.Float
            "kotlin.Double" -> Primitive.Double
            "kotlin.Boolean" -> Primitive.Boolean
            "kotlin.Char" -> Primitive.Char
            "kotlin.Unit" -> Primitive.Void
            else -> null
        }
    }

    /**
     * Construct a JVM-style fully-qualified name for a binary Java class, where nested
     * classes are separated by `$` from their containing class. Kotlin's
     * `BinaryJavaClass.fqName` uses `.` throughout, which conflicts with the Java parser's
     * output and prevents nested classes (e.g. `java.util.Map$Entry`) from dedup-matching.
     */
    private fun toJvmFqn(type: BinaryJavaClass): String {
        val outer = type.outerClass
        if (outer != null) {
            return toJvmFqn(outer as BinaryJavaClass) + "$" + type.name.asString()
        }
        return type.fqName.asString()
    }

    /**
     * Convenience overload: apply [remapKotlinBuiltin] to any [JavaType], returning the
     * input unchanged when it is not a [FullyQualified]. Callers use this on method
     * parameter, receiver, return, and field types so Kotlin builtins (kotlin.String /
     * kotlin.Throwable / ...) surface as the JVM FQN the Java parser would produce.
     */
    private fun remapKotlinBuiltin(t: JavaType?): JavaType? {
        if (t == null) return null
        val fq = TypeUtils.asFullyQualified(t)
        return if (fq != null) remapKotlinBuiltin(fq) else t
    }

    private fun remapKotlinBuiltin(fq: FullyQualified): FullyQualified {
        // For a Parameterized (e.g. kotlin.Enum<Foo>) remap the underlying raw type but
        // keep the parameterization so we don't lose type argument information.
        if (fq is Parameterized) {
            val inner = fq.type ?: return fq
            val remappedInner = remapKotlinBuiltin(inner)
            if (remappedInner === inner) {
                return fq
            }
            val pt = Parameterized(null, null, null)
            pt.unsafeSet(remappedInner, fq.typeParameters)
            return pt
        }
        val javaFqn = when (fq.fullyQualifiedName) {
            "kotlin.Any" -> "java.lang.Object"
            "kotlin.Annotation" -> "java.lang.annotation.Annotation"
            "kotlin.Throwable" -> "java.lang.Throwable"
            "kotlin.Comparable" -> "java.lang.Comparable"
            "kotlin.CharSequence" -> "java.lang.CharSequence"
            "kotlin.Number" -> "java.lang.Number"
            "kotlin.String" -> "java.lang.String"
            "kotlin.Enum" -> "java.lang.Enum"
            // Kotlin translates meta-annotations read from Java class files into its own
            // kotlin.annotation.* equivalents. Remap to the Java names so cross-parser
            // dedup collapses them to a single instance.
            "kotlin.annotation.Retention" -> "java.lang.annotation.Retention"
            "kotlin.annotation.MustBeDocumented" -> "java.lang.annotation.Documented"
            "kotlin.annotation.Target" -> "java.lang.annotation.Target"
            "kotlin.annotation.Repeatable" -> "java.lang.annotation.Repeatable"
            else -> return fq
        }
        // Prefer an existing entry in the type cache so we return the full Class rather
        // than a minimal ShallowClass (which would hide the underlying type information).
        val existing = typeFactory.get<FullyQualified>(javaFqn)
        if (existing != null) {
            return existing
        }
        return ShallowClass.build(javaFqn)
    }

    private fun createShallowClass(name: String): FullyQualified {
        val c = ShallowClass.build(name)
        typeFactory.put(name, c)
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
        val name = type.name.asString()
        return typeFactory.computeGenericTypeVariable(signature, name, GenericTypeVariable.Variance.INVARIANT, type) { gtv ->
            var bounds: MutableList<JavaType>? = null
            var variance: GenericTypeVariable.Variance = GenericTypeVariable.Variance.INVARIANT
            val containerFromJava = type.containingDeclarationSymbol.origin is FirDeclarationOrigin.Java
            if (!(type.bounds.size == 1 && type.bounds[0] is FirImplicitNullableAnyTypeRef)) {
                bounds = ArrayList(type.bounds.size)
                for (bound: FirTypeRef in type.bounds) {
                    var boundType = type(bound)
                    val fq = TypeUtils.asFullyQualified(boundType)
                    val originalWasKotlinAny = fq != null && "kotlin.Any" == fq.fullyQualifiedName
                    if (originalWasKotlinAny) {
                        if (containerFromJava) {
                            continue
                        }
                        boundType = remapKotlinBuiltin(fq)
                    } else if (fq != null) {
                        boundType = remapKotlinBuiltin(fq)
                    }
                    if (!originalWasKotlinAny) {
                        val mappedFq = TypeUtils.asFullyQualified(boundType)
                        if (mappedFq != null && "java.lang.Object" == mappedFq.fullyQualifiedName) {
                            continue
                        }
                    }
                    bounds.add(boundType)
                }
                if (bounds.isEmpty()) {
                    bounds = null
                } else if (type.variance == Variance.IN_VARIANCE) {
                    variance = GenericTypeVariable.Variance.CONTRAVARIANT
                } else {
                    variance = GenericTypeVariable.Variance.COVARIANT
                }
            }
            gtv.unsafeSet(name, variance, bounds)
        }
    }

    fun variableType(variable: FirVariable, parent: Any?): Variable {
        val signature = signatureBuilder.variableSignature(variable, parent)
        return variableType(variable, parent, signature)
    }

    @OptIn(SymbolInternals::class)
    fun variableType(variable: FirVariable, parent: Any?, signature: String): Variable {
        return typeFactory.computeVariable(
            signature,
            mapToFlagsBitmap(variable.visibility, variable.modality, variable.isStatic),
            variableName(variable.name.asString()),
            variable
        ) { vt ->
            val annotations = listAnnotations(variable.annotations)
            var declaringType: JavaType? = null
            when {
                variable.symbol.dispatchReceiverType != null -> {
                    declaringType = type(variable.symbol.dispatchReceiverType)
                }

                variable.symbol.getContainingClassSymbol() != null -> {
                    if (variable.symbol.getContainingClassSymbol() !is FirAnonymousObjectSymbol) {
                        declaringType = type(variable.symbol.getContainingClassSymbol()!!.fir)
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

            if (declaringType is Parameterized) {
                declaringType = declaringType.type
            }

            // See methodDeclarationType: only remap for Java-origin variables (e.g. fields read
            // from a Java class file) so Kotlin properties keep their author-intended types.
            val typeRef = if (variable is FirJavaField) {
                remapKotlinBuiltin(type(variable.returnTypeRef))
            } else {
                type(variable.returnTypeRef)
            }
            vt.unsafeSet(declaringType!!, typeRef, annotations)
        }
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
        return typeFactory.computeArray(signature, type) { arrayType ->
            val classType = type(type.componentType)
            arrayType.unsafeSet(classType, null)
        }
    }

    private fun javaClassType(type: JavaClassifier, signature: String): JavaType {
        if (type !is BinaryJavaClass) {
            throw UnsupportedOperationException("Unsupported JavaClassifier: ${type.javaClass.name}")
        }
        val fqn = toJvmFqn(type)
        var flags = type.access.toLong()
        if (type.isInterface || type.isAnnotationType) {
            flags = flags or (1L shl 9)  // Interface
            flags = flags or (1L shl 10) // Abstract
            flags = flags and (1L shl 4).inv() // not Final
        }
        if (type.isEnum) {
            flags = flags or (1L shl 14) // Enum
        }
        if (fqn.contains('$') && (type.isInterface || type.isAnnotationType)) {
            flags = flags or (1L shl 3) // Static
        }
        val kind = when {
            type.isAnnotationType -> FullyQualified.Kind.Annotation
            type.isEnum -> FullyQualified.Kind.Enum
            type.isInterface -> FullyQualified.Kind.Interface
            type.isRecord -> FullyQualified.Kind.Record
            else -> FullyQualified.Kind.Class
        }
        val clazz: Class = typeFactory.computeClass(fqn, fqn, flags, kind, type) { c ->
            var supertype: FullyQualified? = null
            var interfaces: MutableList<FullyQualified>? = null
            for (classifierSupertype: JavaClassifierType in type.supertypes) {
                if (classifierSupertype.classifier is JavaClass) {
                    if ((classifierSupertype.classifier as JavaClass?)!!.isInterface) {
                        if (interfaces == null) {
                            interfaces = ArrayList()
                        }
                        interfaces!!.add(type(classifierSupertype) as FullyQualified)
                    } else if ("java.lang.Object" != fqn) {
                        supertype = type(classifierSupertype) as FullyQualified
                    }
                }
            }
            var owner: FullyQualified? = null
            if (type.outerClass != null) {
                owner = TypeUtils.asFullyQualified(type(type.outerClass))
            }
            var fields: MutableList<Variable>? = null
            if (type.fields.isNotEmpty()) {
                fields = ArrayList(type.fields.size)
                for (field: JavaField in type.fields) {
                    if (fqn == "java.lang.String" && field.name.asString() == "serialPersistentFields") {
                        continue
                    }
                    fields.add(javaVariableType(field, c))
                }
            }
            var methods: MutableList<Method>? = null
            if (type.methods.isNotEmpty()) {
                methods = ArrayList(type.methods.size)
                for (method: JavaMethod in type.methods) {
                    val mt = methodDeclarationType(method, c)
                    if (mt != null) {
                        methods!!.add(mt)
                    }
                }
            }
            if (type.constructors.isNotEmpty()) {
                for (method: JavaConstructor in type.constructors) {
                    if (method is BinaryJavaConstructor) {
                        if (methods == null) {
                            methods = ArrayList()
                        }
                        if (method.access.toLong() and ((1 shl 12).toLong() or (1L shl 31) or (1L shl 37) or (1 shl 29).toLong()) == 0L) {
                            val ms = javaConstructorType(method, c)
                            if (ms != null) {
                                methods!!.add(ms)
                            }
                        }
                    }
                }
            }
            if (type.isEnum) {
                if (methods == null) {
                    methods = ArrayList()
                }
                methods!!.add(enumValuesMethod(c))
                methods!!.add(enumValueOfMethod(c))
            }
            var typeParameters: MutableList<JavaType>? = null
            if (type.typeParameters.isNotEmpty()) {
                typeParameters = ArrayList(type.typeParameters.size)
                for (typeArgument: JavaTypeParameter in type.typeParameters) {
                    typeParameters!!.add(type(typeArgument))
                }
            }
            c.unsafeSet(typeParameters,
                supertype,
                owner,
                listAnnotations(type.annotations),
                interfaces,
                fields,
                methods
            )
        }
        if (type.typeParameters.isNotEmpty()) {
            return typeFactory.computeParameterized(signature, type) { pt ->
                // Seed the Parameterized with its raw class before recursive lookups so
                // typeParameter resolution observes a usable base type rather than the
                // all-null stub. Without this, cycles through members that reference
                // this same class resolve through a partially-initialized cache entry.
                pt.unsafeSet(clazz, null as List<JavaType>?)
                val typeParameters: MutableList<JavaType> = ArrayList(type.typeParameters.size)
                for (typeArgument: JavaTypeParameter in type.typeParameters) {
                    typeParameters.add(type(typeArgument))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
        }
        return clazz
    }

    private fun javaClassType(type: JavaClassifierType, signature: String): JavaType? {
        // When the classifier is a type parameter (e.g. the `S` return type on a method
        // declared with `<S extends BaseStream<T, S>>`), resolving it gives a
        // GenericTypeVariable rather than a FullyQualified. A JavaClassifierType whose
        // classifier is a type parameter can never be parameterized in its own right
        // (type parameters don't take type arguments), so return the GTV directly —
        // otherwise we fall through to a null FullyQualified and the enclosing
        // `type()` returns Unknown, which breaks F-bounded generic resolution.
        if (type.classifier is JavaTypeParameter && type.typeArguments.isEmpty()) {
            return type(type.classifier!!)
        }
        var clazz : FullyQualified? = if (type.classifier != null) {
            TypeUtils.asFullyQualified(type(type.classifier!!))
        } else {
            createShallowClass(type.classifierQualifiedName)
        }

        if (type.typeArguments.isNotEmpty()) {
            if (clazz is Parameterized) {
                clazz = clazz.type
            }
            val rawClass = clazz
            return typeFactory.computeParameterized(signature, type) { pt ->
                // Seed the Parameterized with its raw class before recursive lookups so
                // typeArgument resolution observes a usable base type rather than `Unknown`.
                pt.unsafeSet(rawClass, null as List<JavaType>?)
                val typeParameters: MutableList<JavaType> = ArrayList(type.typeArguments.size)
                for (typeArgument: org.jetbrains.kotlin.load.java.structure.JavaType? in type.typeArguments) {
                    typeParameters.add(type(typeArgument))
                }
                pt.unsafeSet(rawClass, typeParameters)
            }
        }
        if (clazz is Parameterized) {
            clazz = clazz.type
        }
        return clazz
    }

    private fun javaConstructorType(
        constructor: JavaConstructor,
        declaringType: FullyQualified?
    ): Method? {
        val signature = signatureBuilder.javaConstructorSignature(constructor)
        // Resolve declaring type before computing so we don't strand a stub in the cache
        // when the resolution fails.
        var resolvedDeclaringType: FullyQualified? = declaringType
            ?: TypeUtils.asFullyQualified(type(constructor.containingClass))
        if (resolvedDeclaringType == null) {
            return null
        }
        // The Java parser uses the raw Class (not its Parameterized form) as both the
        // declaringType and returnType of a constructor.
        if (resolvedDeclaringType is Parameterized) {
            resolvedDeclaringType = resolvedDeclaringType.type
        }
        val finalDeclaringType: FullyQualified = resolvedDeclaringType!!
        val paramNamesArr: kotlin.Array<String>? = if (constructor.valueParameters.isNotEmpty()) {
            kotlin.Array(constructor.valueParameters.size) { i -> "arg$i" }
        } else null
        var constructorFlags = if (constructor is BinaryJavaConstructor) {
            constructor.access.toLong()
        } else {
            convertToFlagsBitMap(
                constructor.visibility,
                constructor.isStatic,
                constructor.isFinal,
                constructor.isAbstract
            )
        }
        if (constructorFlags and (1L shl 7) != 0L) {
            constructorFlags = constructorFlags and (1L shl 7).inv()
            constructorFlags = constructorFlags or (1L shl 34)
        }
        constructorFlags = constructorFlags and (1L shl 4).inv()
        return typeFactory.computeMethod(signature, constructorFlags, "<constructor>", paramNamesArr, null, null, constructor) { method ->
            val exceptionTypes: List<JavaType>? = null
            var parameterTypes: MutableList<JavaType>? = null
            if (constructor.valueParameters.isNotEmpty()) {
                parameterTypes = ArrayList(constructor.valueParameters.size)
                for (parameterSymbol: JavaValueParameter in constructor.valueParameters) {
                    val javaType = type(parameterSymbol.type)
                    parameterTypes.add(javaType)
                }
            }
            method.unsafeSet(finalDeclaringType,
                finalDeclaringType,
                parameterTypes, exceptionTypes, listAnnotations(constructor.annotations)
            )
        }
    }

    private fun javaPrimitiveType(type: JavaPrimitiveType): JavaType {
        return when (type.type) {
            PrimitiveType.BOOLEAN -> Primitive.Boolean
            PrimitiveType.BYTE -> Primitive.Byte
            PrimitiveType.CHAR -> Primitive.Char
            PrimitiveType.DOUBLE -> Primitive.Double
            PrimitiveType.FLOAT -> Primitive.Float
            PrimitiveType.INT -> Primitive.Int
            PrimitiveType.LONG -> Primitive.Long
            PrimitiveType.SHORT -> Primitive.Short
            null -> Primitive.Void
        }
    }

    private fun javaTypeParameter(type: JavaTypeParameter, signature: String): JavaType {
        val name = type.name.asString()
        return typeFactory.computeGenericTypeVariable(signature, name, GenericTypeVariable.Variance.INVARIANT, type) { gtv ->
            var bounds: List<JavaType>? = null
            if (type.upperBounds.size == 1) {
                val mappedBound = type(type.upperBounds.toTypedArray()[0])
                if (mappedBound !is FullyQualified ||
                    ("java.lang.Object" != mappedBound.fullyQualifiedName && "kotlin.Any" != mappedBound.fullyQualifiedName)) {
                    bounds = listOf(mappedBound)
                }
            } else {
                bounds = ArrayList(type.upperBounds.size)
                for (bound: org.jetbrains.kotlin.load.java.structure.JavaType in type.upperBounds) {
                    (bounds as ArrayList<JavaType>).add(type(bound))
                }
            }
            gtv.unsafeSet(name,
                if (bounds == null) GenericTypeVariable.Variance.INVARIANT else GenericTypeVariable.Variance.COVARIANT,
                bounds
            )
        }
    }

    private fun javaWildCardType(type: JavaWildcardType, signature: String): JavaType {
        val name = "?"
        return typeFactory.computeGenericTypeVariable(signature, name, GenericTypeVariable.Variance.INVARIANT, type) { gtv ->
            var variance = GenericTypeVariable.Variance.INVARIANT
            var bounds: MutableList<JavaType>? = null
            if (type.bound != null) {
                variance = if (type.isExtends) {
                    GenericTypeVariable.Variance.COVARIANT
                } else {
                    GenericTypeVariable.Variance.CONTRAVARIANT
                }
                val bound = type(type.bound)
                if (bound !is FullyQualified || bound.fullyQualifiedName != "java.lang.Object") {
                    bounds = ArrayList(1)
                    bounds.add(bound)
                }
            }
            gtv.unsafeSet(name, variance, bounds)
        }
    }

    private fun javaVariableType(javaField: JavaField, owner: JavaType?): Variable {
        val signature = signatureBuilder.javaVariableSignature(javaField)
        return typeFactory.computeVariable(
            signature,
            convertToFlagsBitMap(javaField.visibility, javaField.isStatic, javaField.isFinal, javaField.isAbstract),
            variableName(javaField.name.asString()),
            javaField
        ) { variable ->
            var resolvedOwner: JavaType? = owner
            if (owner == null) {
                resolvedOwner = TypeUtils.asFullyQualified(type(javaField.containingClass))
                assert(resolvedOwner != null)
            }
            variable.unsafeSet(resolvedOwner!!, type(javaField.type), listAnnotations(javaField.annotations))
        }
    }

    @OptIn(SymbolInternals::class)
    private fun listAnnotations(firAnnotations: List<FirAnnotation>): MutableList<FullyQualified>? {
        var annotations: MutableList<FullyQualified>? = null
        for (firAnnotation in firAnnotations) {
            val fir = firAnnotation.annotationTypeRef.toRegularClassSymbol(firSession)?.fir
            if (fir != null && isNotSourceRetention(fir.annotations)) {
                if (annotations == null) {
                    annotations = ArrayList()
                }
                val fq = TypeUtils.asFullyQualified(type(firAnnotation))
                if (fq != null) {
                    annotations.add(remapKotlinBuiltin(fq))
                }
            }
        }
        return annotations
    }

    @OptIn(SymbolInternals::class)
    private fun listAnnotations(javaAnnotations: Collection<JavaAnnotation>): List<FullyQualified>? {
        var annotations: MutableList<FullyQualified>? = null
        for (javaAnnotation: JavaAnnotation in javaAnnotations) {
            val fir = javaAnnotation.classId?.toSymbol(firSession)?.fir
            if (fir != null && isNotSourceRetention(fir.annotations)) {
                if (annotations == null) {
                    annotations = ArrayList()
                }
                val fq = TypeUtils.asFullyQualified(type(javaAnnotation))
                if (fq != null) {
                    annotations.add(remapKotlinBuiltin(fq))
                }
            }
        }
        return annotations
    }

    private fun isNotSourceRetention(annotations: List<FirAnnotation>): Boolean {
        for (ann in annotations) {
            if ("kotlin.annotation.Retention" == convertClassIdToFqn(ann.annotationTypeRef.coneType.classId)) {
                for (v in ann.argumentMapping.mapping.values) {
                    if (v is FirQualifiedAccessExpression && v.calleeReference is FirResolvedNamedReference && (v.calleeReference as FirResolvedNamedReference).name.asString() == "SOURCE") {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun mapKind(kind: ClassKind): FullyQualified.Kind {
        return when (kind) {
            ClassKind.INTERFACE -> FullyQualified.Kind.Interface
            ClassKind.ENUM_CLASS -> FullyQualified.Kind.Enum
            // ClassKind.ENUM_ENTRY is compiled to a class.
            ClassKind.ENUM_ENTRY -> FullyQualified.Kind.Class
            ClassKind.ANNOTATION_CLASS -> FullyQualified.Kind.Annotation
            else -> FullyQualified.Kind.Class
        }
    }

    private fun mapToFlagsBitmap(visibility: Visibility, modality: Modality?, isStatic: Boolean): Long {
        var bitMask: Long = 0
        when (visibility.name.lowercase()) {
            "public" -> bitMask += 1L
            "private", "private_to_this" -> bitMask += 1L shl 1
            "protected", "protected_and_package" -> bitMask += 1L shl 2
            "protected_static" -> {
                bitMask += 1L shl 2
                bitMask += 1L shl 3 // static
            }
            "internal", "package", "local" -> {}
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
        if (isStatic) {
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

    fun primitive(type: FirElement): Primitive {
        return when (type) {
            is FirLiteralExpression -> {
                when (type.kind) {
                    ConstantValueKind.Boolean -> Primitive.Boolean
                    ConstantValueKind.Byte, ConstantValueKind.UnsignedByte -> Primitive.Byte
                    ConstantValueKind.Char -> Primitive.Char
                    ConstantValueKind.Double -> Primitive.Double
                    ConstantValueKind.Float -> Primitive.Float
                    ConstantValueKind.Int, ConstantValueKind.IntegerLiteral,
                    ConstantValueKind.UnsignedInt, ConstantValueKind.UnsignedIntegerLiteral -> Primitive.Int

                    ConstantValueKind.Long, ConstantValueKind.UnsignedLong -> Primitive.Long
                    ConstantValueKind.Null -> Primitive.Null
                    ConstantValueKind.Short, ConstantValueKind.UnsignedShort -> Primitive.Short
                    ConstantValueKind.String -> Primitive.String
                    ConstantValueKind.Error -> Primitive.None
                    else -> throw UnsupportedOperationException("Unexpected constant value kind: ${type.kind}")
                }
            }

            else -> {
                Primitive.None
            }
        }
    }
}

internal fun FirBasedSymbol<*>.getContainingFile() =
    when (this) {
        is FirCallableSymbol<*> -> moduleData.session.firProvider.getFirCallableContainerFile(this)
        is FirClassLikeSymbol<*> -> moduleData.session.firProvider.getFirClassifierContainerFileIfAny(this)
        else -> null
    }
