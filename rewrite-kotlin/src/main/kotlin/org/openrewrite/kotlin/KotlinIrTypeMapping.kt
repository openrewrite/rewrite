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
@file:Suppress("DEPRECATION_ERROR")
package org.openrewrite.kotlin

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionWithLateBindingImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.types.Variance
import org.openrewrite.java.JavaTypeMapping
import org.openrewrite.java.internal.JavaTypeFactory
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.GenericTypeVariable
import org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*
import org.openrewrite.java.tree.TypeUtils

@Suppress("unused", "UNUSED_PARAMETER")
class KotlinIrTypeMapping(private val typeFactory: JavaTypeFactory) : JavaTypeMapping<Any> {
    private val signatureBuilder: KotlinTypeIrSignatureBuilder = KotlinTypeIrSignatureBuilder()

    // TEMP: method to map types in an IrFile.
    fun type(irFile: IrFile) {
        for (ann in irFile.annotations) {
            type(ann)
        }
        for (dec in irFile.declarations) {
            if (dec is IrScript) {
                for (irDec in dec.statements) {
                    type(irDec)
                }
            } else {
                type(dec)
            }
        }
    }

    override fun type(type: Any?): JavaType {
        if (type == null || type is IrErrorType) {
            return JavaType.Unknown.getInstance()
        }

        if (type is IrClassifierSymbol) {
            return type(type.owner)
        }

        val signature = signatureBuilder.signature(type)
        val existing: JavaType? = typeFactory.get(signature)
        if (existing != null) {
            return existing
        }

        val baseType = if (type is IrSimpleType) type.classifier.owner else type
        when (baseType) {
            is IrFile -> {
                return fileType(signature)
            }

            is IrClass -> {
                val useSimpleType = (type is IrSimpleType && (type.arguments.isNotEmpty()))
                return classType(if (useSimpleType) type else baseType, signature)
            }

            is IrCall -> {
                return methodDeclarationType(baseType.symbol.owner, signature)
            }

            is IrClassReference -> {
                return type(baseType.type)
            }

            is IrConst -> {
                return primitive(baseType)
            }

            is IrConstructorCall -> {
                return methodInvocationType(baseType, signature)
            }

            is IrExternalPackageFragment -> {
                return externalPackageFragment(signature)
            }

            is IrField -> {
                return variableType(baseType, signature)
            }

            is IrFunction -> {
                return methodDeclarationType(baseType, signature)
            }

            is IrFunctionReference -> {
                return type(baseType.symbol.owner)
            }

            is IrGetValue -> {
                return type(baseType.type)
            }

            is IrProperty -> {
                return variableType(baseType, signature)
            }

            is IrPropertyReference -> {
                return variableType(baseType.symbol.owner, signature)
            }

            is IrTypeAlias -> {
                return alias(baseType, signature)
            }

            is IrTypeOperatorCall -> {
                return methodInvocationType(baseType, signature)
            }

            is IrTypeParameter -> {
                return generic(baseType, signature)
            }

            is IrTypeProjection, is IrStarProjection -> {
                return typeProjection(baseType, signature)
            }

            is IrValueParameter -> {
                return variableType(baseType, signature)
            }

            is IrVariable -> {
                return variableType(baseType, signature)
            }

            else -> {
                throw UnsupportedOperationException("Unsupported type: ${type.javaClass}")
            }
        }
    }

    private fun externalPackageFragment(signature: String): JavaType {
        val packageFragment = JavaType.ShallowClass.build(signature)
        typeFactory.put(signature, packageFragment)
        return packageFragment
    }

    private fun alias(type: IrTypeAlias, signature: String): JavaType {
        val aliased = type(type.expandedType)
        typeFactory.put(signature, aliased)
        return aliased
    }

    private fun fileType(signature: String): JavaType {
        val existing = typeFactory.get<JavaType.FullyQualified>(signature)
        if (existing != null) {
            return existing
        }
        val fileType = JavaType.ShallowClass.build(signature)
        typeFactory.put(signature, fileType)
        return fileType
    }

