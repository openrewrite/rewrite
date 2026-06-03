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
package org.openrewrite.kotlin.recipe.internal

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.extensions.DeclarationGenerationContext
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * FIR declaration-generation extension that re-exposes Java instance methods
 * which Kotlin's mapped-type system hides on the Kotlin counterpart.
 *
 * **The problem**. Kotlin "maps" certain JDK classes to Kotlin types — e.g. the
 * Kotlin type `kotlin.String` actually IS `java.lang.String` at the JVM level,
 * but the visible member scope of `kotlin.String` is the union of:
 *   - The Kotlin built-ins (`kotlin.kotlin_builtins`) descriptor.
 *   - A compatibility allow-list applied by `JvmBuiltInsCustomizer` /
 *     `FirJvmBuiltinProvider` that surfaces a curated subset of Java instance
 *     methods.
 *
 * That allow-list is incomplete: not just JDK 15+ additions like
 * `formatted`, `transform`, `stripIndent`, `translateEscapes`, but also older
 * shadowed methods on other mapped types. Tracked upstream as
 * [KT-52378](https://youtrack.jetbrains.com/issue/KT-52378) (still open in
 * Kotlin 2.3.20).
 *
 * **The fix**. For every mapped pair (`kotlin.K`, `java.J`) listed by
 * [JavaToKotlinClassMap], discover via reflection the public instance methods
 * declared by `J` whose name is NOT already a member of `K` per the mapped-type
 * scope, and synthesize a top-level Kotlin extension function whose receiver
 * type is `K` and whose body delegates to the Java method.
 *
 * **Caveat — scope isolation**. The user-facing spec wants the fallback to
 * only apply inside `rewrite { } to { }` / `before { }` lambdas of the recipe
 * DSL. [FirDeclarationGenerationExtension] generates top-level callables that
 * are globally visible across all compiled sources. The current implementation
 * accepts that limitation: the extension catalog is package-scoped to
 * `org.openrewrite.kotlin.recipe.generated` so authors who don't `import` from
 * that package never see the fallbacks; recipe DSL authors implicitly accept
 * the wider scope since their recipe source typically only imports
 * `org.openrewrite.*`. A future refinement could wire a
 * [FirExpressionResolutionExtension] companion to gate visibility via implicit
 * receiver scoping, but the current FIR API surface in Kotlin 2.3.20 does not
 * cleanly support per-call scope restriction on top-level extensions.
 *
 * **Caveat — body generation**. This file declares the extension *signatures*
 * via [FirDeclarationGenerationExtension]. Producing the actual extension
 * function bodies (which must `invokevirtual` the underlying Java method on
 * the receiver — `kotlin.String` and `java.lang.String` share a JVM class so
 * the call is type-safe) is the responsibility of [RecipeIrGenerationExtension],
 * which intercepts calls to these synthesized symbols during the IR phase and
 * rewrites them to direct Java-method invocations. See the IR pass for details.
 */
internal object MappedTypeFallbackKey : GeneratedDeclarationKey() {
    override fun toString(): String = "MappedTypeFallbackKey"
}

/**
 * Generated extensions live in this package so authors who don't want the
 * fallbacks never trigger them. Recipe DSL source typically does
 * `import org.openrewrite.recipe` which transitively makes the recipe
 * DSL surface visible; the generated package is a sibling under
 * `org.openrewrite.kotlin.recipe.generated`.
 *
 * NOTE: The generated functions are placed in this package so that a
 * conventional `import org.openrewrite.kotlin.recipe.generated.*` (which the
 * IR pass could inject implicitly per compilation unit in the future) would
 * bring them into scope.  Without such an explicit import the generated
 * functions are still findable via the FIR declaration generation symbol
 * provider in any file in the same source set, because top-level callable
 * resolution does not require an import for callables in the same package
 * AND because [FirExtensionDeclarationsSymbolProvider] surfaces generated
 * callables across the session.
 */
