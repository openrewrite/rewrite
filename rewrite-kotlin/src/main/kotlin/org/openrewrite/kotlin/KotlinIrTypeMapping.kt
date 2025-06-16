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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.*
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
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.GenericTypeVariable
import org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*
import org.openrewrite.java.tree.TypeUtils

@Suppress("unused", "UNUSED_PARAMETER")
class KotlinIrTypeMapping(private val typeCache: JavaTypeCache) : JavaTypeMapping<Any> {
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
        val existing: JavaType? = typeCache.get(signature)
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

            is IrConst<*> -> {
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
        typeCache.put(signature, packageFragment)
        return packageFragment
    }

    private fun alias(type: IrTypeAlias, signature: String): JavaType {
        val aliased = type(type.expandedType)
        typeCache.put(signature, aliased)
        return aliased
    }

    private fun fileType(signature: String): JavaType {
        val existing = typeCache.get<JavaType.FullyQualified>(signature)
        if (existing != null) {
            return existing
        }
        val fileType = JavaType.ShallowClass.build(signature)
        typeCache.put(signature, fileType)
        return fileType
    }

    private fun classType(type: Any, signature: String): JavaType {
        if (type !is IrSimpleType && type !is IrClass) {
            throw UnsupportedOperationException("Unexpected classType: " + type.javaClass)
        }
        val irClass = type as? IrClass ?: (type as IrSimpleType).classifier.owner as IrClass
        val fqn: String = signatureBuilder.classSignature(irClass)
        val fq: JavaType.FullyQualified? = typeCache.get(fqn)
        var clazz: JavaType.Class? = (if (fq is JavaType.Parameterized) fq.type else fq) as JavaType.Class?
        if (clazz == null) {
            clazz = JavaType.Class(
                null,
                mapToFlagsBitmap(irClass.visibility, irClass.modality),
                fqn,
                mapKind(irClass.kind),
                null, null, null, null, null, null, null
            )

            typeCache.put(fqn, clazz)

            var supertype: JavaType.FullyQualified? = null
            var interfaceTypes: MutableList<IrType>? = null
            // TODO: review
            //  In Kotlin the super type of java.lang.Object is kotlin.Any.
            //  This condition matches the super type from the Java compiler, but is technically incorrect from the POV of the Kotlin compiler.
            if (signature != "java.lang.Object" ) {
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
                                    interfaceTypes.add(sType)
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
                fields.add(vt)
            }

            var methods: MutableList<JavaType.Method>? = null
            for (function: IrFunction in irClass.constructors) {
                if (methods == null) {
                    methods = ArrayList(irClass.functions.toList().size)
                }
                val mt = methodDeclarationType(function)
                if (mt != null) {
                    methods.add(mt)
                }
            }

            for (function: IrFunction in irClass.functions) {
                if (methods == null) {
                    methods = ArrayList(irClass.functions.toList().size)
                }
                val mt = methodDeclarationType(function)
                if (mt != null) {
                    methods.add(mt)
                }
            }

            var interfaces: MutableList<JavaType.FullyQualified>? = null
            if (!interfaceTypes.isNullOrEmpty()) {
                interfaces = ArrayList(interfaceTypes.size)
                for (iType: IrType in interfaceTypes) {
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
                    typeParameters.add(type(tParam))
                }
            }
            clazz.unsafeSet(typeParameters, supertype, owner, listAnnotations(irClass.annotations), interfaces, fields, methods)
        }

        if (irClass.typeParameters.isNotEmpty()) {
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                val typeParameters: MutableList<JavaType> = ArrayList(irClass.typeParameters.size)
                pt = JavaType.Parameterized(null, null, null)
                typeCache.put(signature, pt)
                val params = if (type is IrSimpleType) type.arguments else (type as IrClass).typeParameters
                for (tp in params) {
                    typeParameters.add(type(tp))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    private fun generic(type: IrTypeParameter, signature: String): JavaType {
        val name = type.name.asString()
        val gtv = GenericTypeVariable(null, name, INVARIANT, null)
        typeCache.put(signature, gtv)

        var bounds: MutableList<JavaType>? = null
        if (type.isReified) {
            throw UnsupportedOperationException("Add support for reified generic types.")
        }
        for (bound: IrType in type.superTypes) {
            if (isNotAny(bound)) {
                if (bounds == null) {
                    bounds = ArrayList()
                }
                bounds.add(type(bound))
            }
        }
        gtv.unsafeSet(gtv.name, if (bounds == null) INVARIANT else COVARIANT, bounds)
        return gtv
    }

    fun methodDeclarationType(function: IrFunction?): JavaType.Method? {
        if (function == null) {
            return null
        }
        val signature = signatureBuilder.methodSignature(function)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }
        return methodDeclarationType(function, signature)
    }

    private fun methodDeclarationType(function: IrFunction, signature: String): JavaType.Method {
        val paramNames: MutableList<String>? =
            if (function.valueParameters.isEmpty()) null else ArrayList(function.valueParameters.size)
        for (param: IrValueParameter in function.valueParameters) {
            paramNames!!.add(param.name.asString())
        }
        val method = JavaType.Method(
            null,
            mapToFlagsBitmap(
                function.visibility,
                when (function) {
                    is IrFunctionImpl -> function.modality
                    is IrFunctionWithLateBindingImpl -> function.modality
                    is Fir2IrLazySimpleFunction -> function.modality
                    is IrConstructorImpl, is Fir2IrLazyConstructor -> null
                    else -> throw UnsupportedOperationException("Unsupported IrFunction type: " + function.javaClass)
                }
            ),
            null,
            if (function is IrConstructor) "<constructor>" else function.name.asString(),
            null,
            paramNames,
            null,
            null,
            null,
            null,
            null
        )
        typeCache.put(signature, method)
        var declaringType = when (val irParent = function.parent) {
            is IrField -> TypeUtils.asFullyQualified(type(irParent.parent))
            else -> TypeUtils.asFullyQualified(type(irParent))
        }

        if (declaringType is JavaType.Parameterized) {
            declaringType = declaringType.type
        }
        val returnType = type(function.returnType)
        val paramTypes: MutableList<JavaType>? =
            if (function.valueParameters.isNotEmpty() || function.extensionReceiverParameter != null)
                ArrayList(function.valueParameters.size + (if (function.extensionReceiverParameter != null) 1 else 0))
            else null
        if (function.extensionReceiverParameter != null) {
            paramTypes!!.add(type(function.extensionReceiverParameter!!.type))
        }
        for (param: IrValueParameter in function.valueParameters) {
            paramTypes!!.add(type(param.type))
        }
        method.unsafeSet(
            declaringType,
            if (function is IrConstructor) declaringType else returnType,
            paramTypes, null, listAnnotations(function.annotations)
        )
        return method
    }

    fun methodInvocationType(type: IrFunctionAccessExpression?): JavaType.Method? {
        if (type == null) {
            return null
        }
        val signature = signatureBuilder.methodSignature(type)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }
        return when (type) {
            is IrConstructorCall -> methodInvocationType(type, signature)
            is IrCall -> methodInvocationType(type, signature)
            else -> throw UnsupportedOperationException("Unsupported methodInvocationType: " + type.javaClass)
        }
    }

    fun methodInvocationType(type: IrCall, signature: String): JavaType.Method {
        val paramNames: MutableList<String> = ArrayList(type.valueArguments.size)

        for (v in type.symbol.owner.valueParameters) {
            paramNames.add(v.name.asString())
        }
        val method = JavaType.Method(
            null,
            mapToFlagsBitmap(type.symbol.owner.visibility),
            null,
            type.symbol.owner.name.asString(),
            null,
            paramNames,
            null,
            null,
            null,
            null,
            null
        )
        typeCache.put(signature, method)
        var declaringType = TypeUtils.asFullyQualified(type(type.symbol.owner.parent))
        if (declaringType is JavaType.Parameterized) {
            declaringType = declaringType.type
        }
        val returnType = type(type.symbol.owner.returnType)
        val paramTypes: MutableList<JavaType>? =
            if (type.valueArguments.isNotEmpty() || type.extensionReceiver != null) ArrayList(type.valueArguments.size + (if (type.extensionReceiver != null) 1 else 0))
            else null
        if (type.extensionReceiver != null) {
            paramTypes!!.add(type(type.extensionReceiver!!.type))
        }
        for (param: IrExpression? in type.valueArguments) {
            if (param != null) {
                paramTypes!!.add(type(param.type))
            }
        }
        method.unsafeSet(
            declaringType,
            returnType,
            paramTypes, null, listAnnotations(type.symbol.owner.annotations)
        )
        return method
    }

    fun methodInvocationType(type: IrConstructorCall, signature: String): JavaType.Method {
        val paramNames: MutableList<String> = ArrayList(type.valueArguments.size)

        for (v in type.symbol.owner.valueParameters) {
            paramNames.add(v.name.asString())
        }
        val method = JavaType.Method(
            null,
            mapToFlagsBitmap(type.symbol.owner.visibility),
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
        typeCache.put(signature, method)
        var declaringType = TypeUtils.asFullyQualified(type(type.symbol.owner.parent))
        if (declaringType is JavaType.Parameterized) {
            declaringType = declaringType.type
        }
        val returnType = declaringType
        val paramTypes: MutableList<JavaType>? =
            if (type.valueArguments.isNotEmpty() || type.extensionReceiver != null) ArrayList(type.valueArguments.size + (if (type.extensionReceiver != null) 1 else 0))
            else null
        if (type.extensionReceiver != null) {
            paramTypes!!.add(type(type.extensionReceiver!!.type))
        }
        for (param: IrExpression? in type.valueArguments) {
            if (param != null) {
                paramTypes!!.add(type(param.type))
            }
        }
        method.unsafeSet(
            declaringType,
            returnType,
            paramTypes, null, listAnnotations(type.symbol.owner.annotations)
        )
        return method
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
            is IrConst<*> -> {
                when (type.kind) {
                    IrConstKind.Int -> JavaType.Primitive.Int
                    IrConstKind.Boolean -> JavaType.Primitive.Boolean
                    IrConstKind.Byte -> JavaType.Primitive.Byte
                    IrConstKind.Char -> JavaType.Primitive.Char
                    IrConstKind.Double -> JavaType.Primitive.Double
                    IrConstKind.Float -> JavaType.Primitive.Float
                    IrConstKind.Long -> JavaType.Primitive.Long
                    IrConstKind.Null -> JavaType.Primitive.Null
                    IrConstKind.Short -> JavaType.Primitive.Short
                    IrConstKind.String -> JavaType.Primitive.String
                }
            }
            else -> {
                // TODO: make sure this isn't visited after StringTemplateExpressions are fixed.
                JavaType.Primitive.None
            }
        }
    }

    private fun typeProjection(type: Any, signature: String): JavaType {
        val gtv = when (type) {
            is IrTypeProjection -> {
                GenericTypeVariable(
                    null,
                    "?",
                    if (type.variance == Variance.OUT_VARIANCE) COVARIANT else CONTRAVARIANT,
                    listOf(type(type.type))
                )
            }

            is IrStarProjection -> {
                GenericTypeVariable(null, "?", INVARIANT, null)
            }

            else -> {
                throw UnsupportedOperationException("Unexpected type projection: " + type.javaClass)
            }
        }
        typeCache.put(signature, gtv)
        return gtv
    }

    fun variableType(property: IrProperty): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(property)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        return variableType(property, signature)
    }