    private fun classType(type: Any, signature: String): JavaType {
        if (type !is IrSimpleType && type !is IrClass) {
            throw UnsupportedOperationException("Unexpected classType: " + type.javaClass)
        }
        val irClass = type as? IrClass ?: (type as IrSimpleType).classifier.owner as IrClass
        val fqn: String = signatureBuilder.classSignature(irClass)
        val cachedAtFqn: JavaType.FullyQualified? = typeFactory.get(fqn)
        val clazz: JavaType.Class = if (cachedAtFqn is JavaType.Parameterized) {
            cachedAtFqn.type as JavaType.Class
        } else if (cachedAtFqn is JavaType.Class) {
            cachedAtFqn
        } else {
            typeFactory.computeClass(fqn, fqn, mapToFlagsBitmap(irClass.visibility, irClass.modality, irClass.kind),
                mapKind(irClass.kind), irClass) { c ->
                var supertype: JavaType.FullyQualified? = null
                var interfaceTypes: MutableList<IrType>? = null
                if (signature != "java.lang.Object") {
                    for (sType in irClass.superTypes) {
                        when (val classifier: IrClassifierSymbol? = sType.classifierOrNull) {
                            is IrClassSymbol -> {
                                when (classifier.owner.kind) {
                                    ClassKind.CLASS -> {
                                        supertype = TypeUtils.asFullyQualified(type(sType))
                                    }

                                    ClassKind.INTERFACE -> {
                                        if (interfaceTypes == null) {
                                            interfaceTypes = ArrayList()
                                        }
                                        interfaceTypes!!.add(sType)
                                    }

                                    else -> {
                                    }
                                }
                            }

                            else -> {
                                throw UnsupportedOperationException("Unexpected classifier symbol: " + classifier?.javaClass)
                            }
                        }
                    }
                }
                var owner: JavaType.FullyQualified? = null
                if (irClass.parent is IrClass) {
                    owner = TypeUtils.asFullyQualified(type(irClass.parent))
                    if (owner is JavaType.Parameterized) {
                        owner = owner.type
                    }
                } else if (irClass.parent is IrFunction) {
                    var parent: IrDeclarationParent = irClass.parent
                    while (parent !is IrClass && parent !is IrFile) {
                        if (parent is IrDeclaration) {
                            parent = parent.parent
                        } else {
                            break
                        }
                    }
                    owner = TypeUtils.asFullyQualified(type(parent))
                }

                var fields: MutableList<JavaType.Variable>? = null
                for (property: IrProperty in irClass.properties) {
                    if (fields == null) {
                        fields = ArrayList(irClass.properties.toList().size)
                    }
                    val vt = variableType(property)
                    fields!!.add(vt)
                }

                var methods: MutableList<JavaType.Method>? = null
                for (function: IrFunction in irClass.constructors) {
                    if (methods == null) {
                        methods = ArrayList(irClass.functions.toList().size)
                    }
                    val mt = methodDeclarationType(function)
                    if (mt != null) {
                        methods!!.add(mt)
                    }
                }

                for (function: IrFunction in irClass.functions) {
                    if (methods == null) {
                        methods = ArrayList(irClass.functions.toList().size)
                    }
                    val mt = methodDeclarationType(function)
                    if (mt != null) {
                        methods!!.add(mt)
                    }
                }

                var interfaces: MutableList<JavaType.FullyQualified>? = null
                if (!interfaceTypes.isNullOrEmpty()) {
                    interfaces = ArrayList(interfaceTypes!!.size)
                    for (iType: IrType in interfaceTypes!!) {
                        val javaType = TypeUtils.asFullyQualified(type(iType))
                        if (javaType != null) {
                            interfaces.add(javaType)
                        }
                    }
                }
                var typeParameters: MutableList<JavaType>? = null
                if (irClass.typeParameters.isNotEmpty()) {
                    typeParameters = ArrayList(irClass.typeParameters.size)
                    for (tParam in irClass.typeParameters) {
                        typeParameters!!.add(type(tParam))
                    }
                }
                c.unsafeSet(typeParameters, supertype, owner, listAnnotations(irClass.annotations), interfaces, fields, methods)
            }
        }

        if (irClass.typeParameters.isNotEmpty()) {
            return typeFactory.computeParameterized(signature, type) { pt ->
                pt.unsafeSet(clazz, null as List<JavaType>?)
                val typeParameters: MutableList<JavaType> = ArrayList(irClass.typeParameters.size)
                val params = if (type is IrSimpleType) type.arguments else (type as IrClass).typeParameters
                for (tp in params) {
                    typeParameters.add(type(tp))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
        }
        return clazz
    }

    private fun generic(type: IrTypeParameter, signature: String): JavaType {
        val name = type.name.asString()
        return typeFactory.computeGenericTypeVariable(signature, name, INVARIANT, type) { gtv ->
            var bounds: MutableList<JavaType>? = null
            if (type.isReified) {
                throw UnsupportedOperationException("Add support for reified generic types.")
            }
            for (bound: IrType in type.superTypes) {
                if (isNotAny(bound) && isNotObject(bound)) {
                    if (bounds == null) {
                        bounds = ArrayList()
                    }
                    bounds!!.add(type(bound))
                }
            }
            gtv.unsafeSet(name, if (bounds == null) INVARIANT else COVARIANT, bounds)
        }
    }

    // Drop `java.lang.Object` bounds to match the Java parser's handling of
    // unbounded type parameters. Kotlin's IR surfaces Java-origin `<T>` as
    // `<T : Object>` (the bytecode's explicit bound); keeping that produces
    // `Generic{T extends java.lang.Object}` which never dedups against the
    // Java parser's unbounded `Generic{T}`.
    private fun isNotObject(type: IrType): Boolean {
        return !(type.classifierOrNull != null && type.classifierOrNull!!.owner is IrClass && "java.lang.Object" == (type.classifierOrNull!!.owner as IrClass).kotlinFqName.asString())
    }

    fun methodDeclarationType(function: IrFunction?): JavaType.Method? {
        if (function == null) {
            return null
        }
        val signature = signatureBuilder.methodSignature(function)
        return methodDeclarationType(function, signature)
    }

    private fun methodDeclarationType(function: IrFunction, signature: String): JavaType.Method {
        val regularParams = function.parameters.filter { it.kind == IrParameterKind.Regular }
        val paramNamesArr: kotlin.Array<String>? = if (regularParams.isEmpty()) null else
            kotlin.Array(regularParams.size) { regularParams[it].name.asString() }
        val modality = when (function) {
            is IrFunctionImpl -> function.modality
            is IrFunctionWithLateBindingImpl -> function.modality
            is Fir2IrLazySimpleFunction -> function.modality
            is IrConstructorImpl, is Fir2IrLazyConstructor -> null
            else -> throw UnsupportedOperationException("Unsupported IrFunction type: " + function.javaClass)
        }
        var methodFlags = mapToFlagsBitmap(function.visibility, modality)
        val parent = function.parent
        if (parent is IrClass && parent.kind == ClassKind.INTERFACE &&
            modality != null && modality != Modality.ABSTRACT &&
            function !is IrConstructor) {
            methodFlags = methodFlags or (1L shl 43) // Default flag
        }
        if (function is IrSimpleFunction && function.dispatchReceiverParameter == null &&
            function !is IrConstructor && parent is IrClass && !parent.isCompanion) {
            methodFlags = methodFlags or (1L shl 3) // Static flag
        }
        val name = if (function is IrConstructor) "<constructor>" else function.name.asString()
        return typeFactory.computeMethod(signature, methodFlags, name, paramNamesArr, null, null, function) { method ->
            var declaringType = when (val irParent = function.parent) {
                is IrField -> TypeUtils.asFullyQualified(type(irParent.parent))
                else -> TypeUtils.asFullyQualified(type(irParent))
            }

            if (declaringType is JavaType.Parameterized) {
                declaringType = declaringType.type
            }
            val returnType = type(function.returnType)
            val extReceiver = function.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            val paramTypes: MutableList<JavaType>? =
                if (regularParams.isNotEmpty() || extReceiver != null)
                    ArrayList(regularParams.size + (if (extReceiver != null) 1 else 0))
                else null
            if (extReceiver != null) {
                paramTypes!!.add(type(extReceiver.type))
            }
            for (param: IrValueParameter in regularParams) {
                paramTypes!!.add(type(param.type))
            }
            method.unsafeSet(declaringType,
                (if (function is IrConstructor) declaringType else returnType) ?: JavaType.Unknown.getInstance(),
                paramTypes, null, listAnnotations(function.annotations)
            )
        }
    }

    fun methodInvocationType(type: IrFunctionAccessExpression?): JavaType.Method? {
        if (type == null) {
            return null
        }
        val signature = signatureBuilder.methodSignature(type)
        return when (type) {
            is IrConstructorCall -> methodInvocationType(type, signature)
            is IrCall -> methodInvocationType(type, signature)
            else -> throw UnsupportedOperationException("Unsupported methodInvocationType: " + type.javaClass)
        }
    }

    fun methodInvocationType(type: IrCall, signature: String): JavaType.Method {
        val ownerRegularParams = type.symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }
        val paramNamesArr: kotlin.Array<String> = kotlin.Array(ownerRegularParams.size) { ownerRegularParams[it].name.asString() }
        val flags = mapToFlagsBitmap(type.symbol.owner.visibility)
        val name = type.symbol.owner.name.asString()
        return typeFactory.computeMethod(signature, flags, name, paramNamesArr, null, null, type) { method ->
            var declaringType = TypeUtils.asFullyQualified(type(type.symbol.owner.parent))
            if (declaringType is JavaType.Parameterized) {
                declaringType = declaringType.type
            }
            val returnType = type(type.symbol.owner.returnType)
            val ownerParams = type.symbol.owner.parameters
            val nonDispatchParams = ownerParams.filter { it.kind != IrParameterKind.DispatchReceiver }
            val paramTypes: MutableList<JavaType>? =
                if (nonDispatchParams.isNotEmpty()) ArrayList(nonDispatchParams.size) else null
            for ((index, param) in ownerParams.withIndex()) {
                if (param.kind == IrParameterKind.DispatchReceiver) continue
                val arg = type.arguments.getOrNull(index)
                if (arg != null) {
                    paramTypes!!.add(type(arg.type))
                }
            }
            method.unsafeSet(declaringType,
                returnType,
                paramTypes, null, listAnnotations(type.symbol.owner.annotations)
            )
        }
    }

    fun methodInvocationType(type: IrConstructorCall, signature: String): JavaType.Method {
        val ownerRegularParams = type.symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }
        val paramNamesArr: kotlin.Array<String> = kotlin.Array(ownerRegularParams.size) { ownerRegularParams[it].name.asString() }
        val flags = mapToFlagsBitmap(type.symbol.owner.visibility)
        return typeFactory.computeMethod(signature, flags, "<constructor>", paramNamesArr, null, null, type) { method ->
            var declaringType = TypeUtils.asFullyQualified(type(type.symbol.owner.parent))
            if (declaringType is JavaType.Parameterized) {
                declaringType = declaringType.type
            }
            val returnType = declaringType
            val ownerParams = type.symbol.owner.parameters
            val nonDispatchParams = ownerParams.filter { it.kind != IrParameterKind.DispatchReceiver }
            val paramTypes: MutableList<JavaType>? =
                if (nonDispatchParams.isNotEmpty()) ArrayList(nonDispatchParams.size) else null
            for ((index, param) in ownerParams.withIndex()) {
                if (param.kind == IrParameterKind.DispatchReceiver) continue
                val arg = type.arguments.getOrNull(index)
                if (arg != null) {
                    paramTypes!!.add(type(arg.type))
                }
            }
            method.unsafeSet(declaringType,
                returnType ?: JavaType.Unknown.getInstance(),
                paramTypes, null, listAnnotations(type.symbol.owner.annotations)
            )
        }
    }

