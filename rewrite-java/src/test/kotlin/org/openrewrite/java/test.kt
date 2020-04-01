package org.openrewrite.java

import org.junit.jupiter.api.Assertions.assertEquals
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Statement
import org.openrewrite.java.tree.TypeUtils

/**
 * The first statement of the first method in the first class declaration
 */
fun J.CompilationUnit.firstMethodStatement(): Statement =
        classes[0].methods[0].body!!.statements[0]

fun J.CompilationUnit.fields(ns: IntRange = 0..0) =
        classes[0].fields.subList(ns.first, ns.last + 1)

fun assertRefactored(cu: J.CompilationUnit, refactored: String) {
    assertEquals(refactored.trimIndent(), cu.printTrimmed())
}

fun JavaType?.hasElementType(clazz: String) = TypeUtils.hasElementType(this, clazz)

fun JavaType?.asClass(): JavaType.Class? = TypeUtils.asClass(this)

fun JavaType?.asArray(): JavaType.Array? = TypeUtils.asArray(this)

fun JavaType?.asGeneric(): JavaType.GenericTypeVariable? = TypeUtils.asGeneric(this)