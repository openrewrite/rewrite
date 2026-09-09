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
@file:OptIn(
    org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class,
    // `IrFunction.valueParameters` and `IrMemberAccessExpression.getValueArgument`
    // were marked for removal in Kotlin 2.1.20 in favour of the unified
    // IrParameterKind-based `parameters` list. The replacement isn't stable
    // across 2.x patch versions for top-level Java functions yet, so we keep
    // using the older accessors and opt in to the deprecation.
    org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi::class,
)
package org.openrewrite.kotlin.recipe.internal

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMemberWithContainerSource
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import java.io.File

/**
 * IR-phase code generator for the recipe authoring DSL — narrow Phase 3 scope.
 *
 * The runtime DSL builder in `RecipeDsl.kt` produces working Recipe /
 * ScanningRecipe instances for the IMPERATIVE shapes (`edit { lang { visitX { } } }`
 * and `scan<A>(initial) { … }.edit { … }`) WITHOUT this IR pass. Phase 3
 * intercepts ONLY the declarative pattern shape:
 *
 *     val UseAppendLine: Recipe = recipe(...) {
 *         edit { rewrite { sb: StringBuilder -> sb.appendln() } to { sb -> sb.appendLine() } }
 *     }
 *
 * For these recipes the runtime DSL's `rewrite { } to { }` is an `error(...)`
 * stub that throws on `getVisitor()`. This pass replaces the entire
 * `recipe(...)` call with a synthetic `<Name>$KtRecipe` constructor call;
 * the synthetic class extends `Recipe` and overrides `getVisitor()` to delegate
 * to `GeneratedRecipeSupport.methodInvocationRewrite[Java](spec, template, csv)`.
 *
 * The LST-structural classifier picks between `methodInvocationRewriteJava`
 * (default — a `JavaVisitor` walks both Java and Kotlin sources via
 * `TreeVisitorAdapter`) and `methodInvocationRewrite` (Kotlin), promoting to
 * the Kotlin variant only when the before/after lambdas reference a `K.*` LST
 * node. Method-name / callee-package signals are deliberately NOT used (see
 * plan §Design.4) — the MethodMatcher spec, resolved at FIR time pre-inline,
 * already encodes Kotlin-extension targets correctly even when the visitor is
 * Java-rooted.
 *
 * What this pass does NOT do (intentional):
 *  - Imperative `edit { lang { visitX { } } }` — handled by the runtime
 *    builder. No IR rewriting needed; deferred static-class optimization is
 *    captured in plan §"Deliberately deferred".
 *  - `scan<A>(initial) { … }.edit { … }` chains — handled by the runtime
 *    builder via `AtomicReference<A>`.
 *  - Mixed shapes that compose `rewrite { } to { }` with other statements in
 *    the same edit block — v1 only handles the canonical "single
 *    `rewrite { } to { }` inside a bare `edit { }`" shape; mixed shapes leave
 *    the runtime stub in place and fail at `getVisitor()` time.
 */
internal class RecipeIrGenerationExtension : IrGenerationExtension {

    private companion object {
        val RECIPE_FQN: FqName = FqName("org.openrewrite.recipe")
        val RECIPES_FQN: FqName = FqName("org.openrewrite.recipes")

        val REWRITE_ADVICE_TO_FQNS: Set<String> = (0..12).map { "org.openrewrite.RewriteAdvice$it.to" }.toSet()

        /** The `.strictArity()` opt-out modifier wrapping a `to` call. */
        const val REWRITE_RULE_STRICT_ARITY_FQN = "org.openrewrite.RewriteRule.strictArity"

        const val EDIT_SCOPE_REWRITE_FQN = "org.openrewrite.EditScope.rewrite"
        const val RECIPE_BUILDER_EDIT_FQN = "org.openrewrite.RecipeBuilder.edit"

        /**
         * K2 FIR2IR represents `expr!!` as a call to this synthetic intrinsic,
         * taking the asserted expression as the (only) value argument. See
         * `org.jetbrains.kotlin.ir.expressions.IrConstantValueImpl` and
         * `FirNotNullableTransformer` in the Kotlin compiler.
         */
        const val CHECK_NOT_NULL_FQN = "kotlin.internal.ir.CHECK_NOT_NULL"

        /**
         * Sentinel that marks the variadic-run position in a generated after
         * template. The runtime ([GeneratedRecipeSupport]) replaces it with the
         * right number of `#{any()}` placeholders for the matched call's args.
         * Control-char-fenced so it can never collide with real source text.
         */
        const val VARARGS_SENTINEL = "VARARGS"

        /**
         * Kotlin builtin → Java FQN, for typing the fixed prefix params of a
         * varargs MethodMatcher spec. `JavaType.Method` carries Java types even
         * for Kotlin sources, so `kotlin.String` must be spelled
         * `java.lang.String` for the matcher to fire.
         */
        val KOTLIN_BUILTIN_TO_JAVA_FQN: Map<String, String> = mapOf(
            "kotlin.String" to "java.lang.String",
            "kotlin.CharSequence" to "java.lang.CharSequence",
            "kotlin.Any" to "java.lang.Object",
            "kotlin.Throwable" to "java.lang.Throwable",
            "kotlin.Int" to "int",
            "kotlin.Long" to "long",
            "kotlin.Short" to "short",
            "kotlin.Byte" to "byte",
            "kotlin.Boolean" to "boolean",
            "kotlin.Char" to "char",
            "kotlin.Float" to "float",
            "kotlin.Double" to "double",
        )
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val ctx = buildIrGenContext(pluginContext) ?: return
        for (file in moduleFragment.files) {
            processFile(file, ctx)
        }
        // Replace the FIR-side default bodies of mapped-type fallback
        // extensions (synthesized in `RecipeFirMappedTypeFallbackExtension`)
        // with actual calls to the underlying Java instance method. Without
        // this pass the generated extensions would throw the FIR-default
        // stub at runtime.
        MappedTypeFallbackBodyGenerator.generate(moduleFragment, pluginContext)
    }

    // ------------------------------------------------------------------
    // Symbol resolution — done once per compilation unit.
    // ------------------------------------------------------------------

    /** Container for the pre-resolved IR symbols the pass refers to repeatedly. */
    private class RecipeIrGenContext(
        val pluginContext: IrPluginContext,
        val recipeClassSymbol: IrClassSymbol,
        val recipeNoArgCtorSymbol: IrConstructorSymbol,
        val getDisplayName: IrSimpleFunction,
        val getDescription: IrSimpleFunction,
        val getTags: IrSimpleFunction?,
        val getEstimatedEffort: IrSimpleFunction?,
        val getVisitor: IrSimpleFunction?,
        val getRecipeList: IrSimpleFunction?,
        val listOfVarargSymbol: IrSimpleFunctionSymbol?,
        val methodInvocationRewriteKotlinSymbol: IrSimpleFunctionSymbol?,
        val methodInvocationRewriteJavaSymbol: IrSimpleFunctionSymbol?,
        val methodInvocationRewriteKotlinNotNullSymbol: IrSimpleFunctionSymbol?,
        val propertyAccessRewriteKotlinSymbol: IrSimpleFunctionSymbol?,
        /**
         * Top-level `org.openrewrite.buildImperativeVisitor` Kotlin helper.
         * The IR pass routes imperative-shape recipes to this helper so the
         * generated class can stay field-less (Jackson-roundtrippable) while
         * still constructing the visitor pipeline fresh on each `getVisitor()`
         * call. Null when the helper isn't on the classpath (older
         * `rewrite-kotlin`) — imperative recipes fall back to the runtime
         * DSL's anonymous-Recipe path.
         */
        val buildImperativeVisitorSymbol: IrSimpleFunctionSymbol?,
    )

    private fun buildIrGenContext(pluginContext: IrPluginContext): RecipeIrGenContext? {
        val recipeClassId = ClassId.topLevel(FqName("org.openrewrite.Recipe"))
        val recipeClassSymbol = pluginContext.referenceClass(recipeClassId) ?: return null
        val recipeNoArgCtor = pluginContext.referenceConstructors(recipeClassId)
            .singleOrNull { it.owner.valueParameters.isEmpty() } ?: return null

        val recipeMembers = recipeClassSymbol.owner.declarations.filterIsInstance<IrSimpleFunction>()
        val getDisplayName = recipeMembers.firstOrNull {
            it.name.asString() == "getDisplayName" && it.valueParameters.isEmpty()
        } ?: return null
        val getDescription = recipeMembers.firstOrNull {
            it.name.asString() == "getDescription" && it.valueParameters.isEmpty()
        } ?: return null
        val getTags = recipeMembers.firstOrNull {
            it.name.asString() == "getTags" && it.valueParameters.isEmpty()
        }
        val getEstimatedEffort = recipeMembers.firstOrNull {
            it.name.asString() == "getEstimatedEffortPerOccurrence" && it.valueParameters.isEmpty()
        }
        val getVisitor = recipeMembers.firstOrNull {
            it.name.asString() == "getVisitor" && it.valueParameters.isEmpty()
        }
        val getRecipeList = recipeMembers.firstOrNull {
            it.name.asString() == "getRecipeList" && it.valueParameters.isEmpty()
        }

        // Resolve the vararg overload of `kotlin.collections.listOf` so the
        // composite `<Name>$KtRecipe.getRecipeList()` body can wrap the original
        // `recipes(...)` vararg in a `List<Recipe>`.
        val listOfFqn = FqName("kotlin.collections.listOf")
        val listOfVarargSymbol = pluginContext
            .referenceFunctions(org.jetbrains.kotlin.name.CallableId(
                packageName = listOfFqn.parent(),
                callableName = listOfFqn.shortName(),
            ))
            .firstOrNull { fn ->
                fn.owner.valueParameters.size == 1 && fn.owner.valueParameters[0].varargElementType != null
            }

        val supportClassId = ClassId.topLevel(FqName("org.openrewrite.kotlin.recipe.GeneratedRecipeSupport"))
        val supportClassSymbol = pluginContext.referenceClass(supportClassId)
        // Resolve by name (each helper name is unique). Arity varies: the two
        // method-invocation helpers carry a 4th `strictArgCount` param; the
        // not-null / property helpers stay at 3.
        val supportFns = supportClassSymbol
            ?.owner?.declarations
            ?.filterIsInstance<IrSimpleFunction>()
            ?.filter { it.dispatchReceiverParameter == null }
            .orEmpty()
        val kotlinHelper = supportFns.firstOrNull { it.name.asString() == "methodInvocationRewrite" }?.symbol
        val javaHelper = supportFns.firstOrNull { it.name.asString() == "methodInvocationRewriteJava" }?.symbol
        val kotlinNotNullHelper = supportFns.firstOrNull {
            it.name.asString() == "methodInvocationRewriteKotlinNotNull"
        }?.symbol
        val propertyAccessHelper = supportFns.firstOrNull {
            it.name.asString() == "propertyAccessRewrite"
        }?.symbol

        // Top-level helper in `org.openrewrite.RecipeDslKt`. Resolved via
        // `referenceFunctions` against the FqName of the standalone declaration.
        val buildImperativeVisitorFqn = FqName("org.openrewrite.buildImperativeVisitor")
        val buildImperativeVisitorSymbol = pluginContext
            .referenceFunctions(org.jetbrains.kotlin.name.CallableId(
                packageName = buildImperativeVisitorFqn.parent(),
                callableName = buildImperativeVisitorFqn.shortName(),
            ))
            .singleOrNull { it.owner.valueParameters.size == 1 }

        return RecipeIrGenContext(
            pluginContext = pluginContext,
            recipeClassSymbol = recipeClassSymbol,
            recipeNoArgCtorSymbol = recipeNoArgCtor,
            getDisplayName = getDisplayName,
            getDescription = getDescription,
            getTags = getTags,
            getEstimatedEffort = getEstimatedEffort,
            getVisitor = getVisitor,
            getRecipeList = getRecipeList,
            listOfVarargSymbol = listOfVarargSymbol,
            methodInvocationRewriteKotlinSymbol = kotlinHelper,
            methodInvocationRewriteJavaSymbol = javaHelper,
            methodInvocationRewriteKotlinNotNullSymbol = kotlinNotNullHelper,
            propertyAccessRewriteKotlinSymbol = propertyAccessHelper,
            buildImperativeVisitorSymbol = buildImperativeVisitorSymbol,
        )
    }