    private fun methodInvocationType(type: IrTypeOperatorCall, signature: String): JavaType {
        val paramNames: MutableList<String> = ArrayList(0) // FIXME init cap

//        for (v in type.symbol.owner.valueParameters) {
//            paramNames.add(v.name.asString())
//        }
        val method = JavaType.Method(
            null,
            0, // FIXME
            null,
            "<constructor>",
            null,
            paramNames,
            null,
            null,
            null,
            null,
            null
        )
        return method
    }

    fun primitive(type: Any?): JavaType.Primitive {
        return when (type) {
            is IrConst -> {
                val irType = type.type
                when {
                    irType.isInt() -> JavaType.Primitive.Int
                    irType.isBoolean() -> JavaType.Primitive.Boolean
                    irType.isByte() -> JavaType.Primitive.Byte
                    irType.isChar() -> JavaType.Primitive.Char
                    irType.isDouble() -> JavaType.Primitive.Double
                    irType.isFloat() -> JavaType.Primitive.Float
                    irType.isLong() -> JavaType.Primitive.Long
                    irType.isShort() -> JavaType.Primitive.Short
                    irType.isString() -> JavaType.Primitive.String
                    type.value == null -> JavaType.Primitive.Null
                    else -> JavaType.Primitive.None
                }
            }
            else -> {
                // TODO: make sure this isn't visited after StringTemplateExpressions are fixed.
                JavaType.Primitive.None
            }
        }
    }

