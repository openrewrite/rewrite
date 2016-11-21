package com.netflix.java.refactor.ast

import java.io.Serializable
import java.util.*

sealed class Type(): Serializable {
    abstract class TypeWithOwner: Type() {
        abstract val owner: Type?

        // FIXME is this useful anymore?
        fun ownedByType(clazz: String): Boolean =
            if (this is Type.Class && fullyQualifiedName == clazz)
                true
            else if(owner is TypeWithOwner) 
                (owner as TypeWithOwner).ownedByType(clazz) 
            else false
    }
    
    data class Package private constructor(val fullName: String, override val owner: Type?): TypeWithOwner() {
        companion object {
            fun build(cache: TypeCache, fullName: String): Package? =
                if(fullName.isEmpty()) null
                else cache.packagePool.getOrPut(fullName) {
                    val subpackage = fullName.substringBeforeLast('.')
                    Package(fullName, if(subpackage != fullName) build(cache, subpackage) else null)
                }
        }
    }
    
    data class Class private constructor(val fullyQualifiedName: String,
                                         override val owner: Type?,
                                         var members: List<Var>,
                                         var supertype: Class?): TypeWithOwner() {

        override fun toString(): String = fullyQualifiedName

        fun isCyclicRef() = this == Cyclic

        fun className() =
                fullyQualifiedName.split('.').dropWhile { it[0].isLowerCase() }.joinToString(".")

        fun packageOwner() =
                fullyQualifiedName.split('.').dropLastWhile { it[0].isUpperCase() }.joinToString(".")

        companion object {
            val Cyclic = Class("CYCLIC_TYPE_REF", null, emptyList(), null)

            fun build(cache: TypeCache, fullyQualifiedName: String, members: List<Var> = emptyList(), supertype: Class? = null): Type.Class {
                val clazz = cache.classPool.getOrPut(fullyQualifiedName) {
                    val subName = fullyQualifiedName.substringBeforeLast(".")
                    Class(fullyQualifiedName,
                            if (!subName.contains('.'))
                                null
                            else if (subName.substringAfterLast('.').first().isUpperCase()) {
                                Class.build(cache, subName, emptyList(), null)
                            } else
                                Package.build(cache, subName),
                            members,
                            supertype)
                }

                if(members.isNotEmpty())
                    clazz.members = members
                if(supertype != null)
                    clazz.supertype = supertype

                return clazz
            }
        }
    }
    
    data class Method(val genericSignature: Signature, val resolvedSignature: Signature, val paramNames: List<String>?): Type() {
        data class Signature(val returnType: Type?, val paramTypes: List<Type>)
    }
   
    data class GenericTypeVariable(val fullyQualifiedName: String, val bound: Class?): Type()
    
    data class Array(val elemType: Type): Type()
    
    data class Primitive(val typeTag: Tag): Type()
    
    data class Var(val name: String, val type: Type?, val flags: Long): Type() {
        enum class Flags(val value: Long) {
            Public(1L),
            Private(1 shl 1),
            Protected(1 shl 2),
            Static(1 shl 3),
            Final(1 shl 4),
            Synchronized(1 shl 5),
            Volatile(1 shl 6),
            Transient(1 shl 7),
            Native(1 shl 8),
            Abstract(1 shl 10),
            StrictFp(1 shl 11);
        }
        
        fun hasFlags(vararg test: Flags) = test.all { flags and it.value != 0L }
    }

    enum class Tag {
        Boolean,
        Byte,
        Char,
        Double,
        Float,
        Int,
        Long,
        Short,
        Void,
        String,
        None,
        Wildcard,
        Null
    }
}

fun Type?.asClass(): Type.Class? = when(this) {
    is Type.Class -> this
    else -> null
}

fun Type?.asPackage(): Type.Package? = when(this) {
    is Type.Package -> this
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