    // ------------------------------------------------------------------
    // File walking — identify recipe properties, build replacement classes.
    // ------------------------------------------------------------------

    private fun processFile(file: IrFile, ctx: RecipeIrGenContext) {
        val replacements: MutableMap<IrCall, IrConstructorCall> = LinkedHashMap()

        // Source recovery for lambda → template string substitution. PSI is
        // stripped by the IR phase; the file path on the IrFileEntry is the
        // reliable handle.
        val sourceText: String? = run {
            val path = file.fileEntry.name
            val onDisk = File(path)
            if (onDisk.isFile) onDisk.readText() else null
        }
        if (sourceText == null) return  // no source = no template extraction possible

        // Iterate over a snapshot — `addChild` mutates `file.declarations` while
        // we add synthetic Recipe subclasses, which would otherwise trip a
        // ConcurrentModificationException on the underlying list iterator.
        for (declaration in file.declarations.toList()) {
            if (declaration !is IrProperty) continue
            val initializerExpr = declaration.backingField?.initializer?.expression as? IrCall ?: continue
            // `IrUtilsKt.hasTopLevelEqualFqName` returns false for callees whose
            // parent is an `IrExternalPackageFragmentImpl` (functions resolved
            // out of a dependency jar — exactly our case). Compare FqName.
            val callFqn = initializerExpr.symbol.owner.kotlinFqName
            val generatedClass: IrClass = when (callFqn) {
                RECIPE_FQN -> {
                    val metadata = readMetadata(initializerExpr) ?: continue
                    // Two paths share the rest of the loop:
                    //   - declarative: `edit { rewrite { } to { } }` — synthesizes a
                    //     visitor whose body is the IR-derived template helper call.
                    //   - imperative:  `edit { lang { visitX { } } }` (or anything
                    //     else not matching the declarative shape) — synthesizes a
                    //     visitor whose body delegates back to the runtime DSL via
                    //     `buildImperativeVisitor`, threading the original recipe
                    //     trailing lambda through a fresh [RecipeBuilder] per call.
                    // Both produce a field-less top-level class so Jackson roundtrip
                    // succeeds (no `validateRecipeSerialization(false)` workaround).
                    val templates = extractRewriteTemplates(initializerExpr, sourceText, ctx)
                    if (templates != null) {
                        val helperSymbol = pickHelperSymbol(templates, ctx) ?: continue
                        buildGeneratedRecipeClass(
                            ctx = ctx,
                            parentFile = file,
                            propertyName = declaration.name,
                            metadata = metadata,
                            templates = templates,
                            helperSymbol = helperSymbol,
                        )
                    } else {
                        val imperativeBlock = findTrailingLambda(initializerExpr) ?: continue
                        val helperSymbol = ctx.buildImperativeVisitorSymbol ?: continue
                        buildImperativeRecipeClass(
                            ctx = ctx,
                            parentFile = file,
                            propertyName = declaration.name,
                            metadata = metadata,
                            recipeBlock = imperativeBlock,
                            helperSymbol = helperSymbol,
                        )
                    }
                }
                RECIPES_FQN -> {
                    val compositeMetadata = readCompositeMetadata(initializerExpr) ?: continue
                    if (ctx.getRecipeList == null || ctx.listOfVarargSymbol == null) continue
                    buildCompositeRecipeClass(
                        ctx = ctx,
                        parentFile = file,
                        propertyName = declaration.name,
                        metadata = compositeMetadata,
                    )
                }
                else -> continue
            }
            file.addChild(generatedClass)

            val generatedCtor = generatedClass.declarations
                .filterIsInstance<IrConstructor>()
                .single().symbol

            val builder = DeclarationIrBuilder(
                generatorContext = ctx.pluginContext,
                symbol = declaration.symbol,
                startOffset = initializerExpr.startOffset,
                endOffset = initializerExpr.endOffset,
            )
            replacements[initializerExpr] = builder.irCallConstructor(
                callee = generatedCtor,
                typeArguments = emptyList(),
            )
        }

        if (replacements.isEmpty()) return

        file.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                replacements[expression]?.let { return it }
                return super.visitCall(expression)
            }
        })
    }

    // ------------------------------------------------------------------
    // Metadata extraction.
    // ------------------------------------------------------------------

    private class RecipeMetadata(
        val displayName: String,
        val description: String,
        val tagsArg: IrExpression?,
        val estimatedEffortArg: IrExpression?,
    )

    private fun readMetadata(call: IrCall): RecipeMetadata? {
        val callee = call.symbol.owner
        val params = callee.valueParameters
        val displayNameIdx = params.indexOfFirst { it.name == Name.identifier("displayName") }
        val descriptionIdx = params.indexOfFirst { it.name == Name.identifier("description") }
        if (displayNameIdx < 0 || descriptionIdx < 0) return null
        val displayName = evalConstString(call.arguments[displayNameIdx]) ?: return null
        val description = evalConstString(call.arguments[descriptionIdx]) ?: return null

        val tagsIdx = params.indexOfFirst { it.name == Name.identifier("tags") }
        val tagsArg = if (tagsIdx >= 0) substantiveArgOrNull(call.arguments[tagsIdx]) else null

        val effortIdx = params.indexOfFirst { it.name == Name.identifier("estimatedEffortPerOccurrence") }
        val effortArg = if (effortIdx >= 0) nonNullArgOrNull(call.arguments[effortIdx]) else null

        return RecipeMetadata(displayName, description, tagsArg, effortArg)
    }

    private class CompositeRecipeMetadata(
        val displayName: String,
        val description: String,
        val recipesVararg: org.jetbrains.kotlin.ir.expressions.IrVararg,
    )

    private fun readCompositeMetadata(call: IrCall): CompositeRecipeMetadata? {
        val callee = call.symbol.owner
        val params = callee.valueParameters
        val displayNameIdx = params.indexOfFirst { it.name == Name.identifier("displayName") }
        val descriptionIdx = params.indexOfFirst { it.name == Name.identifier("description") }
        val recipesIdx = params.indexOfFirst { it.name == Name.identifier("recipes") }
        if (displayNameIdx < 0 || descriptionIdx < 0 || recipesIdx < 0) return null
        val displayName = evalConstString(call.arguments[displayNameIdx]) ?: return null
        val description = evalConstString(call.arguments[descriptionIdx]) ?: return null
        val recipesVararg = call.arguments[recipesIdx] as? org.jetbrains.kotlin.ir.expressions.IrVararg ?: return null
        return CompositeRecipeMetadata(displayName, description, recipesVararg)
    }

    private fun nonNullArgOrNull(arg: IrExpression?): IrExpression? {
        if (arg == null) return null
        if (arg is IrConst && arg.value == null) return null
        return arg
    }

    private fun substantiveArgOrNull(arg: IrExpression?): IrExpression? {
        if (arg == null) return null
        if (arg is IrCall) {
            val fqn = arg.symbol.owner.kotlinFqName.asString()
            if (fqn == "kotlin.collections.emptySet") return null
            if (fqn == "kotlin.collections.setOf") {
                val singleArg = arg.arguments.singleOrNull() ?: return arg
                if (singleArg is org.jetbrains.kotlin.ir.expressions.IrVararg && singleArg.elements.isEmpty()) return null
            }
        }
        return arg
    }

    /**
     * Fold a `displayName` / `description` argument to a compile-time constant
     * String. This runs BEFORE the IR const-evaluation lowering, so even a
     * literal `"a" + "b"` is still an unlowered `IrCall`/`IrStringConcatenation`
     * here — a plain `as? IrConst` would miss it and silently drop the recipe.
     */
    private fun evalConstString(expr: IrExpression?): String? {
        return when (expr) {
            null -> null
            is IrConst -> expr.value?.toString()
            is IrStringConcatenation -> buildString {
                for (part in expr.arguments) append(evalConstString(part) ?: return null)
            }
            is IrCall -> when (expr.symbol.owner.kotlinFqName.asString()) {
                "kotlin.String.plus" -> {
                    val left = evalConstString(expr.arguments.getOrNull(0)) ?: return null
                    val right = evalConstString(expr.arguments.getOrNull(1)) ?: return null
                    left + right
                }
                "kotlin.text.trimIndent" -> evalConstString(expr.arguments.getOrNull(0))?.trimIndent()
                "kotlin.text.trimMargin" -> {
                    val receiver = evalConstString(expr.arguments.getOrNull(0)) ?: return null
                    val marginArg = expr.arguments.getOrNull(1)
                    if (marginArg == null) receiver.trimMargin()
                    else receiver.trimMargin(evalConstString(marginArg) ?: return null)
                }
                else -> null
            }
            else -> null
        }
    }

    // ------------------------------------------------------------------
    // Pattern extraction — `edit { rewrite { } to { } }` shape.
    // ------------------------------------------------------------------

    /**
     * Synthesizer inputs for a `rewrite { } to { }` recipe. `matcherSpecs` is
     * one MethodMatcher spec per before-lambda (multi-before shape:
     * `rewrite(b1, b2) to a`); the runtime helper builds one matcher per spec
     * and accepts when any match. `afterTemplate` + `substitutionSourcesCsv`
     * are described on [GeneratedRecipeSupport.methodInvocationRewrite].
     *
     * [usesKotlinTreeNode] is set when the before/after lambdas structurally
     * reference a `K.*` LST node — promotes the visitor to Kotlin.
     */
    private class RewriteTemplates(
        val matcherSpecs: List<String>,
        val afterTemplate: String,
        val substitutionSourcesCsv: String,
        val usesKotlinTreeNode: Boolean,
        /**
         * True when every before lambda was a `someCall()!!` pattern. The
         * helper selection routes to a `K.Unary(NotNull)`-walking visitor so
         * the rewrite replaces the entire not-null-asserted expression.
         */
        val wrappedInNotNull: Boolean,
        /**
         * True when every before lambda was a property-access pattern
         * (`{ d: Duration -> d.inHours }`) rather than a method invocation.
         * Routes the helper selection to a `J.FieldAccess`-walking visitor.
         */
        val propertyAccess: Boolean,
        /**
         * For `.strictArity()` recipes on a varargs callee: the exact number of
         * call-site arguments to require at runtime (the matcher is still `..`
         * because a varargs method can only be matched by `..`). -1 disables the
         * guard (variadic-by-default, or a non-varargs callee).
         */
        val strictArgCount: Int,
    )

    private fun pickHelperSymbol(
        templates: RewriteTemplates,
        ctx: RecipeIrGenContext,
    ): IrSimpleFunctionSymbol? {
        // v2: route all-Java method-invocation bodies through the Java helper
        // (JavaVisitor + JavaTemplate). The LST-structural classifier decides
        // "all-Java": if the before/after lambdas don't structurally reference
        // any K.* tree node, JavaVisitor matches the same call sites against
        // both Java and Kotlin sources (via TreeVisitorAdapter) and
        // JavaTemplate can parse the after template — none of the after-
        // template syntax that's Kotlin-only (trailing lambdas, `<Type>` arg
        // lists, extension calls, `..<`) can appear without dragging a K.*
        // reference into the lambdas. So the classifier is sufficient.
        //
        // Three Kotlin-only shapes are never safe to route to Java:
        //   - property access (`d.inHours` is Kotlin property syntax;
        //     the Java visitor walks `J.FieldAccess`, a different LST node)
        //   - wrapped-in-not-null (`!!` is a Kotlin-only operator)
        //   - chained calls (the chain encoding's `\t`-separated spec is only
        //     parsed by the Kotlin helper; the Java helper splits on `\n` only)
        //
        // The Java symbol may legitimately be null when authors compile
        // against a rewrite-kotlin without [GeneratedRecipeSupport.methodInvocationRewriteJava]
        // (i.e. older snapshots) — fall back to the Kotlin helper in that case
        // so the recipe still compiles, even though it won't match Java
        // sources without TreeVisitorAdapter glue.
        val classifierResult = templates.usesKotlinTreeNode
        val isChain = templates.matcherSpecs.any { it.contains('\t') }
        val canRouteToJava = !classifierResult && !isChain
        return when {
            templates.propertyAccess -> ctx.propertyAccessRewriteKotlinSymbol
            templates.wrappedInNotNull -> ctx.methodInvocationRewriteKotlinNotNullSymbol
            canRouteToJava -> ctx.methodInvocationRewriteJavaSymbol
                ?: ctx.methodInvocationRewriteKotlinSymbol
            else -> ctx.methodInvocationRewriteKotlinSymbol
        }
    }

    /**
     * Tries to extract the recipe body as a `RewriteTemplates`. v1 accepts the
     * exact canonical shape:
     *
     *     recipe(...) {
     *         edit { rewrite { p -> p.foo(...) } to { p -> p.bar(...) } }
     *     }
     *
     * The edit block must contain exactly one `rewrite { ... } to { ... }`
     * clause as its sole top-level statement. Multi-before
     * (`rewrite(b1, b2) to a`) is accepted iff all before lambdas canonicalise
     * to the same argument signature so they share one after template.
     */
    private fun extractRewriteTemplates(
        recipeCall: IrCall,
        sourceText: String,
        ctx: RecipeIrGenContext,
    ): RewriteTemplates? {
        val recipeBlock = findTrailingLambda(recipeCall) ?: return null
        val recipeStmts = (recipeBlock.function.body as? IrBlockBody)?.statements ?: return null

        // v1 canonical shape: a single `edit { ... }` call.
        val firstStmt = recipeStmts.singleOrNull() ?: return null
        val editCall = (firstStmt as? IrReturn)?.value as? IrCall
            ?: firstStmt as? IrCall
            ?: return null
        if (editCall.symbol.owner.kotlinFqName.asString() != RECIPE_BUILDER_EDIT_FQN) return null
        if (editCall.valueArgumentsCount != 1) return null
        val editBlockArg = editCall.getValueArgument(0) as? IrFunctionExpression ?: return null
        val editStmts = (editBlockArg.function.body as? IrBlockBody)?.statements ?: return null

        // The edit block's sole statement should be the `rewrite { } to { }`
        // call, optionally wrapped in a trailing `.strictArity()` opt-out.
        // Since `to` returns `RewriteRule` (non-Unit), a bare clause is wrapped
        // by K2 in an IMPLICIT_COERCION_TO_UNIT when the edit lambda returns
        // Unit — peel that so we see the underlying call.
        val rewriteStmt = editStmts.singleOrNull() ?: return null
        val rawStmt = (rewriteStmt as? IrReturn)?.value ?: rewriteStmt
        val unwrappedStmt = if (rawStmt is IrTypeOperatorCall &&
            rawStmt.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        ) {
            rawStmt.argument
        } else {
            rawStmt
        }
        val outermostCall = unwrappedStmt as? IrCall ?: return null
        val strict = outermostCall.symbol.owner.kotlinFqName.asString() == REWRITE_RULE_STRICT_ARITY_FQN
        val toCall = if (strict) {
            outermostCall.dispatchReceiver as? IrCall ?: return null
        } else {
            outermostCall
        }
        val toCallFqn = toCall.symbol.owner.kotlinFqName.asString()
        if (toCallFqn !in REWRITE_ADVICE_TO_FQNS) return null
        val rewriteCall = toCall.dispatchReceiver as? IrCall ?: return null
        if (rewriteCall.symbol.owner.kotlinFqName.asString() != EDIT_SCOPE_REWRITE_FQN) return null

        // Two accepted call shapes on `rewrite`:
        //   1) rewrite(before)              — valueArgumentsCount == 1.
        //   2) rewrite(first, vararg rest)  — valueArgumentsCount == 2 with
        //                                     arg 1 an `IrVararg` of lambdas.
        val beforeExprs: List<IrFunctionExpression> = when (rewriteCall.valueArgumentsCount) {
            1 -> {
                val only = rewriteCall.getValueArgument(0) as? IrFunctionExpression ?: return null
                listOf(only)
            }
            2 -> {
                val first = rewriteCall.getValueArgument(0) as? IrFunctionExpression ?: return null
                val rest = rewriteCall.getValueArgument(1) as? org.jetbrains.kotlin.ir.expressions.IrVararg ?: return null
                val restLambdas = rest.elements.map { it as? IrFunctionExpression ?: return null }
                listOf(first) + restLambdas
            }
            else -> return null
        }
        val afterArg = toCall.getValueArgument(0) as? IrFunctionExpression ?: return null

        val beforeLambdas = beforeExprs.map { validateBeforeLambda(it, sourceText, ctx) ?: return null }
        val notNullForAll = beforeLambdas[0].wrappedInNotNull
        val propertyAccessForAll = beforeLambdas[0].propertyAccess
        for (i in 1 until beforeLambdas.size) {
            // Same param count is still required: the after lambda has a
            // fixed param list and each before must supply the same set of
            // bindings. Canonical signatures, however, may differ — they
            // just produce per-before substitution-source CSVs (see below).
            if (beforeLambdas[i].params.size != beforeLambdas[0].params.size) return null
            // Multi-before recipes must agree on the not-null-assertion shape.
            // Mixing `someCall()` and `someCall()!!` befores would require two
            // different visitor entry points; reject and let the runtime DSL
            // builder surface the limitation.
            if (beforeLambdas[i].wrappedInNotNull != notNullForAll) return null
            // Same restriction for property-access vs method-invocation shape:
            // the two routes use different LST visitors, so a multi-before
            // recipe must pick one or the other.
            if (beforeLambdas[i].propertyAccess != propertyAccessForAll) return null
        }
        // Variadic groups only flow through the plain method-invocation helpers;
        // not-null / chain shapes use visitor entries without run support.
        if (beforeLambdas.any { it.varargGroup != null && (it.wrappedInNotNull || it.inner != null) }) return null
        // `.strictArity()` is incompatible with the spread form (a spread has no
        // fixed count to pin). Compute the exact arg count for the runtime guard
        // — only meaningful for varargs callees; -1 disables it.
        if (strict && beforeLambdas.any { it.varargGroup?.isSpread == true }) return null
        val strictArgCount = if (strict) {
            val counts = beforeLambdas.filter { it.varargGroup != null }.map { it.argSignatures.size }.toSet()
            when {
                counts.isEmpty() -> -1
                counts.size == 1 -> counts.single()
                else -> return null
            }
        } else {
            -1
        }
        // Mixed-shape multi-before: each before gets its own
        // substitution-source CSV (since the after-lambda params bind to
        // different positions in each before — receiver in one, arg-N in
        // another). The after TEMPLATE is required to be identical across
        // all befores; if it isn't, the after lambda's IR was somehow
        // different per-before, which the runtime helper can't dispatch on.
        // Whether the generated visitor will use JavaTemplate (vs KotlinTemplate)
        // — mirrors `pickHelperSymbol`. Drives placeholder type spelling: Java
        // templates need `java.lang.String`, Kotlin templates `kotlin.String`.
        val isChain = beforeLambdas.any { it.inner != null }
        val usesKotlinTreeNode = classifyKotlinPromotion(beforeExprs + afterArg)
        val javaTemplate = !usesKotlinTreeNode && !notNullForAll && !propertyAccessForAll &&
            !isChain && ctx.methodInvocationRewriteJavaSymbol != null

        val firstTemplate = buildAfterTemplate(afterArg, sourceText, beforeLambdas[0], strict, javaTemplate) ?: return null
        val csvs = mutableListOf(firstTemplate.second)
        for (i in 1 until beforeLambdas.size) {
            val nextTemplate = buildAfterTemplate(afterArg, sourceText, beforeLambdas[i], strict, javaTemplate) ?: return null
            if (nextTemplate.first != firstTemplate.first) return null
            csvs += nextTemplate.second
        }
        // Reject multi-before chains in v1 (mixing chain shapes with single-call
        // would require per-before matcher dispatch in the runtime helper; the
        // 5 starter chain recipes are all single-before).
        if (beforeLambdas.any { it.inner != null } && beforeLambdas.size > 1) return null
        val matcherSpecs = beforeLambdas.map { bl ->
            bl.inlinedConstantMatcherSpec ?: run {
                val outerSpec = computeMatcherSpec(bl.rootCall!!, bl.propertyAccess)
                val innerSpec = bl.inner?.let { computeMatcherSpec(it.rootCall!!, propertyAccess = false) }
                // Chain encoding: <outerSpec>\t<innerSpec>. The tab separator
                // distinguishes a single chained spec from the \n-separated
                // multi-before shape. Runtime helpers detect the tab and
                // switch to chain-matching mode.
                if (innerSpec != null) "$outerSpec\t$innerSpec" else outerSpec
            }
        }
        // Single-before recipes pass the CSV through as a plain string for
        // backward compatibility with the original helper signature. Multi-
        // before recipes pack per-matcher CSVs `\n`-delimited; helpers detect
        // the delimiter and dispatch per-matcher.
        val csvField = if (csvs.size == 1) csvs[0] else csvs.joinToString("\n")

        return RewriteTemplates(
            matcherSpecs = matcherSpecs,
            afterTemplate = firstTemplate.first,
            substitutionSourcesCsv = csvField,
            usesKotlinTreeNode = usesKotlinTreeNode,
            wrappedInNotNull = notNullForAll,
            propertyAccess = propertyAccessForAll,
            strictArgCount = strictArgCount,
        )
    }

    private fun findTrailingLambda(call: IrCall): IrFunctionExpression? {
        val callee = call.symbol.owner
        val blockIdx = callee.valueParameters.indexOfFirst { it.name == Name.identifier("block") }
        if (blockIdx < 0) return null
        return call.arguments[blockIdx] as? IrFunctionExpression
    }

    /**
     * LST-structural classifier. Scans the before + after lambdas; returns
     * true if ANY of:
     *  - a value-parameter type references a Kotlin-specific tree node
     *    (FQN starts with `org.openrewrite.kotlin.tree.K.`)
     *  - a call expression resolves to a callee in the `kotlin.*` package
     *    namespace (Kotlin stdlib function or extension — `String.lowercase`,
     *    `StringBuilder.appendLine`, `kotlin.enumValues`, etc.)
     *
     * The call-FQN check exists because v2 dispatch routes the non-Kotlin
     * case through `JavaTemplate`, which can't parse Kotlin-extension call
     * syntax in the after-template. If a Kotlin-stdlib callee appears
     * anywhere in the lambdas, the recipe stays on the Kotlin helper.
     *
     * Per plan §Design.4: only LST-structural and callee-namespace signals
     * drive promotion. The MethodMatcher spec built at FIR time pre-inline
     * still works against either visitor's matched node.
     */
    private fun classifyKotlinPromotion(lambdas: List<IrFunctionExpression>): Boolean {
        var found = false
        val visitor = object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (found) return
                element.acceptChildrenVoid(this)
            }

            override fun visitFunctionExpression(expression: IrFunctionExpression) {
                if (found) return
                // Inspect each value parameter's type for K.* references.
                for (p in expression.function.valueParameters) {
                    if (typeIsKotlinSpecific(p.type)) {
                        found = true
                        return
                    }
                }
                expression.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                if (found) return
                // Callee FQN in the `kotlin.*` namespace promotes to Kotlin:
                // JavaTemplate can't parse Kotlin-extension call syntax in
                // the after-template (`s.lowercase()`, `sb.appendLine("x")`,
                // `enumValues<E>()` all resolve to `kotlin.text.lowercase`,
                // `kotlin.text.StringsKt.appendLine`, `kotlin.enumValues`).
                val fqn = expression.symbol.owner.kotlinFqName.asString()
                if (fqn == "kotlin" || fqn.startsWith("kotlin.")) {
                    found = true
                    return
                }
                expression.acceptChildrenVoid(this)
            }
        }
        for (lambda in lambdas) lambda.acceptChildrenVoid(visitor)
        return found
    }

    private fun typeIsKotlinSpecific(type: IrType): Boolean {
        val fqn = type.classFqName?.asString() ?: return false
        return RecipeIrLanguageDescriptors.isKotlinSpecificTreeNode(fqn)
    }

    // ------------------------------------------------------------------
    // Before-lambda validation + matcher spec.
    // ------------------------------------------------------------------

    private sealed class ArgSig {
        class ParamRef(val symbol: IrValueSymbol) : ArgSig()
        class LiteralConst(val kind: org.jetbrains.kotlin.ir.expressions.IrConstKind, val value: Any?) : ArgSig()

        /**
         * A Kotlin spread (`*args`) of an array-typed param into a callee's
         * varargs slot. Only valid as the trailing flattened sig of a varargs
         * callee. Carries the array param symbol — the whole run is captured as
         * one group (see [VarargGroup]).
         */
        class VarargSpread(val symbol: IrValueSymbol) : ArgSig()
    }

    /**
     * The variadic capture of a before-lambda whose root call targets a method
     * whose declared last parameter is varargs (`Object...`). Built from the
     * args the author placed into that slot — either plain "by example" element
     * refs (`asList(a, b, c)`) or a single spread (`asList(*args)`).
     *
     * [startArgIndex] (`k`) is the flattened position where the varargs run
     * begins — i.e. the number of fixed declared prefix params. At a matched
     * call site the group absorbs `getArguments()[k..end]`. [memberSymbols] are
     * the param symbols bound to the slot, in source order (one entry for the
     * spread form; N for the by-example form). The after-template references
     * them as a single contiguous run that expands to the matched args.
     */
    private class VarargGroup(
        val startArgIndex: Int,
        val memberSymbols: List<IrValueSymbol>,
        val isSpread: Boolean,
    )

    private class BeforeLambda(
        val params: List<IrValueParameter>,
        /**
         * Null when the lambda body is an inlined static constant
         * (e.g. `{ -> Math.PI }`). K2 FIR2IR compile-time-folds primitive
         * `const val` accesses to a bare `IrConst`, dropping the qualifier
         * symbol entirely; there's no `IrCall` to anchor a method-matcher
         * spec on. The matcher spec is recovered by parsing the recipe
         * source slice instead — see [inlinedConstantMatcherSpec].
         */
        val rootCall: IrCall?,
        val argSignatures: List<ArgSig>,
        val receiverParamSymbol: IrValueSymbol?,
        /**
         * True when the lambda body returns `someCall()!!` rather than bare
         * `someCall()`. K2 FIR2IR represents `expr!!` as an `IrCall` to the
         * `kotlin.internal.ir.CHECK_NOT_NULL` intrinsic; the matcher targets
         * the inner call but the helper selection routes to a visitor that
         * walks `K.Unary(NotNull)` so the rewrite replaces the entire
         * not-null-asserted expression rather than just the inner invocation.
         */
        val wrappedInNotNull: Boolean,
        /**
         * True when the rootCall is a property getter invoked at the source
         * level via property-access syntax (`d.inHours`) rather than a method
         * call (`d.inHours()`), OR when the body is an inlined static
         * constant. Both shapes are matched as `J.FieldAccess` by the
         * `propertyAccessRewrite` helper — same matcher-spec format
         * (`<owner-fqn>#<name>`), same visitor entry point.
         */
        val propertyAccess: Boolean,
        /**
         * For inlined static-constant befores (e.g. `{ -> Math.PI }`): the
         * matcher spec computed by parsing the recipe source slice. Null
         * otherwise — the spec is computed downstream from [rootCall] via
         * [computeMatcherSpec].
         */
        val inlinedConstantMatcherSpec: String? = null,
        /**
         * Set when the before lambda is a two-segment chained call
         * (e.g. `{ xs, p -> xs.filter(p).first() }`). [rootCall] holds the
         * OUTER call (`first()`); [inner] describes the inner call (`filter(p)`)
         * with its own arg signatures and receiver-param binding. v1 supports
         * depth-2 chains only; deeper chains return null from
         * [validateBeforeLambda].
         *
         * Chain BeforeLambdas may NOT also be wrappedInNotNull or propertyAccess
         * — those routes use different visitor entries.
         */
        val inner: BeforeLambda? = null,
        /**
         * Set when the root call targets a varargs method and the author placed
         * ≥1 arg into the varargs slot. Drives variadic-by-default matching: the
         * matcher becomes `prefix…, ..` and the after-template carries the group
         * as one expandable run. Null for non-varargs callees (or chain/not-null/
         * property shapes, which keep the fixed-arity path). See [VarargGroup].
         */
        val varargGroup: VarargGroup? = null,
    )

    private fun validateBeforeLambda(
        fnExpr: IrFunctionExpression,
        sourceText: String,
        ctx: RecipeIrGenContext,
    ): BeforeLambda? {
        val fn = fnExpr.function
        val params = fn.valueParameters
        val body = fn.body as? IrBlockBody ?: return null
        val singleStmt = body.statements.singleOrNull() ?: return null
        val rawExpr = when (singleStmt) {
            is IrReturn -> singleStmt.value
            is IrCall -> singleStmt
            is IrConst -> singleStmt
            else -> null
        } ?: return null
        // Inlined-constant before pattern: `{ -> Math.PI }`. K2 FIR2IR
        // compile-time-folds primitive `const val` reads to a bare IrConst,
        // erasing both the `Math` qualifier and the `<get-PI>` accessor
        // symbol. Fall back to parsing the recipe source slice to recover
        // the `Class.NAME` shape; route to the property-access helper
        // (matches `J.FieldAccess` in user code by `<owner-fqn>#<name>`).
        if (rawExpr is IrConst && params.isEmpty()) {
            return validateInlinedConstantBeforeLambda(rawExpr, params, sourceText, ctx)
        }
        var wrappedInNotNull = false
        var current: IrCall = rawExpr as? IrCall ?: return null
        while (current.symbol.owner.kotlinFqName.asString() == CHECK_NOT_NULL_FQN) {
            wrappedInNotNull = true
            val inner = current.getValueArgument(0) as? IrCall ?: return null
            current = inner
        }
        val rootCall = current

        // Source-level property access vs method invocation: K2 lowers
        // `d.inHours` (property) and `d.inHours()` (would-be method) both to
        // `IrCall(<get-inHours>)` with identical IR offsets covering the LHS
        // plus the accessor name only — `()` (if present) is NOT inside the
        // IrCall's offsets. We therefore disambiguate by looking at the
        // character immediately following the IrCall's endOffset in source.
        // If it's `(`, the author wrote method-call syntax. Otherwise
        // (newline, whitespace, EOF, operator, `}`), it's property access.
        val propertyAccess = run {
            if (rootCall.symbol.owner.correspondingPropertySymbol == null) return@run false
            val eo = rootCall.endOffset
            if (eo < 0 || eo > sourceText.length) return@run false
            // Skip horizontal whitespace; a literal '(' on the same expression
            // line means the author wrote a method-style call.
            var i = eo
            while (i < sourceText.length) {
                val c = sourceText[i]
                if (c == ' ' || c == '\t') { i++; continue }
                return@run c != '('
            }
            true
        }

        if (params.isEmpty()) {
            if (rootCall.dispatchReceiver != null || rootCall.extensionReceiver != null) return null
            if (rootCall.valueArgumentsCount > 0) return null
            return BeforeLambda(params, rootCall, emptyList(), receiverParamSymbol = null,
                wrappedInNotNull = wrappedInNotNull, propertyAccess = propertyAccess)
        }

        // K2 wraps Java-platform-typed call results (e.g. `Optional.of(x)`
        // returning `Optional<T>!`) in an `IrTypeOperatorCall(IMPLICIT_CAST)`.
        // Peel those off when looking at the receiver — for the chain
        // validator, the wrapped IrCall IS the inner segment we care about,
        // and the cast is downstream-visible only through the same IR offsets.
        val rawReceiver: IrExpression? = unwrapImplicitCasts(rootCall.dispatchReceiver ?: rootCall.extensionReceiver)
        val paramSyms: Set<IrValueSymbol> = params.map { it.symbol }.toSet()

        // Chain detection (v1: depth-2 only). When the root call's receiver is
        // itself an IrCall, treat the root as the OUTER segment and recurse one
        // level to extract the inner segment. The inner segment binds lambda
        // params via its receiver and value args; the outer segment in v1 must
        // bind no lambda params (the outer's args may be literal constants but
        // not param refs — keeps the substitution-source encoding simple).
        if (rawReceiver is IrCall && !wrappedInNotNull && !propertyAccess) {
            val outerSigs = mutableListOf<ArgSig>()
            for (i in 0 until rootCall.valueArgumentsCount) {
                val arg = unwrapImplicitCasts(rootCall.getValueArgument(i)) ?: continue
                when (arg) {
                    is IrConst -> outerSigs += ArgSig.LiteralConst(arg.kind, arg.value)
                    else -> return null  // outer args binding lambda params not yet supported
                }
            }
            val innerCall = rawReceiver
            val innerRawRecv: IrExpression? = unwrapImplicitCasts(innerCall.dispatchReceiver ?: innerCall.extensionReceiver)
            // Reject depth-3+ chains: inner.receiver must not be another IrCall.
            val innerReceiverSym: IrValueSymbol? = when (innerRawRecv) {
                null -> null
                is IrGetValue -> {
                    if (innerRawRecv.symbol !in paramSyms) return null
                    innerRawRecv.symbol
                }
                else -> return null
            }
            val innerSigs = mutableListOf<ArgSig>()
            for (i in 0 until innerCall.valueArgumentsCount) {
                val arg = unwrapImplicitCasts(innerCall.getValueArgument(i)) ?: continue
                when (arg) {
                    is IrGetValue -> {
                        if (arg.symbol !in paramSyms) return null
                        innerSigs += ArgSig.ParamRef(arg.symbol)
                    }
                    is IrConst -> innerSigs += ArgSig.LiteralConst(arg.kind, arg.value)
                    else -> return null
                }
            }
            val inner = BeforeLambda(
                params = params,
                rootCall = innerCall,
                argSignatures = innerSigs,
                receiverParamSymbol = innerReceiverSym,
                wrappedInNotNull = false,
                propertyAccess = false,
            )
            return BeforeLambda(
                params = params,
                rootCall = rootCall,
                argSignatures = outerSigs,
                receiverParamSymbol = null,
                wrappedInNotNull = false,
                propertyAccess = false,
                inner = inner,
            )
        }

        val receiverParamSymbol: IrValueSymbol? = when (rawReceiver) {
            null -> null
            is IrGetValue -> {
                if (rawReceiver.symbol !in paramSyms) return null
                rawReceiver.symbol
            }
            else -> return null
        }

        val isVarargsCallee = rootCall.symbol.owner.valueParameters.lastOrNull()?.varargElementType != null
        val sigs = mutableListOf<ArgSig>()
        var varargGroup: VarargGroup? = null
        for (i in 0 until rootCall.valueArgumentsCount) {
            val rawArg = rootCall.getValueArgument(i) ?: continue
            // A call to a varargs method lowers the trailing args into a single
            // IrVararg at the varargs param's slot. Flatten its elements: plain
            // refs are "by example" run members; a lone `*args` is a spread
            // capturing the whole run. Either way they form a VarargGroup that
            // drives variadic-by-default matching downstream.
            if (rawArg is IrVararg) {
                val k = sigs.size
                val members = mutableListOf<IrValueSymbol>()
                var sawSpread = false
                for (element: IrVarargElement in rawArg.elements) {
                    if (element is IrSpreadElement) {
                        // `*args` — one lone spread of an array param. v1 rejects
                        // mixing a spread with other elements in the same slot.
                        if (rawArg.elements.size != 1) return null
                        val spreadExpr = unwrapImplicitCasts(element.expression)
                        if (spreadExpr !is IrGetValue || spreadExpr.symbol !in paramSyms) return null
                        sigs += ArgSig.VarargSpread(spreadExpr.symbol)
                        members += spreadExpr.symbol
                        sawSpread = true
                    } else {
                        when (val elemExpr = unwrapImplicitCasts(element as? IrExpression)) {
                            is IrGetValue -> {
                                if (elemExpr.symbol !in paramSyms) return null
                                sigs += ArgSig.ParamRef(elemExpr.symbol)
                                members += elemExpr.symbol
                            }
                            // A literal in the varargs slot can't anchor a
                            // pass-through run — reject for v1.
                            else -> return null
                        }
                    }
                }
                // Only a genuinely varargs callee forms a group; an empty
                // by-example slot has nothing to carry.
                if (isVarargsCallee && members.isNotEmpty()) {
                    varargGroup = VarargGroup(startArgIndex = k, memberSymbols = members, isSpread = sawSpread)
                }
                continue
            }
            val arg = unwrapImplicitCasts(rawArg) ?: continue
            when (arg) {
                is IrGetValue -> {
                    if (arg.symbol !in paramSyms) return null
                    sigs += ArgSig.ParamRef(arg.symbol)
                }
                is IrConst -> sigs += ArgSig.LiteralConst(arg.kind, arg.value)
                else -> return null
            }
        }
        // Property accessors take 0 value args; a non-empty arg signature here
        // means the IR is method-style despite a getter-shaped symbol.
        val finalPropertyAccess = propertyAccess && sigs.isEmpty()
        return BeforeLambda(params, rootCall, sigs, receiverParamSymbol, wrappedInNotNull, finalPropertyAccess,
            varargGroup = varargGroup)
    }

    /**
     * Recover a matcher spec for a before-lambda whose body K2 compile-time-
     * folded into an [IrConst]. Primitive Java `public static final` fields
     * (`Math.PI`, `Math.E`, `Integer.MAX_VALUE`) and Kotlin `const val`
     * companions get inlined at FIR2IR, leaving no symbol to anchor the spec
     * on — we re-derive the `Class.NAME` shape by parsing the lambda source.
     *
     * Returns null when the slice doesn't have a `Qualifier.NAME` shape or
     * when the qualifier can't be resolved to a class on the compiler
     * classpath. Imports are erased by the time IR runs, so resolution
     * probes a fixed candidate list (the slice as-FQN, `java.lang.X`,
     * `kotlin.X`) — covers the common JVM/Kotlin primitive-const cases.
     */
    private fun validateInlinedConstantBeforeLambda(
        constExpr: IrConst,
        params: List<IrValueParameter>,
        sourceText: String,
        ctx: RecipeIrGenContext,
    ): BeforeLambda? {
        val so = constExpr.startOffset
        val eo = constExpr.endOffset
        if (so < 0 || eo <= so || eo > sourceText.length) return null
        // The IR-emitted offsets may cover just `PI` (the package qualifier
        // has no IR node); reuse the FQN-extension walk so the parsed slice
        // is the recipe author's full `Foo.BAR` spelling.
        val sliceStart = extendBackwardForQualifierChain(sourceText, so)
        val rawSlice = sourceText.substring(sliceStart, eo).trim()
        val dotIdx = rawSlice.lastIndexOf('.')
        if (dotIdx <= 0 || dotIdx == rawSlice.length - 1) return null
        val qualifier = rawSlice.substring(0, dotIdx)
        val name = rawSlice.substring(dotIdx + 1)
        if (!isValidJavaIdentifier(qualifier.replace(".", "")) || !isValidJavaIdentifier(name)) return null
        val resolvedFqn = resolveQualifierAsClass(ctx.pluginContext, qualifier) ?: return null
        return BeforeLambda(
            params = params,
            rootCall = null,
            argSignatures = emptyList(),
            receiverParamSymbol = null,
            wrappedInNotNull = false,
            propertyAccess = true,
            inlinedConstantMatcherSpec = "$resolvedFqn#$name",
        )
    }

    private fun isValidJavaIdentifier(s: String): Boolean {
        if (s.isEmpty()) return false
        if (!Character.isJavaIdentifierStart(s[0])) return false
        for (i in 1 until s.length) {
            if (!Character.isJavaIdentifierPart(s[i])) return false
        }
        return true
    }

    /**
     * Try to resolve the qualifier slice (everything left of the final dot
     * in a `Qualifier.NAME` constant reference) to a class FQN. Probes:
     *   1. The qualifier as-is (already a FQN, e.g. `java.lang.Math`).
     *   2. `java.lang.<qualifier>` (the auto-imported root in Kotlin source).
     *   3. `kotlin.<qualifier>` (for `Int`, `Long`, `Double` etc. — the
     *      stdlib types that house Kotlin's primitive constants).
     *
     * Returns the first FQN whose class resolves on the compiler classpath,
     * or null. Imports are erased by IR time so we can't query the recipe
     * file's own import list; the candidate set is intentionally narrow to
     * avoid false positives.
     */
    private fun resolveQualifierAsClass(
        pluginContext: IrPluginContext,
        qualifier: String,
    ): String? {
        val candidates = mutableListOf<String>()
        if (qualifier.contains('.')) {
            candidates += qualifier
        }
        candidates += "java.lang.$qualifier"
        candidates += "kotlin.$qualifier"
        for (cand in candidates) {
            val fq = FqName(cand)
            if (fq.isRoot) continue
            if (pluginContext.referenceClass(ClassId.topLevel(fq)) != null) return cand
        }
        return null
    }

    private fun BeforeLambda.canonicalSignature(): CanonicalSignature {
        val paramIdxBySymbol: Map<IrValueSymbol, Int> =
            params.withIndex().associate { (idx, p) -> p.symbol to idx }
        val receiverIdx = receiverParamSymbol?.let { paramIdxBySymbol[it] }
        val args = argSignatures.map { sig ->
            when (sig) {
                is ArgSig.ParamRef -> {
                    val idx = paramIdxBySymbol[sig.symbol] ?: return@map CanonicalArgSig.Unknown
                    CanonicalArgSig.ParamIdx(idx)
                }
                is ArgSig.LiteralConst -> CanonicalArgSig.Literal(sig.kind, sig.value)
                is ArgSig.VarargSpread -> {
                    val idx = paramIdxBySymbol[sig.symbol] ?: return@map CanonicalArgSig.Unknown
                    CanonicalArgSig.Vararg(idx)
                }
            }
        }
        return CanonicalSignature(receiverIdx, args)
    }

    private data class CanonicalSignature(
        val receiverParamIdx: Int?,
        val args: List<CanonicalArgSig>,
    )

    private sealed class CanonicalArgSig {
        data class ParamIdx(val idx: Int) : CanonicalArgSig()
        data class Vararg(val idx: Int) : CanonicalArgSig()
        data class Literal(val kind: org.jetbrains.kotlin.ir.expressions.IrConstKind, val value: Any?) : CanonicalArgSig()
        object Unknown : CanonicalArgSig()
    }

    /**
     * Tightens the MethodMatcher's declaring-type segment based on the
     * resolved before-lambda's root call. Member calls bind to the dispatch
     * receiver's class FQN; extension calls and top-level non-extensions to
     * the JVM facade class. Mirrors [org.openrewrite.kotlin.KotlinTypeMapping].
     *
     * When [propertyAccess] is true, the spec uses the `<owner-fqn>#<property-name>`
     * format consumed by `GeneratedRecipeSupport.propertyAccessRewrite`, which
     * walks `J.FieldAccess` rather than `J.MethodInvocation`. The property
     * name comes from the `IrSimpleFunction`'s `correspondingPropertySymbol`
     * — the function's own `name` is the synthetic `<get-foo>` accessor name,
     * which doesn't match the source-level identifier.
     */
    /**
     * Peel off `IrTypeOperatorCall` wrappers that K2 inserts to mark implicit
     * coercions — most commonly the platform-type-to-Kotlin cast on the return
     * of a Java call (`Optional<T>!` → `Optional<T>`). For the validator's
     * purposes, the cast is invisible: the wrapped IrCall has the same source
     * offsets and is the call we want to introspect.
     *
     * Unwraps both `IMPLICIT_CAST` and `IMPLICIT_NOTNULL` recursively. Other
     * type ops (`SAFE_CAST`, `INSTANCEOF`, `CAST`) are intentionally left
     * alone — they're load-bearing for matcher dispatch.
     */
    private fun unwrapImplicitCasts(expr: IrExpression?): IrExpression? {
        var e: IrExpression? = expr
        while (e is IrTypeOperatorCall &&
            (e.operator == IrTypeOperator.IMPLICIT_CAST || e.operator == IrTypeOperator.IMPLICIT_NOTNULL)) {
            e = e.argument
        }
        return e
    }

    private fun computeMatcherSpec(rootCall: IrCall, propertyAccess: Boolean): String {
        val owner = rootCall.symbol.owner
        if (propertyAccess) {
            val propName = owner.correspondingPropertySymbol?.owner?.name?.asString()
                ?: owner.name.asString().removePrefix("<get-").removeSuffix(">")
            val ownerFqn = owner.dispatchReceiverParameter?.type?.classFqName?.asString()
                ?: (owner.parent as? IrClass)?.kotlinFqName?.asString()
                ?: computeJvmFacadeFqn(owner)
                ?: "*"
            return "$ownerFqn#$propName"
        }
        val methodName = owner.name.asString()
        val argsPattern = computeArgsPattern(owner)

        val dispatchFqn = owner.dispatchReceiverParameter?.type?.classFqName?.asString()
        if (dispatchFqn != null) {
            return "$dispatchFqn $methodName($argsPattern)"
        }

        // Java static methods (and `@JvmStatic` companions): the IrSimpleFunction's
        // parent is the declaring IrClass even when the dispatch-receiver param is
        // null. Pin the matcher to that class so `Math.abs(x)` doesn't accidentally
        // match `kotlin.math.abs(x)` after the rewrite. Without this, the matcher
        // falls through to `* abs(..)` and the recipe keeps firing every cycle,
        // tripping the test framework's single-cycle stability check.
        val parent = owner.parent
        if (parent is IrClass) {
            return "${parent.kotlinFqName.asString()} $methodName($argsPattern)"
        }

        val facadeFqn = computeJvmFacadeFqn(owner)
        return if (facadeFqn != null) {
            "$facadeFqn $methodName($argsPattern)"
        } else {
            "* $methodName($argsPattern)"
        }
    }

    /**
     * Emit a precise MethodMatcher arg pattern (e.g. `*,*` for two args, empty for
     * zero) instead of the `..` wildcard. Tightening the arg count is what
     * distinguishes overloaded methods on the same name: `Iterable<T>.any()` (no
     * predicate) versus `Iterable<T>.any(predicate: (T) -> Boolean)`. A recipe
     * authored as `xs.filter(p).any()` would otherwise also match
     * `xs.filter(p1).any { p2 }` via `(..)` and silently drop the `p2` predicate.
     *
     * Arg count is computed against the JVM-resolved signature, which differs by
     * call shape:
     * <ul>
     *   <li>Member call (dispatch receiver, e.g. {@code String.lowercase()}): the
     *       receiver is dispatched, so the JVM arg list is just the source-level
     *       value args.</li>
     *   <li>Extension or top-level facade call (e.g. {@code Iterable<T>.any(p)}):
     *       the receiver is lifted to the first arg of the static facade method,
     *       so the JVM arg list is the (extension) receiver plus value args.</li>
     * </ul>
     */
    private fun computeArgsPattern(owner: IrSimpleFunction): String {
        val isDispatched = owner.dispatchReceiverParameter != null
        val extReceiverArg = if (!isDispatched && owner.extensionReceiverParameter != null) 1 else 0
        val params = owner.valueParameters
        if (params.lastOrNull()?.varargElementType != null) {
            // Varargs callee: a `JavaType.Method` declares the varargs slot as a
            // single array parameter, so only a trailing `..` can ever match it
            // (a fixed `*,*,*` spec never will — `MethodMatcherTest
            // .varargsMatchesArrayType`). Type the fixed prefix params so we
            // don't also match a same-named sibling overload such as
            // `String.format(Locale, String, Object...)`.
            val tokens = mutableListOf<String>()
            repeat(extReceiverArg) { tokens += "*" }
            for (i in 0 until params.size - 1) {
                tokens += matcherParamType(params[i].type)
            }
            tokens += ".."
            return tokens.joinToString(",")
        }
        val jvmArgCount = params.size + extReceiverArg
        if (jvmArgCount == 0) return ""
        return List(jvmArgCount) { "*" }.joinToString(",")
    }

    /**
     * Render a fixed prefix parameter's type as a MethodMatcher type token,
     * mapping Kotlin builtins back to the Java FQN the matcher resolves against.
     * Falls back to `*` when the type can't be named.
     */
    private fun matcherParamType(type: IrType): String {
        val fqn = type.classFqName?.asString() ?: return "*"
        return KOTLIN_BUILTIN_TO_JAVA_FQN[fqn] ?: fqn
    }

    private fun computeJvmFacadeFqn(fn: IrSimpleFunction): String? {
        val containerSource = (fn as? IrMemberWithContainerSource)?.containerSource
        if (containerSource is JvmPackagePartSource) {
            val jvmName = containerSource.facadeClassName ?: containerSource.className
            return jvmName.fqNameForTopLevelClassMaybeWithDollars.asString()
        }
        val file = fn.parent as? IrFile ?: return null
        val pkgFqn = file.packageFqName.asString()
        val jvmNameOverride = file.annotations
            .firstOrNull { it.type.classFqName?.asString() == "kotlin.jvm.JvmName" }
            ?.let { ann -> (ann.arguments.firstOrNull() as? IrConst)?.value as? String }
        val baseName = jvmNameOverride ?: defaultFacadeBaseName(file.fileEntry.name) ?: return null
        return if (pkgFqn.isEmpty()) baseName else "$pkgFqn.$baseName"
    }

    private fun defaultFacadeBaseName(filePath: String): String? {
        val nameOnly = filePath.substringAfterLast('/').substringAfterLast('\\')
        if (!nameOnly.endsWith(".kt")) return null
        val stem = nameOnly.removeSuffix(".kt")
        if (stem.isEmpty()) return null
        return stem.replaceFirstChar { it.uppercaseChar() } + "Kt"
    }

    /**
     * Walk backward from [start] through any `identifier(.identifier)*` chain
     * in [sourceText] and return the new (earlier) start offset. Used to grow
     * the IR-derived source slice over a syntactic FQN qualifier preceding a
     * top-level call — K2 IR doesn't emit a child node for the qualifier so
     * the IR walk's `minStart` lands at the simple call name. Stops at any
     * non-identifier-or-dot char (whitespace, paren, operator, brace), so a
     * non-qualified call slice is unchanged.
     */
    private fun extendBackwardForQualifierChain(sourceText: String, start: Int): Int {
        var i = start
        while (i > 0) {
            val dotIdx = i - 1
            if (sourceText[dotIdx] != '.') break
            // Walk back through identifier chars preceding the '.'.
            var j = dotIdx - 1
            while (j >= 0 && (sourceText[j].isLetterOrDigit() || sourceText[j] == '_')) j--
            val identStart = j + 1
            // Identifier must be at least one char and start with a non-digit.
            if (identStart >= dotIdx) break
            val first = sourceText[identStart]
            if (!(first.isLetter() || first == '_')) break
            i = identStart
        }
        return i
    }

    // ------------------------------------------------------------------
    // After-template synthesis.
    // ------------------------------------------------------------------

    private fun buildAfterTemplate(
        fnExpr: IrFunctionExpression,
        sourceText: String,
        before: BeforeLambda,
        strict: Boolean,
        javaTemplate: Boolean,
    ): Pair<String, String>? {
        val fn = fnExpr.function
        val afterParams = fn.valueParameters
        if (afterParams.size != before.params.size) return null
        val body = fn.body as? IrBlockBody ?: return null
        val singleStmt = body.statements.singleOrNull() ?: return null
        val expr: IrExpression = when (singleStmt) {
            is IrReturn -> singleStmt.value
            is IrExpression -> singleStmt
            else -> return null
        }

        // Variadic run: when the before targets a varargs method (and the
        // recipe didn't opt into strict arity) the args placed in the varargs
        // slot are carried as ONE expandable run, not fixed captures.
        // `groupAfterSyms` are the after-lambda param symbols (positional twins
        // of the before's group members) that make up that run — they're
        // excluded from normal placeholder mapping and re-emitted as a single
        // sentinel the runtime expands per matched call.
        val group = if (strict) null else before.varargGroup
        val groupBeforeSyms: Set<IrValueSymbol> = group?.memberSymbols?.toSet() ?: emptySet()
        val groupAfterSyms: Set<IrValueSymbol> = if (group == null) emptySet() else buildSet {
            for (i in before.params.indices) {
                if (before.params[i].symbol in groupBeforeSyms) add(afterParams[i].symbol)
            }
        }

        // Substitution source encoding:
        //   * Single-segment recipes use plain integer strings ("-1", "0", "N")
        //     for backward compatibility with the runtime helper's existing
        //     parser. -1 means root-call receiver; N >= 0 means root-call arg N.
        //   * Chain (two-segment) recipes prefix each source with a segment
        //     tag: "o:" for outer, "i:" for inner. v1 outer args are all
        //     literal constants and chain BeforeLambdas always set
        //     receiverParamSymbol = null on the outer, so every param-derived
        //     source for a chain recipe carries the "i:" prefix in practice.
        //     The CSV parser at runtime branches on the presence of ':'.
        val isChain = before.inner != null
        fun outerSrc(pos: Int): String = if (isChain) "o:$pos" else pos.toString()
        fun innerSrc(pos: Int): String = "i:$pos"

        val paramSymToSource = HashMap<IrValueSymbol, String>(afterParams.size)
        for (i in 0 until afterParams.size) {
            val afterSym = afterParams[i].symbol
            if (afterSym in groupAfterSyms) continue  // emitted as the variadic run below
            val beforeParamSym = before.params[i].symbol
            val source: String = when {
                beforeParamSym == before.receiverParamSymbol -> outerSrc(-1)
                else -> {
                    val outerArgIdx = before.argSignatures.indexOfFirst { sig ->
                        sig is ArgSig.ParamRef && sig.symbol == beforeParamSym
                    }
                    if (outerArgIdx >= 0) outerSrc(outerArgIdx)
                    else if (isChain) {
                        val inner = before.inner!!
                        if (beforeParamSym == inner.receiverParamSymbol) innerSrc(-1)
                        else {
                            val innerArgIdx = inner.argSignatures.indexOfFirst { sig ->
                                sig is ArgSig.ParamRef && sig.symbol == beforeParamSym
                            }
                            if (innerArgIdx < 0) return null
                            innerSrc(innerArgIdx)
                        }
                    } else return null
                }
            }
            paramSymToSource[afterSym] = source
        }

        data class LiteralSlot(val src: String, val sig: ArgSig.LiteralConst, var consumed: Boolean = false)
        val literalSlots = mutableListOf<LiteralSlot>()
        before.argSignatures.forEachIndexed { idx, sig ->
            if (sig is ArgSig.LiteralConst) literalSlots += LiteralSlot(outerSrc(idx), sig)
        }
        if (isChain) {
            before.inner!!.argSignatures.forEachIndexed { idx, sig ->
                if (sig is ArgSig.LiteralConst) literalSlots += LiteralSlot(innerSrc(idx), sig)
            }
        }

        // Span computation: walk IR for min(start)/max(end) over valid offsets.
        var minStart = Int.MAX_VALUE
        var maxEnd = Int.MIN_VALUE
        expr.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                val so = element.startOffset
                val eo = element.endOffset
                if (so >= 0 && eo > so) {
                    if (so < minStart) minStart = so
                    if (eo > maxEnd) maxEnd = eo
                }
                element.acceptChildrenVoid(this)
            }
        })
        run {
            val so = expr.startOffset
            val eo = expr.endOffset
            if (so >= 0 && eo > so) {
                if (so < minStart) minStart = so
                if (eo > maxEnd) maxEnd = eo
            }
        }
        if (minStart == Int.MAX_VALUE || maxEnd > sourceText.length) return null
        // Source-text-slice gives the IR-visible span, which excludes any
        // syntactic FQN qualifier preceding a top-level call (K2 IR resolves
        // `kotlin.math.abs(x)` to a single `IrCall(abs)` whose offsets cover
        // only `abs(x)`; the `kotlin.math.` package qualifier has no IR node).
        // Walk backward through any `identifier(.identifier)*` chain so the
        // template includes whatever qualifier the recipe author wrote — the
        // rewrite is then output-identical to the source spelling and remains
        // single-cycle stable (no separate import-add pass needed).
        val sliceStart = extendBackwardForQualifierChain(sourceText, minStart)
        val sliceEnd = maxEnd
        val slice = sourceText.substring(sliceStart, sliceEnd)

        data class Spot(
            val startOffset: Int,
            val endOffset: Int,
            val source: String,
            // The exact text to splice in at this span: a `#{any(T)}` placeholder
            // for a normal capture, or the variadic sentinel for the run.
            val render: String,
        )
        val spots = mutableListOf<Spot>()
        // Group-member references in the after body, folded into one run below.
        data class GroupRef(val startOffset: Int, val endOffset: Int, val symbol: IrValueSymbol)
        val groupRefs = mutableListOf<GroupRef>()

        // `accept(visitor, null)` includes `expr` itself; `acceptChildrenVoid`
        // would only visit its children. The distinction matters for after
        // bodies that are a single bare param reference (`{ x -> x }`) — the
        // IrGetValue IS the expression, not a child, so a children-only walk
        // would miss it and the template would render the literal `x` instead
        // of a substitution placeholder.
        expr.accept(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) { element.acceptChildrenVoid(this) }

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol in groupAfterSyms) {
                    groupRefs += GroupRef(expression.startOffset, expression.endOffset, expression.symbol)
                    return
                }
                val src = paramSymToSource[expression.symbol] ?: return
                spots += Spot(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    source = src,
                    render = renderPlaceholder(renderPlaceholderType(expression.symbol.owner.type, javaTemplate)),
                )
            }

            override fun visitConst(expression: IrConst) {
                val slot = literalSlots.firstOrNull { !it.consumed && it.sig.kind == expression.kind && it.sig.value == expression.value }
                    ?: return
                slot.consumed = true
                spots += Spot(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    source = slot.src,
                    render = renderPlaceholder(renderPlaceholderType(expression.type, javaTemplate)),
                )
            }
        }, null)

        // Fold the group-member references into one run spot. The runtime
        // expands the sentinel to one `#{any()}` per matched arg and splices
        // `getArguments()[k..end]` at this ordinal (source `V<k>`).
        if (group != null) {
            if (groupRefs.isEmpty()) return null                       // after must reference the run
            val seen = groupRefs.map { it.symbol }.toSet()
            if (seen.size != groupRefs.size) return null               // a member used twice
            if (seen != groupAfterSyms) return null                    // missing / extra members
            val runStartRaw = groupRefs.minOf { it.startOffset }
            val runEnd = groupRefs.maxOf { it.endOffset }
            // Spread form (`*args`): swallow the leading `*` (and any horizontal
            // whitespace) so the sentinel replaces the whole spread expression.
            val runStart = if (group.isSpread) {
                var s = runStartRaw
                while (s > 0 && (sourceText[s - 1] == ' ' || sourceText[s - 1] == '\t')) s--
                if (s > 0 && sourceText[s - 1] == '*') s - 1 else runStartRaw
            } else {
                runStartRaw
            }
            // Contiguity: no normal capture may sit inside the run span.
            if (spots.any { it.startOffset < runEnd && it.endOffset > runStart }) return null
            spots += Spot(runStart, runEnd, "V${group.startArgIndex}", VARARGS_SENTINEL)
        }

        val inSliceSpots = mutableListOf<Spot>()
        val prependSpots = mutableListOf<Spot>()
        // "Prepend" handling — a receiver-source spot that the IR placed
        // outside the source slice (the package qualifier on a static call,
        // typically) needs to attach to the front of the rendered template
        // as `#{any()}.<slice>`. For single-segment recipes that means
        // source == "-1"; for chains the prepend slot would be "i:-1".
        val receiverSourceTags = if (isChain) setOf("i:-1", "o:-1") else setOf("-1")
        for (spot in spots) {
            val inSlice = spot.startOffset in sliceStart until sliceEnd &&
                spot.endOffset in (spot.startOffset + 1)..sliceEnd
            if (inSlice) {
                inSliceSpots += spot
            } else if (spot.source in receiverSourceTags) {
                prependSpots += spot
            } else {
                return null
            }
        }
        if (prependSpots.size > 1) return null

        val templateBuilder = StringBuilder(slice)
        for (spot in inSliceSpots.sortedByDescending { it.startOffset }) {
            templateBuilder.replace(
                spot.startOffset - sliceStart,
                spot.endOffset - sliceStart,
                spot.render,
            )
        }
        val template = if (prependSpots.isEmpty()) {
            templateBuilder.toString()
        } else {
            "${prependSpots.single().render}.$templateBuilder"
        }

        val orderedSources = mutableListOf<String>()
        if (prependSpots.isNotEmpty()) orderedSources += prependSpots.single().source
        orderedSources += inSliceSpots.sortedBy { it.startOffset }.map { it.source }
        val csv = orderedSources.joinToString(",")

        return template to csv
    }

    private fun renderPlaceholderType(type: IrType, javaTemplate: Boolean): String? {
        val fqn = type.classFqName?.asString() ?: return null
        // On the JavaTemplate path, map Kotlin builtin reference types to their
        // Java FQN so the placeholder (`#{any(java.lang.String)}`) resolves the
        // templated call's method type. Only remap reference FQNs (those with a
        // dot); leave primitives (`int`) and non-builtins alone. KotlinTemplate
        // keeps the Kotlin spelling, which is what it resolves against.
        if (javaTemplate) {
            val mapped = KOTLIN_BUILTIN_TO_JAVA_FQN[fqn]
            return if (mapped != null && mapped.contains('.')) mapped else fqn
        }
        // KotlinTemplate path: spell out concrete type arguments so overloads that
        // dispatch on a generic argument resolve (e.g. `Iterable<T>.sumOf` on the
        // selector's return type). Anything but an invariant, concrete argument
        // (star projection, use-site variance, type parameter) falls back to raw.
        val args = (type as? IrSimpleType)?.arguments
        if (args.isNullOrEmpty()) return fqn
        val rendered = args.map { renderTypeArgument(it) ?: return fqn }
        return "$fqn<${rendered.joinToString(", ")}>"
    }

    private fun renderTypeArgument(arg: IrTypeArgument): String? {
        if (arg !is IrTypeProjection || arg.variance != Variance.INVARIANT) return null
        return renderPlaceholderType(arg.type, javaTemplate = false)
    }

    private fun renderPlaceholder(typeFqn: String?): String =
        if (typeFqn != null) "#{any($typeFqn)}" else "#{any()}"

    // ------------------------------------------------------------------
    // Generated class synthesis.
    // ------------------------------------------------------------------

    private fun buildGeneratedRecipeClass(
        ctx: RecipeIrGenContext,
        parentFile: IrFile,
        propertyName: Name,
        metadata: RecipeMetadata,
        templates: RewriteTemplates,
        helperSymbol: IrSimpleFunctionSymbol,
    ): IrClass {
        val cls = ctx.pluginContext.irFactory.buildClass {
            name = Name.identifier("${propertyName.asString()}\$KtRecipe")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }
        cls.parent = parentFile
        cls.createThisReceiverParameter()
        cls.superTypes = listOf(ctx.recipeClassSymbol.defaultType)

        cls.addConstructor {
            isPrimary = true
            returnType = cls.symbol.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(ctx.recipeNoArgCtorSymbol.owner)
            }
        }

        addMetadataOverrides(cls, ctx, metadata)
        if (ctx.getVisitor != null) {
            addGetVisitorOverride(
                cls = cls,
                ctx = ctx,
                overrides = ctx.getVisitor,
                helperSymbol = helperSymbol,
                templates = templates,
            )
        }
        return cls
    }

    private fun addMetadataOverrides(cls: IrClass, ctx: RecipeIrGenContext, metadata: RecipeMetadata) {
        addStringOverride(cls, ctx.pluginContext, "getDisplayName", metadata.displayName, ctx.getDisplayName)
        addStringOverride(cls, ctx.pluginContext, "getDescription", metadata.description, ctx.getDescription)
        if (metadata.tagsArg != null && ctx.getTags != null) {
            addReturningOverride(cls, ctx, "getTags", ctx.getTags, metadata.tagsArg)
        }
        if (metadata.estimatedEffortArg != null && ctx.getEstimatedEffort != null) {
            addReturningOverride(cls, ctx, "getEstimatedEffortPerOccurrence", ctx.getEstimatedEffort, metadata.estimatedEffortArg)
        }
    }

    private fun addStringOverride(
        cls: IrClass,
        pluginContext: IrPluginContext,
        jvmName: String,
        value: String,
        overrides: IrSimpleFunction,
    ) {
        cls.addFunction(
            name = jvmName,
            returnType = pluginContext.irBuiltIns.stringType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irReturn(irString(value))
            }
        }
    }

    /**
     * Emits an override that returns a deep-copied IR expression. Shared
     * between getTags and getEstimatedEffortPerOccurrence — both just lift
     * the user's argument expression into a getter body. Deep-copy because
     * the original argument lives inside the soon-to-be-replaced `recipe(...)`
     * call; handing the same node to two owners breaks IR invariants.
     */
    private fun addReturningOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        jvmName: String,
        overrides: IrSimpleFunction,
        valueExpr: IrExpression,
    ) {
        cls.addFunction(
            name = jvmName,
            returnType = overrides.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irReturn(valueExpr.deepCopyWithSymbols(initialParent = this@apply))
            }
        }
    }

    private fun addGetVisitorOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        helperSymbol: IrSimpleFunctionSymbol,
        templates: RewriteTemplates,
    ) {
        cls.addFunction(
            name = "getVisitor",
            returnType = overrides.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                val factoryCall = irCall(
                    callee = helperSymbol,
                    type = helperSymbol.owner.returnType,
                )
                factoryCall.arguments[0] = irString(templates.matcherSpecs.joinToString("\n"))
                factoryCall.arguments[1] = irString(templates.afterTemplate)
                factoryCall.arguments[2] = irString(templates.substitutionSourcesCsv)
                // The method-invocation helpers take a 4th `strictArgCount`;
                // the not-null / property helpers don't.
                if (helperSymbol.owner.valueParameters.size >= 4) {
                    factoryCall.arguments[3] = irInt(templates.strictArgCount)
                }
                +irReturn(factoryCall)
            }
        }
    }

    // ------------------------------------------------------------------
    // Imperative-shape class synthesis.
    // ------------------------------------------------------------------

    /**
     * Synthesize a `<Name>$KtRecipe` class for an imperative recipe
     * (`recipe(...) { edit { lang { visitX { … } } } }`). The class is
     * structurally identical to the declarative one — same metadata
     * overrides, same field-less shape — but its `getVisitor()` body
     * delegates to `buildImperativeVisitor(<recipe trailing lambda>)`
     * instead of a template-driven factory.
     *
     * The recipe trailing lambda is deep-copied so it survives the
     * subsequent `recipe(...) → <Name>$KtRecipe()` replacement (the
     * original call's IR is discarded by the transformer).
     */
    private fun buildImperativeRecipeClass(
        ctx: RecipeIrGenContext,
        parentFile: IrFile,
        propertyName: Name,
        metadata: RecipeMetadata,
        recipeBlock: IrFunctionExpression,
        helperSymbol: IrSimpleFunctionSymbol,
    ): IrClass {
        val cls = ctx.pluginContext.irFactory.buildClass {
            name = Name.identifier("${propertyName.asString()}\$KtRecipe")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }
        cls.parent = parentFile
        cls.createThisReceiverParameter()
        cls.superTypes = listOf(ctx.recipeClassSymbol.defaultType)

        cls.addConstructor {
            isPrimary = true
            returnType = cls.symbol.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(ctx.recipeNoArgCtorSymbol.owner)
            }
        }

        addMetadataOverrides(cls, ctx, metadata)
        if (ctx.getVisitor != null) {
            addImperativeGetVisitorOverride(
                cls = cls,
                ctx = ctx,
                overrides = ctx.getVisitor,
                helperSymbol = helperSymbol,
                recipeBlock = recipeBlock,
            )
        }
        return cls
    }

    private fun addImperativeGetVisitorOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        helperSymbol: IrSimpleFunctionSymbol,
        recipeBlock: IrFunctionExpression,
    ) {
        cls.addFunction(
            name = "getVisitor",
            returnType = overrides.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                val factoryCall = irCall(
                    callee = helperSymbol,
                    type = helperSymbol.owner.returnType,
                )
                factoryCall.arguments[0] = recipeBlock.deepCopyWithSymbols(initialParent = this@apply)
                +irReturn(factoryCall)
            }
        }
    }

    // ------------------------------------------------------------------
    // Composite-shape class synthesis (for `recipes(...)` properties).
    // ------------------------------------------------------------------

    /**
     * Synthesize a `<Name>$KtRecipe` class for a `recipes(displayName, description,
     * vararg recipes: Recipe)` call site. The class extends `Recipe` and
     * overrides:
     *  - `getDisplayName()` returns the captured displayName constant
     *  - `getDescription()` returns the captured description constant
     *  - `getRecipeList()` returns `listOf(<vararg elements>)`
     *
     * Like the recipe(...) path, the synthesized class is field-less so Jackson
     * roundtrip is clean — the child recipe references live in the file's
     * top-level property getters, which the synthesized class invokes fresh on
     * each `getRecipeList()` call.
     */
    private fun buildCompositeRecipeClass(
        ctx: RecipeIrGenContext,
        parentFile: IrFile,
        propertyName: Name,
        metadata: CompositeRecipeMetadata,
    ): IrClass {
        val cls = ctx.pluginContext.irFactory.buildClass {
            name = Name.identifier("${propertyName.asString()}\$KtRecipe")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }
        cls.parent = parentFile
        cls.createThisReceiverParameter()
        cls.superTypes = listOf(ctx.recipeClassSymbol.defaultType)

        cls.addConstructor {
            isPrimary = true
            returnType = cls.symbol.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(ctx.recipeNoArgCtorSymbol.owner)
            }
        }

        addStringOverride(cls, ctx.pluginContext, "getDisplayName", metadata.displayName, ctx.getDisplayName)
        addStringOverride(cls, ctx.pluginContext, "getDescription", metadata.description, ctx.getDescription)
        addGetRecipeListOverride(cls, ctx, ctx.getRecipeList!!, ctx.listOfVarargSymbol!!, metadata.recipesVararg)
        return cls
    }

    private fun addGetRecipeListOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        listOfVarargSymbol: IrSimpleFunctionSymbol,
        recipesVararg: org.jetbrains.kotlin.ir.expressions.IrVararg,
    ) {
        cls.addFunction(
            name = "getRecipeList",
            returnType = overrides.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                val listOfCall = irCall(
                    callee = listOfVarargSymbol,
                    type = overrides.returnType,
                )
                // listOf<T>(vararg elements: T): T is the single type parameter.
                listOfCall.typeArguments[0] = ctx.recipeClassSymbol.defaultType
                // Deep-copy because the original IrVararg lives inside the
                // soon-to-be-replaced `recipes(...)` call; sharing the node
                // with two owners breaks IR invariants.
                listOfCall.arguments[0] = recipesVararg.deepCopyWithSymbols(initialParent = this@apply)
                +irReturn(listOfCall)
            }
        }
    }
}
