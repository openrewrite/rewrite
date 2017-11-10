/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.ast

import com.fasterxml.jackson.annotation.*
import com.koloboke.collect.map.hash.HashObjObjMap
import com.koloboke.collect.map.hash.HashObjObjMaps
import com.koloboke.collect.set.hash.HashObjSet
import com.koloboke.collect.set.hash.HashObjSets
import com.netflix.rewrite.ast.Type.Companion.deepEquals
import java.io.Serializable

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
sealed class Type: Serializable {

    companion object {
        internal fun Type?.deepEquals(t: Type?): Boolean = when (this) {
            null -> t == null
            is Array -> t is Array && this.deepEquals(t)
            is Class -> t is Class && this.deepEquals(t)
            is Cyclic -> this == t
            is GenericTypeVariable -> t is GenericTypeVariable && this.deepEquals(t)
            is Method -> t is Method && this.deepEquals(t)
            is Primitive -> t is Primitive && this == t
            is Var -> t is Var && this.deepEquals(t)
            is ShallowClass -> t is ShallowClass && fullyQualifiedName == t.fullyQualifiedName
            is MultiCatchType -> t is MultiCatchType && throwableTypes.size == t.throwableTypes.size &&
                    throwableTypes.indices.all { i -> throwableTypes[i].deepEquals(t.throwableTypes[i]) }
        }
    }

    data class MultiCatchType(val throwableTypes: List<Type>): Type()

    /**
     * Reduces memory and CPU footprint when deep class insight isn't necessary, such as
     * for the type parameters of a Type.Class
     */
    data class ShallowClass(val fullyQualifiedName: String): Type()

    data class Class private constructor(val fullyQualifiedName: String,
                                         val members: List<Var>, // will always be sorted by name by build(..)
                                         val supertype: Class?,
                                         val typeParameters: List<Type>,
                                         val interfaces: List<Type>): Type() {

        override fun toString(): String = fullyQualifiedName

        fun className() = fullyQualifiedName.split('.').dropWhile { it[0].isLowerCase() }.joinToString(".")

        override fun hashCode() = fullyQualifiedName.hashCode()

        fun packageName(): String {
            fun packageNameInternal(fqn: String): String {
                if(!fqn.contains("."))
                    return ""

                val subName = fqn.substringBeforeLast(".")
                return if (subName.substringAfterLast('.').first().isUpperCase()) {
                    packageNameInternal(subName)
                } else {
                    subName
                }
            }

            return packageNameInternal(fullyQualifiedName)
        }

        fun deepEquals(c: Class?): Boolean {
            if(c == null || fullyQualifiedName != c.fullyQualifiedName)
                return false

            if(members.size != c.members.size)
                return false
            members.indices.forEach {
                if(!members[it].deepEquals(c.members[it])) return@deepEquals false
            }

            if(!supertype.deepEquals(c.supertype))
                return false

            if(typeParameters.size != c.typeParameters.size)
                return false
            typeParameters.indices.forEach {
                if(!typeParameters[it].deepEquals(c.typeParameters[it])) return@deepEquals false
            }

            return true
        }

        companion object {
            var classVersionDependentComparison = true

            // there shouldn't be too many distinct types represented by the same fully qualified name
            val flyweights = HashObjObjMaps.newMutableMap<String, HashObjSet<Class>>()

            @JvmStatic @JsonCreator
            fun build(fullyQualifiedName: String,
                      members: List<Var> = emptyList(),
                      supertype: Class? = null,
                      typeParameters: List<Type> = emptyList(),
                      interfaces: List<Type> = emptyList()): Type.Class {
                // the variants are the various versions of this fully qualified name, where equality is determined by
                // whether the supertype hierarchy and members through the entire supertype hierarchy are equal
                val test = Class(fullyQualifiedName, members.sortedBy { it.name }, supertype, typeParameters, interfaces)

                return synchronized(flyweights) {
                    val variants = flyweights.getOrPut(fullyQualifiedName) {
                        HashObjSets.newMutableSet<Class>(arrayOf(Class(fullyQualifiedName, members, supertype, typeParameters, interfaces)))
                    }

                    (if(classVersionDependentComparison) variants.find { it.deepEquals(test) } else variants.firstOrNull()) ?: {
                        variants.add(test)
                        test
                    }()
                }
            }
        }
    }

    data class Cyclic(val fullyQualifiedName: String) : Type()

