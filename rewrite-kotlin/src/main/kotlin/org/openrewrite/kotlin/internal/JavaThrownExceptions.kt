/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.internal

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.util.Collections
import java.util.WeakHashMap

/**
 * The `throws` clause a Java method or constructor declares. Kotlin's class file reader keeps a
 * method's parameters and return type but drops its exception types, leaving only the bytecode.
 */
internal object JavaThrownExceptions {

    /**
     * Indexes outlive the per-file type mapping that asks for them, so a class file is read once per
     * parse. Keys are weak: a [JavaClass] belongs to the compiler session that loaded it.
     */
    private val perClass: MutableMap<JavaClass, Map<String, List<String>?>> =
        Collections.synchronizedMap(WeakHashMap())

    /**
     * JVM internal names of the exceptions declared by [name] taking [erasedParameterTypes], or null
     * when the class file is unreachable, the method declares none, or the key is ambiguous.
     * Constructors are named `<init>`, as they are in bytecode.
     */
    fun of(declaringClass: JavaClass?, name: String, erasedParameterTypes: List<String?>): List<String>? {
        if (declaringClass == null || erasedParameterTypes.any { it == null }) {
            return null
        }
        val index = perClass.getOrPut(declaringClass) { index(declaringClass) }
        @Suppress("UNCHECKED_CAST")
        return index[key(name, erasedParameterTypes as List<String>)]
    }

    fun of(declaringClass: FirClass?, name: String, erasedParameterTypes: List<String?>): List<String>? =
        of(javaClassOf(declaringClass), name, erasedParameterTypes)

    private fun index(declaringClass: JavaClass): Map<String, List<String>?> {
        val bytes = try {
            (declaringClass as? VirtualFileBoundJavaClass)?.virtualFile?.contentsToByteArray()
        } catch (@Suppress("SwallowedException") e: Exception) {
            null
        } ?: return emptyMap()

        val index = HashMap<String, List<String>?>()
        val throwingNames = HashSet<String>()
        try {
            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: kotlin.Array<String>?
                ): MethodVisitor? {
                    // Bridges and other synthetics restate a declaration a real method already carries.
                    if (access and (Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC) != 0) {
                        return null
                    }
                    val declared = exceptions?.toList() ?: emptyList()
                    if (declared.isNotEmpty()) {
                        throwingNames.add(name)
                    }
                    val key = key(name, Type.getArgumentTypes(descriptor).map { erasedName(it) })
                    // Overloads declaring nothing are indexed too, so a shared key reads as ambiguous.
                    if (key in index) {
                        if (index[key] != declared) {
                            index[key] = null
                        }
                    } else {
                        index[key] = declared
                    }
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        } catch (@Suppress("SwallowedException") e: Exception) {
            return emptyMap()
        }
        index.keys.retainAll { it.substringBefore('(') in throwingNames }
        return index
    }

    private val JAVA_CLASS_FIELD = try {
        FirJavaClass::class.java.getDeclaredField("javaClass").apply { isAccessible = true }
    } catch (@Suppress("SwallowedException") e: Exception) {
        // FIR keeps the JavaClass it was built from private, so a Kotlin version that renames or
        // drops the field leaves every Java callee with no declared exceptions.
        null
    }

    private fun javaClassOf(declaringClass: FirClass?): JavaClass? =
        if (declaringClass is FirJavaClass) JAVA_CLASS_FIELD?.get(declaringClass) as? JavaClass else null

    private fun key(name: String, erasedParameterTypes: List<String>) =
        erasedParameterTypes.joinToString(",", "$name(", ")")

    /** Descriptor-side spelling of a parameter, matching how the caller erases its own types. */
    private fun erasedName(type: Type): String =
        if (type.sort == Type.ARRAY) {
            normalize(type.elementType.className) + "[]".repeat(type.dimensions)
        } else {
            normalize(type.className)
        }

    /**
     * Kotlin renders both a Java `int` and a Java `Integer` as `kotlin.Int`, so both take one key.
     */
    fun normalize(fullyQualifiedName: String): String = when (fullyQualifiedName) {
        "java.lang.Boolean" -> "boolean"
        "java.lang.Byte" -> "byte"
        "java.lang.Character" -> "char"
        "java.lang.Double" -> "double"
        "java.lang.Float" -> "float"
        "java.lang.Integer" -> "int"
        "java.lang.Long" -> "long"
        "java.lang.Short" -> "short"
        else -> fullyQualifiedName
    }
}
