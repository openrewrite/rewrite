package com.netflix.java.refactor.ast

import org.junit.Assert
import org.junit.Assert.assertEquals

/**
 * The first statement of the first method in the first class declaration
 */
fun Tr.CompilationUnit.firstMethodStatement() =
        classes[0].methods()[0].body!!.statements[0]

fun Tr.CompilationUnit.fields(ns: IntRange = 0..0) =
        classes[0].fields().subList(ns.start, ns.endInclusive + 1)

fun assertRefactored(cu: Tr.CompilationUnit, refactored: String) {
    assertEquals(refactored.trimMargin(), cu.printTrimmed())
}