    private fun typeProjection(type: Any, signature: String): JavaType {
        val variance = when (type) {
            is IrTypeProjection -> if (type.variance == Variance.OUT_VARIANCE) COVARIANT else CONTRAVARIANT
            is IrStarProjection -> INVARIANT
            else -> throw UnsupportedOperationException("Unexpected type projection: " + type.javaClass)
        }
        return typeFactory.computeGenericTypeVariable(signature, "?", variance, type) { gtv ->
            val bounds: List<JavaType>? = if (type is IrTypeProjection) listOf(type(type.type)) else null
            gtv.unsafeSet("?", variance, bounds)
        }
    }

    fun variableType(property: IrProperty): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(property)
        return variableType(property, signature)
    }

    private fun variableType(property: IrProperty, signature: String): JavaType.Variable {
        return typeFactory.computeVariable(signature, 0, property.name.asString(), property) { variable ->
            val annotations = listAnnotations(property.annotations)
            var owner = type(property.parent)
            if (owner is JavaType.Parameterized) {
                owner = owner.type
            }
            val typeRef = if (property.getter != null) {
                type(property.getter!!.returnType)
            } else if (property.backingField != null) {
                type(property.backingField!!.type)
            } else {
                throw UnsupportedOperationException("Unsupported typeRef for property: $signature")
            }
            variable.unsafeSet(owner, typeRef, annotations)
        }
    }

    fun variableType(variable: IrField): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(variable)
        return variableType(variable, signature)
    }

    private fun variableType(variable: IrField, signature: String): JavaType.Variable {
        return typeFactory.computeVariable(signature, 0, variable.name.asString(), variable) { vt ->
            val annotations = listAnnotations(variable.annotations)
            var owner = type(variable.parent)
            if (owner is JavaType.Parameterized) {
                owner = owner.type
            }
            val typeRef = type(variable.type)
            vt.unsafeSet(owner, typeRef, annotations)
        }
    }

    fun variableType(variable: IrVariable): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(variable)
        return variableType(variable, signature)
    }

    private fun variableType(variable: IrVariable, signature: String): JavaType.Variable {
        return typeFactory.computeVariable(signature, 0, variable.name.asString(), variable) { vt ->
            val annotations = listAnnotations(variable.annotations)
            var owner = type(variable.parent)
            if (owner is JavaType.Parameterized) {
                owner = owner.type
            }
            val typeRef = type(variable.type)
            vt.unsafeSet(owner, typeRef, annotations)
        }
    }

    fun variableType(valueParameter: IrValueParameter): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(valueParameter)
        return variableType(valueParameter, signature)
    }

    private fun variableType(valueParameter: IrValueParameter, signature: String): JavaType.Variable {
        return typeFactory.computeVariable(signature, 0, valueParameter.name.asString(), valueParameter) { variable ->
            val annotations = listAnnotations(valueParameter.annotations)
            var owner = type(valueParameter.parent)
            if (owner is JavaType.Parameterized) {
                owner = owner.type
            }
            val typeRef = type(valueParameter.type)
            variable.unsafeSet(owner, typeRef, annotations)
        }
    }

    private fun listAnnotations(annotations: List<IrConstructorCall>): List<JavaType.FullyQualified> {
        val mapped: MutableList<JavaType.FullyQualified> = ArrayList(annotations.size)
        for (annotation: IrConstructorCall in annotations) {
            if (isNotSourceRetention(annotation.type.classifierOrNull?.owner)) {
                val type = TypeUtils.asFullyQualified(type(annotation.type))
                if (type != null) {
                    mapped.add(type)
                }
            }
        }
        return mapped.toList()
    }

    private fun isNotSourceRetention(owner: IrSymbolOwner?): Boolean {
        if (owner is IrMutableAnnotationContainer) {
            for (ann in owner.annotations) {
                if (isSourceRetention(ann)) {
                    return false
                }
            }
        } else {
            println()
        }
        return true
    }

    private fun isSourceRetention(annotation: IrConstructorCall): Boolean {
        val sig = signatureBuilder.classSignature(annotation.type)
        if (sig == "kotlin.annotation.Retention" || sig == "java.lang.annotation") {
            for (arg in annotation.arguments) {
                val args = arg
                if (args is IrDeclarationReference && args.symbol.owner is IrDeclarationWithName) {
                    return (args.symbol.owner as IrDeclarationWithName).name.asString() == "SOURCE"
                }
            }
        }
        return false
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

    private fun mapToFlagsBitmap(visibility: DescriptorVisibility): Long {
        return mapToFlagsBitmap(visibility, null)
    }

    private fun mapToFlagsBitmap(visibility: DescriptorVisibility, modality: Modality?): Long {
        return mapToFlagsBitmap(visibility, modality, null)
    }

    private fun mapToFlagsBitmap(visibility: DescriptorVisibility, modality: Modality?, classKind: ClassKind?): Long {
        var bitMask: Long = 0

        when (visibility.externalDisplayName.lowercase()) {
            "public" -> bitMask += 1L
            "private" -> bitMask += 1L shl 1
            "protected" -> bitMask += 1L shl 2
            "internal", "package-private", "local" -> {}
            else -> {
                throw UnsupportedOperationException("Unsupported visibility: ${visibility.name.lowercase()}")
            }
        }

        if (modality != null) {
            bitMask += when (modality.name.lowercase()) {
                "final" -> 1L shl 4
                "abstract" -> 1L shl 10
                "sealed" -> 1L shl 62
                "open" -> 0
                else -> {
                    throw UnsupportedOperationException("Unsupported modality: ${modality.name.lowercase()}")
                }
            }
        }

        // Set class-kind-specific flags to match the Java parser's output.
        // Interfaces and annotations are implicitly abstract in the JVM.
        if (classKind != null) {
            when (classKind) {
                ClassKind.INTERFACE -> {
                    bitMask = bitMask or (1L shl 9)  // Interface
                    bitMask = bitMask or (1L shl 10) // Abstract
                    bitMask = bitMask and (1L shl 4).inv() // not Final
                }
                ClassKind.ANNOTATION_CLASS -> {
                    bitMask = bitMask or (1L shl 9)  // Interface (annotations are interfaces in bytecode)
                    bitMask = bitMask or (1L shl 10) // Abstract
                    bitMask = bitMask and (1L shl 4).inv() // not Final
                }
                ClassKind.ENUM_CLASS -> {
                    bitMask = bitMask or (1L shl 14) // Enum
                }
                else -> {}
            }
        }

        return bitMask
    }

    private fun isNotAny(type: IrType): Boolean {
        return !(type.classifierOrNull != null && type.classifierOrNull!!.owner is IrClass && "kotlin.Any" == (type.classifierOrNull!!.owner as IrClass).kotlinFqName.asString())
    }
}
