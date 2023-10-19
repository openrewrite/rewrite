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

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.types.Variance
import org.openrewrite.java.JavaTypeSignatureBuilder
import java.util.*

class KotlinTypeIrSignatureBuilder: JavaTypeSignatureBuilder {
    private var typeVariableNameStack: MutableSet<String>? = null

    override fun signature(type: Any?): String {
        if (type == null || type is IrErrorType) {
            return "{undefined}"
        }

        if (type is IrClassifierSymbol) {
            return signature(type.owner)
        }

        val baseType = if (type is IrSimpleType) type.classifier.owner else type

        when (baseType) {
            is IrFile -> {
                return fileSignature(baseType)
            }

            is IrClass -> {
                // The IrSimpleType may contain bounded type arguments
                val useSimpleType = (type is IrSimpleType && (type.arguments.isNotEmpty()))
                return if (baseType.typeParameters.isEmpty()) classSignature(baseType) else parameterizedSignature(if (useSimpleType) type else baseType)
            }

            is IrCall -> {
                return signature(baseType.symbol.owner)
            }

            is IrConstructorCall -> {
                return signature(baseType.symbol.owner)
            }

            is IrExternalPackageFragment -> {
                return externalPackageFragmentSignature(baseType)
            }

            is IrField -> {
                TODO("IrField not yet implemented.")
            }

            is IrFunction -> {
                return methodDeclarationSignature(baseType)
            }

            is IrProperty -> {
                return variableSignature(baseType)
            }

            is IrTypeAlias -> {
                return aliasSignature(baseType)
            }

            is IrTypeParameter -> {
                return genericSignature(baseType)
            }

            is IrTypeProjection, is IrStarProjection -> {
                return typeProjection(baseType)
            }

            is IrValueParameter -> {
                return variableSignature(baseType)
            }
            is IrVariable -> {
                TODO("IrVariable not yet implemented.")
            }
        }

        throw IllegalStateException("Unexpected type " + baseType.javaClass.getName())
    }

    private fun aliasSignature(type: IrTypeAlias): String {
        if (type.parent !is IrFile) {
            TODO()
        }
        val s = StringBuilder()
        if ((type.parent as IrFile).fqName.asString().isNotEmpty()) {
            s.append((type.parent as IrFile).fqName.asString()).append(".")
        }
        s.append(type.name.asString())
        val joiner = StringJoiner(", ", "<", ">")
        for (tp in type.typeParameters) {
            joiner.add(signature(tp))
        }
        s.append(joiner)
        return s.toString()
    }

    private fun externalPackageFragmentSignature(baseType: IrExternalPackageFragment): String {
        return baseType.fqName.asString()
    }

    private fun fileSignature(type: IrFile): String {
        return (if (type.fqName.asString()
                .isNotEmpty()
        ) type.fqName.asString() + "." else "") + type.name.replace(".kt", "Kt")
    }

    /**
     * Kotlin does not support dimensioned arrays.
     */
    override fun arraySignature(type: Any): String {
        throw UnsupportedOperationException("This should never happen.")
    }

    override fun classSignature(type: Any): String {
        if (type !is IrClass) {
            throw UnsupportedOperationException("Unexpected classType: " + type.javaClass)
        }
        val sb = StringBuilder()
        if (type.parent is IrClass) {
            sb.append(classSignature(type.parent)).append("$")
        } else if (type.parent is IrFunction) {
            // TODO: review how Method parents should be represented.
        } else if (type.packageFqName != null && type.packageFqName!!.asString().isNotEmpty()) {
            sb.append(type.packageFqName).append(".")
        }
        sb.append(type.name)
        return sb.toString()
    }

    override fun genericSignature(type: Any): String {
        val typeParameter: IrTypeParameter = type as IrTypeParameter
        val name = typeParameter.name.asString()

        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }

        if (!typeVariableNameStack!!.add(name)) {
            return "Generic{$name}"
        }

        val s = StringBuilder("Generic{").append(name)
        val boundSigs = StringJoiner(" & ")
        for (bound in typeParameter.superTypes) {
            if (isNotAny(bound)) {
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

    private fun typeProjection(type: Any): String {
        val sig = StringBuilder("Generic{")
        when (type) {
            is IrTypeProjection -> {
                sig.append("?")
                sig.append(if (type.variance == Variance.OUT_VARIANCE) " extends " else " super ")
                sig.append(signature(type.type))
            }

            is IrStarProjection -> {
                sig.append("*")
            }

            else -> {
                throw UnsupportedOperationException("Unsupported type projection: " + type.javaClass)
            }
        }
        return sig.append("}").toString()
    }

    override fun parameterizedSignature(type: Any): String {
        if (type !is IrSimpleType && type !is IrClass) {
            throw UnsupportedOperationException("Unexpected parameterizedType: " + type.javaClass)
        }

        val s = StringBuilder(classSignature(if (type is IrSimpleType) type.classifier.owner else type))
        val joiner = StringJoiner(", ", "<", ">")
        val params = if (type is IrSimpleType) type.arguments else (type as IrClass).typeParameters
        for (tp in params) {
            joiner.add(signature(tp))
        }
        s.append(joiner)
        return s.toString()
    }

    override fun primitiveSignature(type: Any): String {
        TODO("Not yet implemented")
    }

    fun variableSignature(
        property: IrProperty
    ): String {
        val owner = if (property.parent is IrClass) classSignature(property.parent) else signature(property.parent)
        val typeSig = if (property.getter != null) {
            signature(property.getter!!.returnType)
        } else if (property.backingField != null) {
            signature(property.backingField!!.type)
        } else {
            // FIXME
            "{undefined}"
        }
        return "$owner{name=${property.name.asString()},type=$typeSig}"
    }

    fun variableSignature(
        valueParameter: IrValueParameter
    ): String {
        val owner = if (valueParameter.parent is IrClass) classSignature(valueParameter.parent) else signature(valueParameter.parent)
        val typeSig = signature(valueParameter.type)
        return "$owner{name=${valueParameter.name.asString()},type=$typeSig}"
    }

    fun methodDeclarationSignature(function: IrFunction): String {
        val parent = when (function.parent) {
            is IrClass -> classSignature(function.parent)
            else -> signature(function.parent)
        }
        val signature = StringBuilder(parent)
        if (function is IrConstructor) {
            signature.append("{name=<constructor>,return=$parent")
        } else {
            signature.append("{name=").append(function.name.asString())
            signature.append(",return=").append(signature(function.returnType))
        }
        signature.append(",parameters=").append(methodArgumentSignature(function)).append("}")
        return signature.toString()
    }

    private fun methodArgumentSignature(function: IrFunction): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        if (function.extensionReceiverParameter != null) {
            genericArgumentTypes.add(signature(function.extensionReceiverParameter!!.type))
        }
        for (param: IrValueParameter in function.valueParameters) {
            genericArgumentTypes.add(signature(param.type))
        }
        return genericArgumentTypes.toString()
    }

    private fun isNotAny(type: IrType): Boolean {
        return !(type.classifierOrNull != null && type.classifierOrNull!!.owner is IrClass && "kotlin.Any" == (type.classifierOrNull!!.owner as IrClass).kotlinFqName.asString())
    }
}