/*
 * Copyright 2026 the original author or authors.
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
@file:JvmName("HotPath")
package org.openrewrite.kotlin

import org.openrewrite.Cursor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Loop

// Detection helpers for "is the current cursor position inside a hot path?" — the kind
// of code position where allocations, regex constructions, reflective lookups, etc.
// matter most. Recipe authors compose these to gate performance rewrites.
//
// Semantics: direct lexical containment, not transitive-callee analysis. A function
// declared inside a loop body counts as inside the loop; a function defined elsewhere
// and called from a loop does NOT count without an opt-in `@HotPath` marker.

private val HOT_COLLECTION_METHODS: Set<String> = setOf(
    "forEach", "forEachIndexed",
    "map", "mapIndexed", "mapNotNull", "mapNotNullTo", "mapTo",
    "flatMap", "flatMapTo",
    "filter", "filterNot", "filterNotNull", "filterTo", "filterNotTo",
    "onEach", "onEachIndexed",
    "fold", "foldRight", "reduce", "reduceRight", "scan", "runningFold", "runningReduce",
    "sumOf", "minOf", "maxOf",
    "any", "all", "none", "count",
    "first", "firstOrNull", "last", "lastOrNull",
    "find", "findLast",
    "associate", "associateBy", "associateWith",
    "groupBy", "partition",
)

/** True if the cursor sits inside a Java/Kotlin for/while/do-while loop body. */
public fun Cursor.isInsideLoop(): Boolean =
    firstEnclosing(Loop::class.java) != null

/**
 * True if the cursor sits inside the trailing lambda of a hot Kotlin collection /
 * sequence operation (`forEach`, `map`, `filter`, etc.). Caller-controlled allocations
 * in these positions run per element.
 *
 * Walks the cursor path upward: finds the first enclosing `J.Lambda`, then keeps
 * walking to the next enclosing `J.MethodInvocation` (the one whose argument list
 * contains the lambda), and checks its name against the hot-method set.
 */
public fun Cursor.isInsideHotCollectionLambda(): Boolean {
    val path = getPath()
    var foundLambda = false
    while (path.hasNext()) {
        val v = path.next()
        if (v is J.Lambda) {
            foundLambda = true
        } else if (foundLambda && v is J.MethodInvocation) {
            return v.simpleName in HOT_COLLECTION_METHODS
        }
    }
    return false
}

/** True if the cursor sits inside an `@Composable` function body. */
public fun Cursor.isInsideComposable(): Boolean =
    isInsideMethodAnnotated("Composable")

/** True if the cursor sits inside a `View.onDraw(Canvas)` override. */
public fun Cursor.isInsideOnDraw(): Boolean =
    isInsideOverridingMethodNamed("onDraw")

/** True if the cursor sits inside a `View.onMeasure(int, int)` override. */
public fun Cursor.isInsideOnMeasure(): Boolean =
    isInsideOverridingMethodNamed("onMeasure")

/** True if the cursor sits inside a `View.onLayout(boolean, int, int, int, int)` override. */
public fun Cursor.isInsideOnLayout(): Boolean =
    isInsideOverridingMethodNamed("onLayout")

/**
 * True if the cursor sits inside a function annotated `@HotPath`. Opt-in escape hatch
 * for recipe authors who want performance recipes to fire on a function reached only
 * from hot paths but not lexically contained in a loop / Composable / etc.
 */
public fun Cursor.isInsideHotPathAnnotated(): Boolean =
    isInsideMethodAnnotated("HotPath")

/**
 * Composite: true if the cursor sits in any recognized hot path position. Use this as
 * the default gate in performance recipes; reach for the individual helpers only when
 * a recipe applies in some hot positions but not others.
 */
public fun Cursor.isInsideHotPath(): Boolean =
    isInsideLoop()
        || isInsideHotCollectionLambda()
        || isInsideComposable()
        || isInsideOnDraw()
        || isInsideOnMeasure()
        || isInsideOnLayout()
        || isInsideHotPathAnnotated()

private fun Cursor.isInsideMethodAnnotated(annotationSimpleName: String): Boolean {
    val method = firstEnclosing(J.MethodDeclaration::class.java) ?: return false
    return method.leadingAnnotations.any { it.simpleName == annotationSimpleName }
}

private fun Cursor.isInsideOverridingMethodNamed(name: String): Boolean {
    val method = firstEnclosing(J.MethodDeclaration::class.java) ?: return false
    if (method.simpleName != name) return false
    return method.leadingAnnotations.any { it.simpleName == "Override" }
}
