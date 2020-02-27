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
package org.openrewrite.java

import org.openrewrite.java.tree.Statement
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Type
import org.openrewrite.java.tree.TypeUtils
import org.junit.jupiter.api.Assertions.assertEquals

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

fun Type?.hasElementType(clazz: String) = TypeUtils.hasElementType(this, clazz)

fun Type?.asClass(): Type.Class? = TypeUtils.asClass(this)

fun Type?.asArray(): Type.Array? = TypeUtils.asArray(this)

fun Type?.asGeneric(): Type.GenericTypeVariable? = TypeUtils.asGeneric(this)