    data class Method private constructor(val declaringType: Class,
                                          val name: String,
                                          val genericSignature: Signature?,
                                          val resolvedSignature: Signature?,
                                          val paramNames: List<String>?,
                                          val flags: List<Flag>): Type() {

        fun hasFlags(vararg test: Flag) = test.all { flags.contains(it) }

        data class Signature(val returnType: Type?, val paramTypes: List<Type>): Serializable

        internal fun deepEquals(method: Method?): Boolean {
            if(method == null)
                return false

            return declaringType.deepEquals(method.declaringType) &&
                    genericSignature.deepEquals(method.genericSignature) &&
                    resolvedSignature.deepEquals(method.resolvedSignature) &&
                    flags.all { method.flags.contains(it) } &&
                    paramNames?.equals(method.paramNames) ?: method.paramNames == null
        }

        companion object {
            val flyweights = HashObjObjMaps.newMutableMap<Class, HashObjObjMap<String, HashObjSet<Method>>>()

            private fun Signature?.deepEquals(signature: Signature?): Boolean {
                if(this == null)
                    return signature == null
                if(signature == null)
                    return false

                return returnType.deepEquals(signature.returnType) &&
                        paramTypes.size == signature.paramTypes.size &&
                        (paramTypes.isEmpty() || paramTypes.zip(signature.paramTypes).map { (p1, p2) -> p1.deepEquals(p2) }.reduce(Boolean::and))
            }

            @JvmStatic @JsonCreator
            fun build(declaringType: Class, name: String, genericSignature: Signature?, resolvedSignature: Signature?,
                      paramNames: List<String>, flags: List<Flag>): Type.Method {
                val test = Method(declaringType, name, genericSignature, resolvedSignature, paramNames, flags)

                return synchronized(flyweights) {
                    val methods = flyweights
                            .getOrPut(declaringType, { HashObjObjMaps.newMutableMap(mapOf(name to HashObjSets.newMutableSet(listOf(test)))) })
                            .getOrPut(name, { HashObjSets.newMutableSet(listOf(test)) })

                    methods.find { m ->
                        paramNames == m.paramNames &&
                                genericSignature.deepEquals(m.genericSignature) &&
                                resolvedSignature.deepEquals(m.resolvedSignature) &&
                                flags.all { m.flags.contains(it) }
                    } ?: {
                        methods.add(test)
                        test
                    }()
                }
            }
        }
    }

    data class GenericTypeVariable(val fullyQualifiedName: String, val bound: Class?): Type() {
        internal fun deepEquals(generic: GenericTypeVariable?): Boolean =
                generic != null && fullyQualifiedName == generic.fullyQualifiedName && bound.deepEquals(generic.bound)
    }

    data class Array(val elemType: Type): Type() {
        internal fun deepEquals(array: Array?): Boolean =
                array != null && elemType.deepEquals(array.elemType)
    }

    data class Primitive(val keyword: String): Type() {
        companion object {
            val Boolean = Primitive("boolean")
            val Byte = Primitive("byte")
            val Char = Primitive("char")
            val Double = Primitive("double")
            val Float = Primitive("float")
            val Int = Primitive("int")
            val Long = Primitive("long")
            val Short = Primitive("short")
            val Void = Primitive("void")
            val String = Primitive("String")
            val None = Primitive("")
            val Wildcard = Primitive("*")
            val Null = Primitive("null")

            fun build(keyword: String): Primitive = when(keyword) {
                "boolean" -> Boolean
                "byte" -> Byte
                "char" -> Char
                "double" -> Double
                "float" -> Float
                "int" -> Int
                "long" -> Long
                "short" -> Short
                "void" -> Void
                "String" -> String
                "" -> None
                "*" -> Wildcard
                "null" -> Null
                else -> throw IllegalArgumentException("Invalid primitive ordinal")
            }
        }
    }

    data class Var(val name: String, val type: Type?, val flags: List<Flag>): Type() {
        fun hasFlags(vararg test: Flag) = test.all { flags.contains(it) }

        override fun hashCode() = name.hashCode()

        internal fun deepEquals(v: Var): Boolean =
                name == v.name && type.deepEquals(v.type) && flags.all { v.flags.contains(it) }
    }
}

fun Type?.asClass(): Type.Class? = when(this) {
    is Type.Class -> this
    else -> null
}

fun Type?.asArray(): Type.Array? = when(this) {
    is Type.Array -> this
    else -> null
}

fun Type?.asGeneric(): Type.GenericTypeVariable? = when(this) {
    is Type.GenericTypeVariable -> this
    else -> null
}

fun Type?.asMethod(): Type.Method? = when(this) {
    is Type.Method -> this
    else -> null
}

fun Type?.hasElementType(fullyQualifiedName: String): Boolean = when(this) {
    is Type.Array -> this.elemType.hasElementType(fullyQualifiedName)
    is Type.Class -> this.fullyQualifiedName == fullyQualifiedName
    is Type.GenericTypeVariable -> this.fullyQualifiedName == fullyQualifiedName
    else -> false
}

fun Type?.asPrimitive(): Type.Primitive? = when(this) {
    is Type.Primitive -> this
    else -> null
}