/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java

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

fun JavaType?.hasElementType(clazz: String) = TypeUtils.hasElementType(this, clazz)

fun JavaType?.asClass(): JavaType.Class? = TypeUtils.asClass(this)

fun JavaType?.asArray(): JavaType.Array? = TypeUtils.asArray(this)

fun JavaType?.asGeneric(): JavaType.GenericTypeVariable? = TypeUtils.asGeneric(this)