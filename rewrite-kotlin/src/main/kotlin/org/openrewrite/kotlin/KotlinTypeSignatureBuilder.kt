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
import org.jetbrains.kotlin.fir.*
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

@Suppress("DuplicatedCode")
class KotlinTypeSignatureBuilder(private val firSession: FirSession, private val firFile: FirFile) :
    JavaTypeSignatureBuilder {
    private var typeVariableNameStack: MutableSet<String>? = null

    override fun signature(type: Any?): String {
        return signature(type, firFile)
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

            is FirAnonymousFunctionExpression -> {
                signature(type.anonymousFunction)
            }

            is FirBlock -> {
                // AssignmentOperationTest#augmentedAssignmentAnnotation
                // There is an issue in the KotlinTreeParserVisitor, PsiElementVisitor,
                // or no FIR element associated to the Kt that requested a type.
                // Example: AssignmentOperationTest#augmentedAssignmentAnnotation
                "{undefined}"
            }

            is FirAnonymousObject -> {
                if (type.typeParameters.isNotEmpty()) anonymousParameterizedSignature(type) else anonymousClassSignature(type)
            }

            is FirClass -> {
                if (type.typeParameters.isNotEmpty()) parameterizedSignature(type) else classSignature(type)
            }

            is FirErrorNamedReference -> {
                return type.name.asString()
            }

            is FirFile -> {
                fileSignature(type)
            }

            is FirFunction -> {
                methodSignature(type, parent)
            }

            is FirFunctionCall -> {
                methodCallSignature(type)
            }

            is FirJavaTypeRef -> {
                signature(type.type, parent)
            }

            is FirOuterClassTypeParameterRef -> {
                signature(type.symbol.fir)
            }

            is FirPackageDirective -> {
                type.packageFqName.asString()
            }

            is FirPropertyAccessExpression -> {
                signature(type.calleeReference)
            }

            is FirResolvedNamedReference -> {
                resolvedNameReferenceSignature(type, parent)
            }

            is FirResolvedTypeRef -> {
                signature(type.coneType)
            }

            is FirResolvedQualifier -> {
                if (type.typeArguments.isNotEmpty()) parameterizedSignature(type) else classSignature(type)
            }

            is FirStringConcatenationCall -> {
                signature(type.typeRef)
            }

            is FirSuperReference -> {
                signature(type.superTypeRef)
            }

            is FirTypeParameter -> {
                typeParameterSignature(type)
            }

            is FirTypeProjection -> {
                typeProjectionSignature(type)
            }

            is FirSafeCallExpression -> {
                signature(type.selector)
            }

            is FirVariable -> {
                variableSignature(type, parent)
            }

            is FirVariableAssignment -> {
                signature(type.lValue.typeRef, parent)
            }

            is FirExpression -> {
                signature(type.typeRef)
            }

            is JavaElement -> {
                javaElement(type)
            }

            is FirResolvedImport -> {
                resolveImport(type)
            }

            is FqName -> {
                type.asString()
            }

            else -> "{undefined}"
        }
    }

    private fun anonymousClassSignature(type: FirAnonymousObject): String {
        val sig = StringBuilder(type.symbol.classId.asFqNameString())
        for (supertype in type.superTypeRefs) {
            sig.append(signature(supertype))
        }
        return sig.toString()
    }

    private fun anonymousParameterizedSignature(type: FirAnonymousObject): String {
        val sig = StringBuilder(classSignature(type))
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeParameters) {
            joiner.add(signature(tp, type))
        }
        return sig.append(joiner).toString()
    }

    override fun arraySignature(type: Any): String {
        throw UnsupportedOperationException("This should never happen.")
    }

    override fun classSignature(type: Any): String {
        return when (type) {
            is ConeClassLikeType -> convertClassIdToFqn(type.classId)
            is ConeFlexibleType -> convertClassIdToFqn(type.lowerBound.classId)
            is ConeTypeParameterType -> signature(type.type)
            is FirClass -> convertClassIdToFqn(type.classId)
            is FirFile -> fileSignature(type)
            is FirResolvedTypeRef -> classSignature(type.type)
            is FirResolvedQualifier -> convertClassIdToFqn(type.classId)
            else -> {
                throw UnsupportedOperationException("Unsupported class type: ${type.javaClass.name}")
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun coneTypeProjectionSignature(type: ConeTypeProjection): String {
        return when (type) {
            is ConeKotlinTypeProjectionIn -> "Generic{? super ${signature(type.type)}}"
            is ConeKotlinTypeProjectionOut -> "Generic{? extends ${signature(type.type)}}"
            is ConeStarProjection -> "Generic{?}"
            is ConeTypeParameterType -> {
                signature(type.lookupTag.typeParameterSymbol.fir)
            }

            is ConeDefinitelyNotNullType -> {
                if (type.typeArguments.isNotEmpty()) {
                    throw UnsupportedOperationException("Unsupported ConeDefinitelyNotNullType with type arguments: ${type.javaClass.name}")
                }
                signature(type.original)
            }

            is ConeCapturedType -> {
                if (type.typeArguments.isNotEmpty()) {
                    throw UnsupportedOperationException("Unsupported ConeCapturedType with type arguments: ${type.javaClass.name}")
                }
                return "Generic{?}"
            }

            is ConeIntersectionType -> {
                val sig = StringBuilder()
                sig.append("Generic{")
                val boundSigs = StringJoiner(" & ")
                for (coneKotlinType in type.intersectedTypes) {
                    boundSigs.add(signature(coneKotlinType))
                }
                sig.append(boundSigs)
                sig.append("}")
                sig.toString()
            }

            else -> throw UnsupportedOperationException("Unsupported ConeTypeProjection ${type.javaClass.name}")
        }
    }

    override fun genericSignature(type: Any): String {
        throw UnsupportedOperationException("Call type specific generic signature methods.")
    }

    override fun parameterizedSignature(type: Any): String {
        return when (type) {
            is ConeClassLikeType -> parameterizedSignature(type)
            is FirRegularClass -> parameterizedSignature(type)
            is FirResolvedQualifier -> parameterizedSignature(type)
            else -> {
                throw UnsupportedOperationException("Unsupported parameterized FirClass type: ${type.javaClass.name}")
            }
        }
    }

    private fun parameterizedSignature(type: FirRegularClass): String {
        val s = StringBuilder(classSignature(type))
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeParameters) {
            joiner.add(signature(tp, type))
        }
        return s.append(joiner).toString()
    }

    private fun parameterizedSignature(type: ConeClassLikeType): String {
        val s = StringBuilder(classSignature(type))
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeArguments) {
            joiner.add(signature(tp, type.toFirResolvedTypeRef()))
        }
        return s.append(joiner).toString()
    }

    @OptIn(SymbolInternals::class)
    fun parameterizedSignature(type: FirResolvedQualifier): String {
        val s = StringBuilder(classSignature(type))
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeArguments) {
            joiner.add(signature(tp, type.symbol?.fir))
        }
        return s.append(joiner).toString()
    }

    override fun primitiveSignature(type: Any): String {
        throw UnsupportedOperationException("Call primitive instead")
    }

    @OptIn(SymbolInternals::class)
    fun methodSignature(
        function: FirFunction,
        parent: Any?
    ): String {
        val clazz = when {
            function.symbol is FirConstructorSymbol -> classSignature(function.returnTypeRef)
            function.dispatchReceiverType != null -> classSignature(function.dispatchReceiverType!!)
            function.symbol.getOwnerLookupTag() != null && function.symbol.getOwnerLookupTag()!!
                .toFirRegularClass(firSession) != null -> {
                classSignature(function.symbol.getOwnerLookupTag()!!.toFirRegularClass(firSession)!!)
            }

            parent is FirClass -> classSignature(parent)
            else -> fileSignature(firFile)
        }
        val sig = StringBuilder(clazz)
        when {
            function.symbol is FirConstructorSymbol -> sig.append("{name=<constructor>,return=${signature(function.returnTypeRef)}")
            else -> sig.append("{name=${methodName(function)},return=${signature(function.returnTypeRef)}")
        }
        sig.append(",parameters=${methodArgumentSignature(function)}}")
        return sig.toString()
    }

    private fun methodArgumentSignature(function: FirFunction): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        if (function.receiverParameter != null) {
            genericArgumentTypes.add(signature(function.receiverParameter!!.typeRef))
        }
        for (p in function.valueParameters) {
            genericArgumentTypes.add(signature(p.returnTypeRef, function))
        }
        return genericArgumentTypes.toString()
    }

    @OptIn(SymbolInternals::class)
    fun methodCallSignature(function: FirFunctionCall): String {
        val sym = function.calleeReference.toResolvedBaseSymbol() ?: return "{undefined}"
        var declaringSig: String? = null
        if (function.calleeReference is FirResolvedNamedReference &&
            (function.calleeReference as FirResolvedNamedReference).resolvedSymbol is FirNamedFunctionSymbol
        ) {
            val resolvedSymbol =
                (function.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirNamedFunctionSymbol
            if (resolvedSymbol.dispatchReceiverType is ConeClassLikeType) {
                declaringSig = signature(resolvedSymbol.dispatchReceiverType)
            } else if (resolvedSymbol.containingClassLookupTag() != null &&
                resolvedSymbol.containingClassLookupTag()!!.toFirRegularClass(firSession) != null
            ) {
                declaringSig = signature(resolvedSymbol.containingClassLookupTag()!!.toFirRegularClass(firSession))
            } else if (resolvedSymbol.origin == FirDeclarationOrigin.Library) {
                if (resolvedSymbol.fir.containerSource is JvmPackagePartSource) {
                    val source: JvmPackagePartSource? = resolvedSymbol.fir.containerSource as JvmPackagePartSource?
                    if (source != null) {
                        declaringSig = if (source.facadeClassName != null) {
                            (source.facadeClassName as JvmClassName).fqNameForTopLevelClassMaybeWithDollars.asString()
                        } else {
                            source.className.fqNameForTopLevelClassMaybeWithDollars.asString()
                        }
                    }
                } else if (!resolvedSymbol.fir.origin.generated &&
                    !resolvedSymbol.fir.origin.fromSupertypes &&
                    !resolvedSymbol.fir.origin.fromSource
                ) {
                    declaringSig = "kotlin.Library"
                }
            } else {
                declaringSig = signature(resolvedSymbol.getContainingFile())
            }
        } else if (sym is FirFunctionSymbol<*>) {
            declaringSig = signature(function.typeRef)
        }
        if (declaringSig == null) {
            declaringSig = signature(firFile)
        }

        val sig = StringBuilder(declaringSig)
        when {
            sym is FirConstructorSymbol ||
                    sym is FirSyntheticFunctionSymbol && sym.origin == FirDeclarationOrigin.SamConstructor -> sig.append("{name=<constructor>,return=${signature(function.typeRef)}")
            sym is FirNamedFunctionSymbol -> {
                sig.append("{name=${sym.name.asString()}")
                sig.append(",return=${signature(function.typeRef)}")
            }

            else -> throw UnsupportedOperationException("Unsupported function calleeReference: ${function.calleeReference.name}")
        }

        sig.append(",parameters=${methodCallArgumentSignature(function)}}")
        return sig.toString()
    }

    @OptIn(SymbolInternals::class)
    private fun methodCallArgumentSignature(function: FirFunctionCall): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        if (function.toResolvedCallableSymbol()?.receiverParameter != null) {
            genericArgumentTypes.add(signature(function.toResolvedCallableSymbol()?.receiverParameter!!.typeRef))
        }
        val mapNames = function.arguments.any { it is FirNamedArgumentExpression }
        var args: MutableMap<String, FirNamedArgumentExpression>? = null
        if (mapNames) {
            for (a in function.arguments) {
                if (args == null) {
                    args = HashMap(function.arguments.size)
                }
                if (a is FirNamedArgumentExpression) {
                    args[a.name.asString()] = a
                }
            }
        }

        val valueParams = (function.toResolvedCallableSymbol()?.fir as FirFunction).valueParameters
        for ((index, p) in valueParams.withIndex()) {
            val sig = signature(p.returnTypeRef, function)
            if (sig.startsWith("Generic{")) {
                if (mapNames && args != null && args.containsKey(p.name.asString())) {
                    genericArgumentTypes.add(signature(args[p.name.asString()]!!.typeRef, function))
                } else if (index < function.arguments.size) {
                    genericArgumentTypes.add(signature((function.arguments[index]).typeRef, function))
                }
            } else {
                genericArgumentTypes.add(sig)
            }
        }
        return genericArgumentTypes.toString()
    }

    private fun resolveImport(type: FirResolvedImport): String {
        return signature(type.importedFqName) + (if (type.isAllUnder) ".*" else "")
    }

    @OptIn(SymbolInternals::class)
    private fun resolvedNameReferenceSignature(type: FirResolvedNamedReference, parent: Any?): String {
        return when (val sym = type.resolvedSymbol) {
            is FirBackingFieldSymbol -> signature(sym.fir, parent)
            is FirConstructorSymbol -> signature(sym.fir, parent)
            is FirEnumEntrySymbol -> signature(sym.fir, parent)
            is FirFieldSymbol -> signature(sym.fir, parent)
            is FirNamedFunctionSymbol -> signature(sym.fir, parent)
            is FirPropertySymbol -> signature(sym.fir, parent)
            is FirValueParameterSymbol -> signature(sym.fir, parent)
            else -> {
                throw UnsupportedOperationException("Unsupported FirResolvedNamedReference type: ${type.javaClass.name}")
            }
        }
    }

    private fun typeParameterSignature(type: FirTypeParameter): String {
        val name = type.name.asString()
        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }
        if (!typeVariableNameStack!!.add(name)) {
            return "Generic{$name}"
        }
        val s = StringBuilder("Generic{").append(name)
        val boundSigs = StringJoiner(" & ")
        for (bound in type.bounds) {
            if (bound !is FirImplicitNullableAnyTypeRef) {
                boundSigs.add(signature(bound))
            }
        }
        val boundSigStr = boundSigs.toString()
        if (boundSigStr.isNotEmpty()) {
            s.append(" extends ").append(boundSigStr)
        }
        typeVariableNameStack!!.remove(name)
        return s.append("}").toString()
    }

    private fun typeProjectionSignature(type: FirTypeProjection): String {
        return when (type) {
            is FirTypeProjectionWithVariance -> {
                when (type.variance) {
                    Variance.INVARIANT -> signature(type.typeRef)
                    Variance.IN_VARIANCE -> {
                        return "Generic{? super " + signature(type.typeRef)
                    }

                    Variance.OUT_VARIANCE -> {
                        return "Generic{? extends " + signature(type.typeRef)
                    }
                }
            }
            is FirStarProjection -> {
                return "Generic{?}"
            }

            else -> throw UnsupportedOperationException("Unsupported FirTypeProjection: ${type.javaClass.name}")
        }
    }

    fun variableSignature(property: FirVariable, parent: Any?): String {
        val sig = StringBuilder()
        val owner = when {
            property.dispatchReceiverType is ConeClassLikeType && property.dispatchReceiverType!!.toRegularClassSymbol(
                firSession
            ) != null -> {
                convertClassIdToFqn(property.dispatchReceiverType!!.toRegularClassSymbol(firSession)!!.classId)
            }

            property.symbol.callableId.classId != null -> {
                var oSig = convertClassIdToFqn(property.symbol.callableId.classId)
                if (oSig.contains("<")) {
                    oSig = oSig.substring(0, oSig.indexOf('<'))
                }
                oSig
            }

            parent is FirFunction -> methodSignature(parent, null)
            parent is FirFile -> fileSignature(parent)
            parent is FirClass -> classSignature(parent)
            else -> fileSignature(firFile)
        }
        sig.append(owner)
        sig.append("{name=${variableName(property.name.asString())}")
        sig.append(",type=${signature(property.returnTypeRef)}}")
        return sig.toString()
    }

    @OptIn(SymbolInternals::class)
    private fun javaElement(type: JavaElement): String {
        return when (type) {
            is JavaArrayType -> javaArraySignature(type)
            is JavaPrimitiveType -> javaPrimitiveSignature(type)
            // The classifier is evaluated separately, because the BinaryJavaClass may have type parameters.
            is JavaClassifierType -> if (type.typeArguments.isNotEmpty()) javaParameterizedSignature(type) else signature(
                type.classifier
            )

            is BinaryJavaAnnotation -> signature(type.classId.toSymbol(firSession)?.fir)
            is BinaryJavaClass -> if (type.typeParameters.isNotEmpty()) javaParameterizedSignature(type) else javaClassSignature(
                type
            )

            is BinaryJavaTypeParameter -> javaTypeParameterSignature(type)
            is JavaWildcardType -> javaWildCardSignature(type)
            is JavaValueParameter -> signature(type.type)
            else -> throw UnsupportedOperationException("Unsupported JavaElement: ${type.javaClass.name}")
        }
    }

    private fun javaArraySignature(type: JavaArrayType): String {
        return "${signature(type.componentType)}[]"
    }

    private fun javaClassSignature(type: BinaryJavaClass): String {
        return type.fqName.asString()
    }

    private fun javaClassSignature(type: JavaClass): String {
        if (type.fqName == null) {
            return "{undefined}"
        }
        return when {
            type.outerClass != null -> "${javaClassSignature(type.outerClass!!)}${'$'}${type.name}"
            else -> type.fqName!!.asString()
        }
    }

    fun javaConstructorSignature(method: JavaConstructor): String {
        val sig = StringBuilder(javaClassSignature(method.containingClass))
        sig.append("{name=<constructor>,return=${signature(method.containingClass)}")
        sig.append(",parameters=${javaMethodArgumentSignature(method.valueParameters)}}")
        return sig.toString()
    }

    fun javaMethodSignature(method: JavaMethod): String {
        val sig = StringBuilder(javaClassSignature(method.containingClass))
        sig.append("{name${method.name.asString()}")
        sig.append(",return=${signature(method.returnType)}")
        sig.append(",parameters=${javaMethodArgumentSignature(method.valueParameters)}}")
        return sig.toString()
    }

    private fun javaMethodArgumentSignature(valueParameters: List<JavaValueParameter>): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        for (valueParameter in valueParameters) {
            genericArgumentTypes.add(signature(valueParameter))
        }
        return genericArgumentTypes.toString()
    }

    private fun javaParameterizedSignature(type: BinaryJavaClass): String {
        val sig = StringBuilder(type.fqName.asString())
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeParameters) {
            joiner.add(signature(tp, type))
        }
        return sig.append(joiner).toString()
    }

    private fun javaParameterizedSignature(type: JavaClassifierType): String {
        val sig = StringBuilder(type.classifierQualifiedName)
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeArguments) {
            joiner.add(signature(tp, type))
        }
        return sig.append(joiner).toString()
    }

    private fun javaPrimitiveSignature(type: JavaPrimitiveType): String {
        return when (type.type) {
            PrimitiveType.BOOLEAN -> JavaType.Primitive.Boolean.className
            PrimitiveType.BYTE -> JavaType.Primitive.Byte.className
            PrimitiveType.CHAR -> JavaType.Primitive.Char.className
            PrimitiveType.DOUBLE -> JavaType.Primitive.Double.className
            PrimitiveType.FLOAT -> JavaType.Primitive.Float.className
            PrimitiveType.INT -> JavaType.Primitive.Int.className
            PrimitiveType.LONG -> JavaType.Primitive.Long.className
            PrimitiveType.SHORT -> JavaType.Primitive.Short.className
            null -> JavaType.Primitive.Void.className
        }
    }

    private fun javaTypeParameterSignature(type: BinaryJavaTypeParameter): String {
        val name = type.name.asString()
        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }
        if (!typeVariableNameStack!!.add(name)) {
            return "Generic{$name}"
        }
        val sig = StringBuilder("Generic{").append(name)
        for (b in type.upperBounds) {
            if (b.classifierQualifiedName != "java.lang.Object") {
                sig.append(signature(b))
            }
        }
        typeVariableNameStack!!.remove(name)
        return sig.append("}").toString()
    }

    private fun javaWildCardSignature(type: JavaWildcardType): String {
        val sig = StringBuilder("Generic{?")
        if (type.bound != null) {
            if (type.isExtends) {
                sig.append(" extends ")
            } else {
                sig.append(" super ")
            }
            sig.append(signature(type.bound))
        }
        return sig.append("}").toString()
    }

    fun javaVariableSignature(javaField: JavaField): String {
        val sig = StringBuilder(signature(javaField.containingClass))
        var owner = signature(javaField.containingClass)
        if (owner.contains("<")) {
            owner = owner.substring(0, owner.indexOf('<'))
        }
        sig.append(owner)
        sig.append("{name=${variableName(javaField.name.asString())}")
        sig.append(",type=${signature(javaField.type)}}")
        return sig.toString()
    }

    companion object {
        fun convertClassIdToFqn(classId: ClassId?): String {
            return convertKotlinFqToJavaFq(classId.toString())
        }

        fun fileSignature(file: FirFile): String {
            val sig = StringBuilder()
            if (file.packageFqName.asString().isNotEmpty()) {
                sig.append("${file.packageFqName.asString()}.")
            }
            sig.append(file.name.replace("/", ".").replace("\\", ".").replace(".kt", "Kt"))
            return sig.toString()
        }

        fun convertKotlinFqToJavaFq(kotlinFqn: String): String {
            val cleanedFqn = kotlinFqn
                .replace(".", "$")
                .replace("/", ".")
                .replace("?", "")
            return if (cleanedFqn.startsWith(".")) cleanedFqn.substring(1) else cleanedFqn
        }

        fun methodName(function: FirFunction): String {
            return when (function) {
                is FirPropertyAccessor -> when {
                    function.isGetter -> "get"
                    function.isSetter -> "set"
                    else -> function.symbol.name.asString()
                }
                else -> function.symbol.name.asString()
            }
        }

        fun variableName(name: String): String {
            return when (name) {
                "<unused var>" -> "_"
                else -> name
            }
        }
    }
}
