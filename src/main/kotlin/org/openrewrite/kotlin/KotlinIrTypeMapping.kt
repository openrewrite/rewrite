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
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
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

class KotlinIrTypeMapping(typeCache: JavaTypeCache) : JavaTypeMapping<Any> {
    private val signatureBuilder: KotlinTypeIrSignatureBuilder = KotlinTypeIrSignatureBuilder()
    private val typeCache: JavaTypeCache

    init {
        this.typeCache = typeCache
    }

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

            is IrConstructorCall -> {
                return type(baseType.symbol.owner)
            }

            is IrExternalPackageFragment -> {
                return externalPackageFragment(signature)
            }

            is IrField -> {
                TODO("IrField not implemented")
            }

            is IrFunction -> {
                return methodDeclarationType(baseType, signature)
            }

            is IrProperty -> {
                return variableType(baseType, signature)
            }

            is IrTypeAlias -> {
                return alias(baseType, signature)
            }

            is IrTypeParameter -> {
                return generic(baseType, signature)
            }

            is IrTypeProjection, is IrStarProjection -> {
                return typeProjection(baseType, signature)
            }

            is IrValueParameter -> {
                return variableType(baseType)
            }

            is IrVariable -> {
                TODO("IrVariable not implemented")
            }
        }

        throw UnsupportedOperationException("Unsupported type: ${type.javaClass}")
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
        val irClass = if (type is IrClass) type else (type as IrSimpleType).classifier.owner as IrClass
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
            var interfaceSymbols: MutableList<IrSymbolOwner>? = null
            for (sType in irClass.superTypes) {
                when (val classifier: IrClassifierSymbol? = sType.classifierOrNull) {
                    is IrClassSymbol -> {
                        when (classifier.owner.kind) {
                            ClassKind.CLASS -> {
                                supertype = TypeUtils.asFullyQualified(type(classifier.owner))
                            }

                            ClassKind.INTERFACE -> {
                                if (interfaceSymbols == null) {
                                    interfaceSymbols = ArrayList()
                                }
                                interfaceSymbols.add(classifier.owner)
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
                methods.add(mt)
            }

            for (function: IrFunction in irClass.functions) {
                if (methods == null) {
                    methods = ArrayList(irClass.functions.toList().size)
                }
                val mt = methodDeclarationType(function)
                methods.add(mt)
            }

            var interfaces: MutableList<JavaType.FullyQualified>? = null
            if (!interfaceSymbols.isNullOrEmpty()) {
                interfaces = ArrayList(interfaceSymbols.size)
                for (interfaceSymbol: IrSymbolOwner in interfaceSymbols) {
                    val sym: Any =
                        if (interfaceSymbol is Fir2IrLazyClass) interfaceSymbol.symbol.owner.symbol else interfaceSymbol
                    val javaType = TypeUtils.asFullyQualified(type(sym))
                    if (javaType != null) {
                        interfaces.add(javaType)
                    }
                }
            }
            clazz.unsafeSet(null, supertype, owner, listAnnotations(irClass.annotations), interfaces, fields, methods)
        }

        if (irClass.typeParameters.isNotEmpty()) {
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                val typeParameters: MutableList<JavaType> = ArrayList(irClass.typeParameters.size)
                pt = JavaType.Parameterized(null, null, null)

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

    fun methodDeclarationType(function: IrFunction): JavaType.Method {
        val signature = signatureBuilder.methodDeclarationSignature(function)
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
            mapToFlagsBitmap(function.visibility),
            null,
            if (function is IrConstructor) "<constructor>" else function.name.asString(),
            null,
            paramNames,
            null, null, null, null
        )
        typeCache.put(signature, method)
        var declaringType = TypeUtils.asFullyQualified(type(function.parent)) ?: TODO()
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
                GenericTypeVariable(null, "*", INVARIANT, null)
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
            val type = TypeUtils.asFullyQualified(type(annotation.type))
            if (type != null) {
                mapped.add(type)
            }
        }
        return mapped.toList()
    }

    private fun mapKind(kind: ClassKind): JavaType.FullyQualified.Kind {
        return when (kind) {
            ClassKind.INTERFACE -> JavaType.FullyQualified.Kind.Interface
            ClassKind.ENUM_CLASS -> JavaType.FullyQualified.Kind.Enum
            ClassKind.ENUM_ENTRY -> TODO()
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