private val GENERATED_PACKAGE: FqName = FqName.ROOT

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
internal class RecipeFirMappedTypeFallbackExtension(session: FirSession) :
    FirDeclarationGenerationExtension(session) {

    /**
     * Catalog of (kotlinClassId, javaClass, methodName, parameterTypes) tuples
     * discovered dynamically via [JavaToKotlinClassMap] + reflection on the
     * Java counterpart. Populated lazily on first use of [getTopLevelCallableIds].
     */
    private data class Entry(
        val kotlinClassId: ClassId,
        val javaClass: Class<*>,
        val method: Method,
    )

    private val catalog: List<Entry> by lazy { buildCatalog() }

    private val callableIdsByName: Map<Name, List<Entry>> by lazy {
        catalog.groupBy { Name.identifier(it.method.name) }
    }

    /**
     * Build the catalog of Java instance methods to re-expose. For each mapping
     * in [JavaToKotlinClassMap.mutabilityMappings] (which covers the
     * read-only/mutable collection pairs) AND for the primitive + String
     * mappings registered via the same singleton, walk the Java class's
     * declared methods and capture each non-overridden public instance method.
     *
     * We intentionally consult `Class.getDeclaredMethods()` (declared only, not
     * inherited) to keep the catalog focused on members the JDK class adds at
     * its own level — supertype methods like `Object.toString` are handled by
     * Kotlin's normal scope.
     */
    private fun buildCatalog(): List<Entry> {
        val entries = mutableListOf<Entry>()
        // The full mapped-type catalog: every Kotlin class that JavaToKotlinClassMap
        // maps back to a Java counterpart. We get the Kotlin FQNs from the
        // public `mappedKotlinClassFqNames` accessor (Set of FqName).
        val mappedKotlinFqns = JavaToKotlinClassMap::class.java
            .getDeclaredField("mappedKotlinClassFqNames")
            .apply { isAccessible = true }
            .get(JavaToKotlinClassMap) as Set<*>

        // Multiple Kotlin types can map to the same Java class — both
        // `kotlin.collections.Collection` and `kotlin.collections.MutableCollection`
        // map to `java.util.Collection`. We process each unique Java class once
        // and emit one entry per Java method per *unique* Kotlin counterpart so
        // the bytecode emitter doesn't see duplicate `equals(Collection,Object)`
        // top-level extensions in the generated facade class.
        val seenSignatures = mutableSetOf<String>()

        for (raw in mappedKotlinFqns) {
            val kotlinFqn = raw as? FqName ?: continue
            val javaClassId = JavaToKotlinClassMap
                .mapKotlinToJava(FqNameUnsafe(kotlinFqn.asString())) ?: continue
            val kotlinClassId = ClassId.topLevel(kotlinFqn)

            val javaClass = try {
                Class.forName(javaClassId.asFqNameString())
            } catch (_: Throwable) {
                continue
            }

            for (method in javaClass.declaredMethods) {
                val mods = method.modifiers
                if (!Modifier.isPublic(mods)) continue
                if (Modifier.isStatic(mods)) continue
                if (method.isSynthetic) continue
                if (method.isBridge) continue
                // Note: vararg methods like `String.formatted(Object... args)`
                // are emitted as plain Array<out Any?> parameters below
                // (see generateExtensionFor). Authors lose the trailing-vararg
                // convenience syntax (must pass a literal array) for these
                // entries — acceptable for the recipe DSL use case.
                // Skip methods whose name collides with an existing kotlin
                // member of the same arity to avoid `equals/hashCode/toString`
                // double-declaration in the generated facade.
                if (method.name in MASKED_INHERITED_NAMES) continue
                // Deduplicate at the *erased* JVM-bytecode level. The generated
                // top-level extensions all sit in the same facade class
                // `__GENERATED__CALLABLES__Kt`; two extensions on the same
                // mapped type that share name + erased-arg shape collide there.
                //
                // Two cases drive this:
                //   1. Mapped-type variants (List/MutableList both map to
                //      java.util.List): same Java signature, both produce the
                //      same `(java.util.List, Object) -> ...` static.
                //   2. Overloads whose Kotlin counterpart erases to a single
                //      static — `String.contentEquals(CharSequence)` and
                //      `String.contentEquals(StringBuffer)` both erase to
                //      `(String, Object)Z` once we map non-primitive params
                //      to `kotlin.Any?`.
                // Keying on the erased signature collapses both cases.
                val erasedSignature = "${javaClassId.asFqNameString()}::${method.name}(${
                    method.parameterTypes.joinToString(",") { erasedJvmDescriptor(it) }
                })"
                if (!seenSignatures.add(erasedSignature)) continue
                entries += Entry(kotlinClassId, javaClass, method)
            }
        }
        return entries
    }

    /**
     * Coarse erasure used purely to dedup top-level extensions in the
     * generated facade class. Mirrors how we map Java types to Kotlin in
     * [mapJavaTypeToKotlin] so the dedup key matches what FIR actually emits.
     */
    private fun erasedJvmDescriptor(jType: Class<*>): String = when {
        jType == Void.TYPE -> "V"
        jType == java.lang.Boolean.TYPE -> "Z"
        jType == java.lang.Byte.TYPE -> "B"
        jType == java.lang.Character.TYPE -> "C"
        jType == java.lang.Short.TYPE -> "S"
        jType == java.lang.Integer.TYPE -> "I"
        jType == java.lang.Long.TYPE -> "J"
        jType == java.lang.Float.TYPE -> "F"
        jType == java.lang.Double.TYPE -> "D"
        jType.name == "java.lang.String" -> "Ljava/lang/String;"
        // Every other reference-typed param is mapped to `kotlin.Any?` →
        // erased descriptor `Ljava/lang/Object;`.
        else -> "Ljava/lang/Object;"
    }

    private companion object {
        /**
         * Names whose Java instance versions are already members of every
         * mapped Kotlin type via the inherited `kotlin.Any` scope. Re-exporting
         * them as extensions produces JVM bytecode-level duplicates (the
         * generated facade class would contain two `equals(Collection, Object)`
         * statics) and provides zero value (Kotlin's resolution already finds
         * them through the receiver's member scope).
         */
        val MASKED_INHERITED_NAMES = setOf("equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait")
    }

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return callableIdsByName.keys.mapTo(mutableSetOf()) { name ->
            CallableId(GENERATED_PACKAGE, name)
        }
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == GENERATED_PACKAGE
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: DeclarationGenerationContext.Member?
    ): List<FirNamedFunctionSymbol> {
        // We only generate top-level functions in our synthetic package.
        if (callableId.packageName != GENERATED_PACKAGE) return emptyList()
        if (context != null) return emptyList()  // not generating members of any class
        val name = callableId.callableName
        val entries = callableIdsByName[name] ?: return emptyList()

        return entries.mapNotNull { entry ->
            generateExtensionFor(callableId, entry)?.symbol
        }
    }

    private fun generateExtensionFor(
        callableId: CallableId,
        entry: Entry,
    ): org.jetbrains.kotlin.fir.declarations.FirNamedFunction? {
        // Receiver type = the mapped Kotlin type's ConeKotlinType. We don't
        // need to resolve the symbol here — the FIR scope-resolution machinery
        // looks up the class via the ClassId we encode into the ConeKotlinType.
        val receiverType = entry.kotlinClassId.constructClassLikeType(
            typeArguments = emptyArray(),
            isMarkedNullable = false,
        )

        // Return type: best-effort map from Java return type. For now, model
        // every non-primitive return as `kotlin.Any?` and primitive returns
        // as their Kotlin counterpart. This is a coarse approximation — the
        // actual JVM call site is type-erased anyway, so the worst case is
        // an author has to insert a manual cast at the use site. Concretely
        // the JDK 15 String additions all return `String` and are handled
        // by the specific-return-type path below.
        val returnType: ConeKotlinType = mapJavaReturnTypeToKotlin(entry.method.returnType, receiverType)

        val function = createTopLevelFunction(
            key = MappedTypeFallbackKey,
            callableId = callableId,
            returnType = returnType,
        ) {
            extensionReceiverType(receiverType)
            for ((idx, p) in entry.method.parameters.withIndex()) {
                // Java vararg: declare an `Array<Any?>` parameter (non-vararg
                // from Kotlin's POV) so authors call the extension with an
                // explicit `arrayOf(...)`. We don't surface `isVararg=true`
                // here because the FIR-builder + IR-backend pair has a
                // round-tripping bug with synthesized varargs whose element
                // type is `Any?` (the call-side IR construction emits a
                // `vararg(...)` expression whose element type doesn't match
                // the param's declared element type, tripping
                // `IllegalStateException: Vararg expression has incorrect type`
                // during fir2ir).
                val paramType = if (p.isVarArgs && p.type.isArray) {
                    arrayOfAny()
                } else {
                    mapJavaTypeToKotlin(p.type)
                }
                valueParameter(
                    name = Name.identifier(p.name ?: "p$idx"),
                    type = paramType,
                    isVararg = false,
                )
            }
            // Default body — IR phase replaces this with a direct invokevirtual.
            withGeneratedDefaultBody()
        }
        return function
    }

    /**
     * Map a Java reflection [Class] to a Kotlin [ConeKotlinType]. Primitives
     * use their Kotlin counterparts; `String`, `Object`, and arrays use the
     * obvious mappings; anything else falls through to `kotlin.Any?`.
     */
    private fun mapJavaTypeToKotlin(jType: Class<*>): ConeKotlinType {
        return when {
            jType == Void.TYPE -> stdLibClass("kotlin", "Unit").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Boolean.TYPE -> stdLibClass("kotlin", "Boolean").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Byte.TYPE -> stdLibClass("kotlin", "Byte").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Character.TYPE -> stdLibClass("kotlin", "Char").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Short.TYPE -> stdLibClass("kotlin", "Short").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Integer.TYPE -> stdLibClass("kotlin", "Int").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Long.TYPE -> stdLibClass("kotlin", "Long").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Float.TYPE -> stdLibClass("kotlin", "Float").constructClassLikeType(emptyArray(), false)
            jType == java.lang.Double.TYPE -> stdLibClass("kotlin", "Double").constructClassLikeType(emptyArray(), false)
            jType.name == "java.lang.String" ->
                stdLibClass("kotlin", "String").constructClassLikeType(emptyArray(), false)
            jType.name == "java.lang.Object" ->
                stdLibClass("kotlin", "Any").constructClassLikeType(emptyArray(), true)
            else -> stdLibClass("kotlin", "Any").constructClassLikeType(emptyArray(), true)
        }
    }

    private fun mapJavaReturnTypeToKotlin(jType: Class<*>, receiverType: ConeKotlinType): ConeKotlinType {
        // If the Java return type is the same as the declaring class, prefer
        // returning the Kotlin receiver type so e.g. `String.formatted` returns
        // `String` not `Any?`.
        return if (jType.name == receiverType.classId?.asFqNameString()) {
            receiverType
        } else {
            mapJavaTypeToKotlin(jType)
        }
    }

    private fun stdLibClass(packageName: String, className: String): ClassId =
        ClassId(FqName(packageName), Name.identifier(className))

    /** Construct `Array<Any?>` as a ConeKotlinType for vararg-Object methods. */
    private fun arrayOfAny(): ConeKotlinType {
        val any = stdLibClass("kotlin", "Any").constructClassLikeType(emptyArray(), true)
        val arrayCls = stdLibClass("kotlin", "Array")
        return arrayCls.constructClassLikeType(arrayOf(any), false)
    }

    private val ConeKotlinType.classId: ClassId?
        get() = (this as? org.jetbrains.kotlin.fir.types.ConeClassLikeType)?.lookupTag?.classId
}