    private fun variableType(property: IrProperty, signature: String): JavaType.Variable {
        val variable = JavaType.Variable(
            null,
            0,
            property.name.asString(),
            null, null, null
        )
        typeCache.put(signature, variable)
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
        return variable
    }

    fun variableType(variable: IrField): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(variable)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        return variableType(variable, signature)
    }

    private fun variableType(variable: IrField, signature: String): JavaType.Variable {
        val vt = JavaType.Variable(
            null,
            0,
            variable.name.asString(),
            null, null, null
        )
        typeCache.put(signature, vt)
        val annotations = listAnnotations(variable.annotations)
        var owner = type(variable.parent)
        if (owner is JavaType.Parameterized) {
            owner = owner.type
        }
        val typeRef = type(variable.type)
        vt.unsafeSet(owner, typeRef, annotations)
        return vt
    }

    fun variableType(variable: IrVariable): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(variable)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        return variableType(variable, signature)
    }

    private fun variableType(variable: IrVariable, signature: String): JavaType.Variable {
        val vt = JavaType.Variable(
            null,
            0,
            variable.name.asString(),
            null, null, null
        )
        typeCache.put(signature, vt)
        val annotations = listAnnotations(variable.annotations)
        var owner = type(variable.parent)
        if (owner is JavaType.Parameterized) {
            owner = owner.type
        }
        val typeRef = type(variable.type)
        vt.unsafeSet(owner, typeRef, annotations)
        return vt
    }

    fun variableType(valueParameter: IrValueParameter): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(valueParameter)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        return variableType(valueParameter, signature)
    }

    private fun variableType(valueParameter: IrValueParameter, signature: String): JavaType.Variable {
        val variable = JavaType.Variable(
            null,
            0,
            valueParameter.name.asString(),
            null, null, null
        )
        typeCache.put(signature, variable)
        val annotations = listAnnotations(valueParameter.annotations)
        var owner = type(valueParameter.parent)
        if (owner is JavaType.Parameterized) {
            owner = owner.type
        }
        val typeRef = type(valueParameter.type)
        variable.unsafeSet(owner, typeRef, annotations)
        return variable
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
            for (args in annotation.valueArguments) {
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
        return bitMask
    }

    private fun isNotAny(type: IrType): Boolean {
        return !(type.classifierOrNull != null && type.classifierOrNull!!.owner is IrClass && "kotlin.Any" == (type.classifierOrNull!!.owner as IrClass).kotlinFqName.asString())
    }
}
