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
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.io.File
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.time.Duration as JDuration
import java.time.format.DateTimeParseException

/**
 * IR-phase code generator for the recipe authoring DSL.
 *
 * For every top-level property whose initializer is a call to
 * `org.openrewrite.recipe(displayName, description, …) { … }`, this extension:
 *
 *  1. Reads the metadata arguments (`displayName`, `description`, `tags`,
 *     `estimatedEffortPerOccurrence`).
 *  2. Emits a synthetic top-level class `Generated$<PropertyName>` that
 *     extends `org.openrewrite.Recipe` and overrides the metadata accessors.
 *  3. For pattern-mode recipes whose body is the v0-supported shape
 *     `rewrite { p0: T, p1: U, ... -> p0.foo(...) } to { p0, p1, ... -> ... }`,
 *     emits a `getVisitor()` override that returns
 *     `GeneratedRecipeSupport.methodInvocationRewrite(matcherSpec, afterTemplate, substitutionSourcesCsv)`.
 *     Wider lambda shapes leave the framework's default no-op visitor in
 *     place (so the recipe compiles but doesn't transform code).
 *  4. Rewrites the original `recipe(...)` call expression in the property
 *     initializer to a constructor invocation of the generated class.
 *
 * Without this pass the call to `org.openrewrite.recipe` falls through to
 * its runtime stub in `RecipeDsl.kt`, which throws to flag the missing
 * plugin. Once this pass replaces the call, the val initializer becomes a
 * direct constructor invocation and the runtime stub is never reached.
 */
internal class RecipeIrGenerationExtension : IrGenerationExtension {

    private companion object {
        val RECIPE_FQN: FqName = FqName("org.openrewrite.recipe")

        /**
         * Per-scope FQN prefixes used to recognise `visitX { ... }` calls
         * inside scan/edit blocks. The visit-method's simple name is recovered
         * by stripping the prefix and matched against [VISIT_NAMES].
         */
        const val SCAN_SCOPE_PREFIX = "org.openrewrite.ScanScope."
        const val EDIT_SCOPE_PREFIX = "org.openrewrite.EditScope."
        const val EDIT_SCOPE_WITH_ACC_PREFIX = "org.openrewrite.EditScopeWithAcc."

        /**
         * Each entry maps a DSL visit method name to the two runtime helpers
         * (edit phase and scan phase) in [GeneratedRecipeSupport] that wrap a
         * user lambda into a TreeVisitor for that node kind. Adding a new
         * primitive is one row here + four declarations (DSL stubs on the
         * three scopes + helper functions in Java) and the IR pass picks it
         * up automatically.
         */
        val VISIT_PRIMITIVES: List<VisitPrimitive> = listOf(
            VisitPrimitive("visitMethodInvocation", "methodInvocationEditVisitor", "methodInvocationScanVisitor"),
            VisitPrimitive("visitClassDeclaration", "classDeclarationEditVisitor", "classDeclarationScanVisitor"),
            VisitPrimitive("visitMethodDeclaration", "methodDeclarationEditVisitor", "methodDeclarationScanVisitor"),
            VisitPrimitive("visitVariableDeclarations", "variableDeclarationsEditVisitor", "variableDeclarationsScanVisitor"),
            VisitPrimitive("visitImport", "importEditVisitor", "importScanVisitor"),
            VisitPrimitive("visitProperty", "propertyEditVisitor", "propertyScanVisitor"),
        )

        val VISIT_NAMES: Set<String> = VISIT_PRIMITIVES.map { it.visitMethodName }.toSet()
    }

    private data class VisitPrimitive(
        val visitMethodName: String,
        val editHelperName: String,
        val scanHelperName: String,
    )

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val recipeClassId = ClassId.topLevel(FqName("org.openrewrite.Recipe"))
        val recipeClassSymbol = pluginContext.referenceClass(recipeClassId) ?: return

        val recipeNoArgCtor = pluginContext.referenceConstructors(recipeClassId)
            .singleOrNull { it.owner.valueParameters.isEmpty() } ?: return

        val recipeMembers = recipeClassSymbol.owner.declarations.filterIsInstance<IrSimpleFunction>()
        val recipeGetDisplayName = recipeMembers.firstOrNull {
            it.name.asString() == "getDisplayName" && it.valueParameters.isEmpty()
        } ?: return
        val recipeGetDescription = recipeMembers.firstOrNull {
            it.name.asString() == "getDescription" && it.valueParameters.isEmpty()
        } ?: return
        // getTags / getEstimatedEffortPerOccurrence aren't strictly required: if a
        // future Recipe revision renames or removes either, we just stop generating
        // that override rather than failing the whole pass.
        val recipeGetTags = recipeMembers.firstOrNull {
            it.name.asString() == "getTags" && it.valueParameters.isEmpty()
        }
        val recipeGetEstimatedEffort = recipeMembers.firstOrNull {
            it.name.asString() == "getEstimatedEffortPerOccurrence" && it.valueParameters.isEmpty()
        }
        val recipeGetVisitor = recipeMembers.firstOrNull {
            it.name.asString() == "getVisitor" && it.valueParameters.isEmpty()
        }

        // `GeneratedRecipeSupport.methodInvocationRewrite(matcherSpec, afterTemplate, substitutionSourcesCsv)`
        // is the lowering target for `rewrite { p -> p.foo(...) } to { p -> p.bar(...) }`.
        // If we can't find it (e.g. when running against an older rewrite-kotlin),
        // we silently skip getVisitor() generation — recipes still compile, they
        // just don't transform code.
        val supportClassId = ClassId.topLevel(FqName("org.openrewrite.kotlin.recipe.GeneratedRecipeSupport"))
        val supportClassSymbol = pluginContext.referenceClass(supportClassId)
        val methodInvocationRewriteSymbol: IrSimpleFunctionSymbol? = supportClassSymbol
            ?.owner?.declarations
            ?.filterIsInstance<IrSimpleFunction>()
            ?.firstOrNull { fn ->
                fn.name.asString() == "methodInvocationRewrite" &&
                    fn.dispatchReceiverParameter == null &&
                    fn.valueParameters.size == 3
            }
            ?.symbol

        // For each visit primitive (visitMethodInvocation, visitClassDeclaration,
        // ...), look up the runtime helper symbols in GeneratedRecipeSupport for
        // both phases. Maps are keyed by visit-method-name (e.g.
        // "visitMethodInvocation") so the IR pass can route a recognised
        // `visit*` call straight to its helper. Same fall-through contract as
        // before: missing symbols just leave the recipe with a no-op visitor.
        val supportFns = supportClassSymbol
            ?.owner?.declarations
            ?.filterIsInstance<IrSimpleFunction>()
            ?.filter { it.dispatchReceiverParameter == null && it.valueParameters.size == 1 }
            .orEmpty()
        val editVisitorHelpers: Map<String, IrSimpleFunctionSymbol> = VISIT_PRIMITIVES
            .mapNotNull { p ->
                supportFns.firstOrNull { it.name.asString() == p.editHelperName }
                    ?.symbol?.let { p.visitMethodName to it }
            }.toMap()
        val scanVisitorHelpers: Map<String, IrSimpleFunctionSymbol> = VISIT_PRIMITIVES
            .mapNotNull { p ->
                supportFns.firstOrNull { it.name.asString() == p.scanHelperName }
                    ?.symbol?.let { p.visitMethodName to it }
            }.toMap()

        // `ScanningRecipe<T>` is the superclass when the recipe body uses
        // scan/edit phase mode. As with Recipe we need: the class symbol (for
        // the parameterized supertype), its no-arg constructor (for super-
        // delegation), and the three abstract / overridable hooks.
        val scanningRecipeClassId = ClassId.topLevel(FqName("org.openrewrite.ScanningRecipe"))
        val scanningRecipeClassSymbol: IrClassSymbol? = pluginContext.referenceClass(scanningRecipeClassId)
        val scanningRecipeNoArgCtor: IrConstructorSymbol? = scanningRecipeClassSymbol?.let {
            pluginContext.referenceConstructors(scanningRecipeClassId)
                .singleOrNull { it.owner.valueParameters.isEmpty() }
        }
        val scanningRecipeMembers = scanningRecipeClassSymbol?.owner?.declarations
            ?.filterIsInstance<IrSimpleFunction>().orEmpty()
        val getInitialValueFn = scanningRecipeMembers.firstOrNull {
            it.name.asString() == "getInitialValue" && it.valueParameters.size == 1
        }
        val getScannerFn = scanningRecipeMembers.firstOrNull {
            it.name.asString() == "getScanner" && it.valueParameters.size == 1
        }
        // `getVisitor(T)` is the one-arg overload distinct from `Recipe.getVisitor()`.
        // `ScanningRecipe` also `final`s the no-arg getVisitor; we override the T-arg
        // form.
        val getVisitorAccFn = scanningRecipeMembers.firstOrNull {
            it.name.asString() == "getVisitor" && it.valueParameters.size == 1
        }

        // `ExecutionContext` for the getInitialValue(ctx) parameter type.
        val executionContextClassId = ClassId.topLevel(FqName("org.openrewrite.ExecutionContext"))
        val executionContextClassSymbol: IrClassSymbol? = pluginContext.referenceClass(executionContextClassId)

        // ScanScope<A>.acc / EditScopeWithAcc<A>.acc getter symbols — references
        // to either inside a hoisted lambda body must be rewritten to read the
        // local visitor method's `acc` parameter instead of the (nonexistent at
        // runtime) outer receiver-scope object.
        val scanScopeAccGetterFqn = "org.openrewrite.ScanScope.acc.<get-acc>"
        val editScopeWithAccAccGetterFqn = "org.openrewrite.EditScopeWithAcc.acc.<get-acc>"
        val scanScopeClassId = ClassId.topLevel(FqName("org.openrewrite.ScanScope"))
        val editScopeWithAccClassId = ClassId.topLevel(FqName("org.openrewrite.EditScopeWithAcc"))
        val scanScopeAccGetterSymbol: IrSimpleFunctionSymbol? = pluginContext
            .referenceClass(scanScopeClassId)?.owner?.declarations
            ?.filterIsInstance<IrProperty>()
            ?.firstOrNull { it.name.asString() == "acc" }
            ?.getter?.symbol
        val editScopeWithAccGetterSymbol: IrSimpleFunctionSymbol? = pluginContext
            .referenceClass(editScopeWithAccClassId)?.owner?.declarations
            ?.filterIsInstance<IrProperty>()
            ?.firstOrNull { it.name.asString() == "acc" }
            ?.getter?.symbol

        // `Duration.parse(CharSequence)` for materialising the
        // estimatedEffortPerOccurrence ISO-8601 string into a runtime Duration.
        val durationClassId = ClassId.topLevel(FqName("java.time.Duration"))
        val durationClassSymbol: IrClassSymbol? = pluginContext.referenceClass(durationClassId)
        val durationParseSymbol: IrSimpleFunctionSymbol? = durationClassSymbol
            ?.owner?.declarations
            ?.filterIsInstance<IrSimpleFunction>()
            ?.firstOrNull { fn ->
                fn.name.asString() == "parse" &&
                    fn.dispatchReceiverParameter == null &&
                    fn.valueParameters.size == 1
            }
            ?.symbol

        val recipeContext = RecipeIrGenContext(
            pluginContext = pluginContext,
            recipeClassSymbol = recipeClassSymbol,
            recipeNoArgCtorSymbol = recipeNoArgCtor,
            getDisplayName = recipeGetDisplayName,
            getDescription = recipeGetDescription,
            getTags = recipeGetTags,
            getEstimatedEffort = recipeGetEstimatedEffort,
            getVisitor = recipeGetVisitor,
            methodInvocationRewriteSymbol = methodInvocationRewriteSymbol,
            editVisitorHelpers = editVisitorHelpers,
            scanVisitorHelpers = scanVisitorHelpers,
            scanningRecipeClassSymbol = scanningRecipeClassSymbol,
            scanningRecipeNoArgCtorSymbol = scanningRecipeNoArgCtor,
            getInitialValue = getInitialValueFn,
            getScanner = getScannerFn,
            getVisitorAcc = getVisitorAccFn,
            executionContextClassSymbol = executionContextClassSymbol,
            scanScopeAccGetterSymbol = scanScopeAccGetterSymbol,
            editScopeWithAccGetterSymbol = editScopeWithAccGetterSymbol,
            durationClassSymbol = durationClassSymbol,
            durationParseSymbol = durationParseSymbol,
        )

        for (file in moduleFragment.files) {
            processFile(file, recipeContext)
        }
    }

    /**
     * Bundles the IR symbols / function references we look up once at the start
     * of [generate] so we don't re-resolve them per file or per property.
     */
    private class RecipeIrGenContext(
        val pluginContext: IrPluginContext,
        val recipeClassSymbol: IrClassSymbol,
        val recipeNoArgCtorSymbol: IrConstructorSymbol,
        val getDisplayName: IrSimpleFunction,
        val getDescription: IrSimpleFunction,
        val getTags: IrSimpleFunction?,
        val getEstimatedEffort: IrSimpleFunction?,
        val getVisitor: IrSimpleFunction?,
        val methodInvocationRewriteSymbol: IrSimpleFunctionSymbol?,
        /**
         * Visit-method-name (e.g. "visitMethodInvocation") → runtime helper
         * symbol in `GeneratedRecipeSupport` for the edit phase. Used by both
         * `EditScope` (stateless) and `EditScopeWithAcc` (acc-threaded), which
         * share the same per-kind helper because the lambda's signature is
         * identical and acc capture happens via Kotlin closure lowering, not
         * via the helper.
         */
        val editVisitorHelpers: Map<String, IrSimpleFunctionSymbol>,
        /** Same idea, for the scan phase (lambda returns Unit). */
        val scanVisitorHelpers: Map<String, IrSimpleFunctionSymbol>,
        val scanningRecipeClassSymbol: IrClassSymbol?,
        val scanningRecipeNoArgCtorSymbol: IrConstructorSymbol?,
        val getInitialValue: IrSimpleFunction?,
        val getScanner: IrSimpleFunction?,
        val getVisitorAcc: IrSimpleFunction?,
        val executionContextClassSymbol: IrClassSymbol?,
        val scanScopeAccGetterSymbol: IrSimpleFunctionSymbol?,
        val editScopeWithAccGetterSymbol: IrSimpleFunctionSymbol?,
        val durationClassSymbol: IrClassSymbol?,
        val durationParseSymbol: IrSimpleFunctionSymbol?,
    )

    private fun processFile(file: IrFile, ctx: RecipeIrGenContext) {
        // First pass: identify recipe properties, emit synthetic classes, and
        // record the call→constructor-call replacement map. We don't mutate
        // the initializer in place because the transform below needs the
        // original IrCall identity to match against.
        val replacements: MutableMap<IrCall, IrConstructorCall> = LinkedHashMap()

        // Source recovery for lambda → KotlinTemplate string substitution
        // (see [[lane-e-design-2026-05-14]] Probe 1). PSI is stripped by IR
        // phase; the file path on the IrFileEntry is the reliable handle.
        val sourceText: String? = run {
            val path = file.fileEntry.name
            val onDisk = File(path)
            if (onDisk.isFile) onDisk.readText() else null
        }

        // Iterate over a snapshot — `addChild` mutates `file.declarations` while
        // we add synthetic Recipe subclasses, which would otherwise trip a
        // ConcurrentModificationException on the underlying list iterator.
        for (declaration in file.declarations.toList()) {
            if (declaration !is IrProperty) continue
            val initializerExpr = declaration.backingField?.initializer?.expression as? IrCall
                ?: continue
            // `IrUtilsKt.hasTopLevelEqualFqName` returns false when the callee's parent
            // is an `IrExternalPackageFragmentImpl` (functions resolved out of a
            // dependency jar — exactly our case, since `recipe` lives in rewrite-kotlin
            // and consumers import it). Compare the full FqName directly instead.
            if (initializerExpr.symbol.owner.kotlinFqName != RECIPE_FQN) continue

            val metadata = readMetadata(initializerExpr) ?: continue
            // Two lowering shapes today, mutually exclusive by FIR-checker
            // construction (mode-mixing is rejected at compile time): pattern
            // mode (`rewrite ... to ...`) becomes string-args to a Java helper;
            // phase mode (`edit { visitMethodInvocation { ... } }` for the
            // stateless v0 slice) becomes a lambda passed straight through.
            // Try pattern mode first because it has the stricter shape match;
            // fall through to phase mode if no pattern clause is recognised.
            val patternTemplates = if (sourceText != null) {
                extractRewriteTemplates(initializerExpr, sourceText)
            } else null
            val visitorLowering: VisitorLowering? = patternTemplates?.let(VisitorLowering::PatternMode)
                ?: extractScanEditPhase(initializerExpr)
                ?: extractStatelessEditLambda(initializerExpr)

            val generatedClass = buildGeneratedRecipeClass(
                ctx = ctx,
                parentFile = file,
                propertyName = declaration.name,
                metadata = metadata,
                visitorLowering = visitorLowering,
            )
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

    /**
     * Holds the metadata extracted from a `recipe(...)` call. `displayName` and
     * `description` are required; the rest are nullable when the user took the
     * default (empty set for tags, empty string for effort) — we then skip the
     * corresponding override and inherit Recipe's default behaviour.
     */
    private class RecipeMetadata(
        val displayName: String,
        val description: String,
        /** The user's `tags` IR expression, only set when it's not the default empty set. */
        val tagsArg: IrExpression?,
        /** The ISO-8601 duration literal, only set when it's a non-empty constant. */
        val estimatedEffortLiteral: String?,
    )

    /**
     * Extracts the constant `displayName` and `description` arguments plus the
     * optional `tags` IR expression and `estimatedEffortPerOccurrence` literal
     * from a `recipe(...)` call. Returns null if displayName/description are
     * missing or non-constant — the IR pass simply leaves such calls alone, and
     * the runtime stub error informs the author when it fires.
     */
    private fun readMetadata(call: IrCall): RecipeMetadata? {
        val callee = call.symbol.owner
        val params = callee.valueParameters
        val displayNameIdx = params.indexOfFirst { it.name == Name.identifier("displayName") }
        val descriptionIdx = params.indexOfFirst { it.name == Name.identifier("description") }
        if (displayNameIdx < 0 || descriptionIdx < 0) return null
        val displayName = (call.arguments[displayNameIdx] as? IrConst)?.value as? String ?: return null
        val description = (call.arguments[descriptionIdx] as? IrConst)?.value as? String ?: return null

        val tagsIdx = params.indexOfFirst { it.name == Name.identifier("tags") }
        val tagsArg = if (tagsIdx >= 0) substantiveArgOrNull(call.arguments[tagsIdx]) else null

        val effortIdx = params.indexOfFirst { it.name == Name.identifier("estimatedEffortPerOccurrence") }
        val effortLiteral = if (effortIdx >= 0) {
            ((call.arguments[effortIdx] as? IrConst)?.value as? String)?.takeIf { it.isNotEmpty() }
        } else null

        return RecipeMetadata(displayName, description, tagsArg, effortLiteral)
    }

    /**
     * Returns the argument expression iff it's something the author specified —
     * not the default `emptySet()` or `setOf()` fallback. We deep-copy whatever
     * this returns into the generated override; null means "skip override and
     * inherit the framework default".
     */
    private fun substantiveArgOrNull(arg: IrExpression?): IrExpression? {
        if (arg == null) return null
        if (arg is IrCall) {
            val fqn = arg.symbol.owner.kotlinFqName.asString()
            if (fqn == "kotlin.collections.emptySet") return null
            if (fqn == "kotlin.collections.setOf") {
                // setOf() with no varargs (or an empty vararg) is functionally the default.
                val singleArg = arg.arguments.singleOrNull() ?: return arg
                if (singleArg is org.jetbrains.kotlin.ir.expressions.IrVararg && singleArg.elements.isEmpty()) return null
            }
        }
        return arg
    }

    /**
     * The recipe body's intended visitor lowering. Mutually exclusive by the
     * FIR checker — at most one of these survives extraction.
     */
    private sealed class VisitorLowering {
        /** `rewrite { p -> p.foo() } to { p -> p.bar() }` — string-template path. */
        class PatternMode(val templates: RewriteTemplates) : VisitorLowering()

        /**
         * `edit { [aux*]; visitMethodInvocation { call -> ... } }` with no
         * scan. The whole `edit { }` block lambda is carried as a single
         * unit so symbol cross-references (e.g., an aux `val` referenced by
         * the visit lambda) survive deep-copy: the IR pass deep-copies the
         * block once at emit time and then partitions the copy. The
         * partitioned aux statements get hoisted into the generated
         * `getVisitor()` body ahead of the runtime helper call, so the visit
         * lambda closes over them.
         */
        class PhaseStatelessEdit(val editBlock: IrFunctionExpression) : VisitorLowering()

        /**
         * `scan<A>(initial) { [aux*]; visitMethodInvocation { ... } }` paired
         * with an optional `edit(scanRef) { [aux*]; visitMethodInvocation
         * { ... } }`. The generated class extends `ScanningRecipe<A>`. Each
         * block is carried as a single function expression (see
         * [PhaseStatelessEdit] for why); deep-copied and partitioned at emit
         * time. `acc` references inside the hoisted statements (aux + visit
         * lambda) are rewritten from `ScanScope<A>.acc` / `EditScopeWithAcc
         * <A>.acc` getter calls into direct `IrGetValue`s of the respective
         * override-method's `acc` parameter so the runtime closure captures
         * the right value.
         */
        class PhaseScanEdit(
            val accType: IrType,
            val initialExpr: IrExpression,
            val scanBlock: IrFunctionExpression,
            val editBlock: IrFunctionExpression?,
        ) : VisitorLowering()
    }

    private fun buildGeneratedRecipeClass(
        ctx: RecipeIrGenContext,
        parentFile: IrFile,
        propertyName: Name,
        metadata: RecipeMetadata,
        visitorLowering: VisitorLowering?,
    ): IrClass {
        // Two superclass shapes: pattern mode and stateless-edit extend
        // `Recipe`; scan + acc-threaded edit extends `ScanningRecipe<A>` so the
        // framework runs the scan phase before the visit phase and threads the
        // accumulator through. Everything else (metadata overrides, the class
        // shell itself) is identical, so branch only at the superclass /
        // delegating-constructor / visitor-override emission steps.
        val cls = ctx.pluginContext.irFactory.buildClass {
            name = Name.identifier("Generated\$${propertyName.asString()}")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }
        cls.parent = parentFile
        cls.createThisReceiverParameter()

        val phaseScanEdit = visitorLowering as? VisitorLowering.PhaseScanEdit
        val (superType, superCtor) = if (phaseScanEdit != null &&
            ctx.scanningRecipeClassSymbol != null &&
            ctx.scanningRecipeNoArgCtorSymbol != null
        ) {
            // `ScanningRecipe<A>` — parameterised on the accumulator type the
            // user wrote on the `scan<A>(initial)` call.
            ctx.scanningRecipeClassSymbol.typeWith(phaseScanEdit.accType) to ctx.scanningRecipeNoArgCtorSymbol
        } else {
            // Plain `Recipe`. Includes the "phaseScanEdit but ScanningRecipe
            // symbols missing" fallback — we still emit a Recipe shell so the
            // val resolves, just without scan wiring.
            ctx.recipeClassSymbol.defaultType to ctx.recipeNoArgCtorSymbol
        }
        cls.superTypes = listOf(superType)

        cls.addConstructor {
            isPrimary = true
            // IrClass.defaultType lives in `org.jetbrains.kotlin.ir.util`; we avoid
            // adding a second import that collides on simple name with the symbol-side
            // extension by going through the class symbol.
            returnType = cls.symbol.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(superCtor.owner)
            }
        }

        addMetadataOverrides(cls, ctx, metadata)

        when (visitorLowering) {
            is VisitorLowering.PatternMode -> {
                if (ctx.getVisitor != null && ctx.methodInvocationRewriteSymbol != null) {
                    addGetVisitorOverride(
                        cls = cls,
                        ctx = ctx,
                        overrides = ctx.getVisitor,
                        factorySymbol = ctx.methodInvocationRewriteSymbol,
                        templates = visitorLowering.templates,
                    )
                }
            }
            is VisitorLowering.PhaseStatelessEdit -> {
                if (ctx.getVisitor != null) {
                    addPhaseEditGetVisitorOverride(
                        cls = cls,
                        ctx = ctx,
                        overrides = ctx.getVisitor,
                        editBlock = visitorLowering.editBlock,
                    )
                }
            }
            is VisitorLowering.PhaseScanEdit -> {
                addPhaseScanEditOverrides(cls, ctx, visitorLowering)
            }
            null -> Unit
        }
        return cls
    }

    private fun addMetadataOverrides(cls: IrClass, ctx: RecipeIrGenContext, metadata: RecipeMetadata) {
        addStringOverride(cls, ctx.pluginContext, "getDisplayName", metadata.displayName, ctx.getDisplayName)
        addStringOverride(cls, ctx.pluginContext, "getDescription", metadata.description, ctx.getDescription)
        if (metadata.tagsArg != null && ctx.getTags != null) {
            addTagsOverride(cls, ctx, ctx.getTags, metadata.tagsArg)
        }
        if (metadata.estimatedEffortLiteral != null &&
            ctx.getEstimatedEffort != null &&
            ctx.durationParseSymbol != null
        ) {
            // Defense in depth. RecipeDslPropertyChecker rejects invalid ISO-8601
            // literals at FIR time so this branch only stays reachable if the
            // IR pass were ever run without the checker (both register from the
            // same plugin entry point — not a configuration users hit today).
            // Skipping the override on bad input keeps generated bytecode honest.
            if (parsesAsIsoDuration(metadata.estimatedEffortLiteral)) {
                addEstimatedEffortOverride(cls, ctx, ctx.getEstimatedEffort, metadata.estimatedEffortLiteral)
            }
        }
    }

    /**
     * Renders an [IrType] as the Kotlin FQN string used inside a
     * `#{any(...)}` placeholder (e.g., `kotlin.String`, `java.lang.StringBuilder`).
     * Returns null when the type can't be reduced to a class FQN — function
     * types, intersection types, generic parameters etc. — in which case the
     * placeholder emits as untyped `#{any()}` instead of breaking the
     * template parse.
     *
     * Nullability is not encoded here: Kotlin's nullable `String?` shares its
     * `classFqName` with `String`, and KotlinTemplate placeholders accept the
     * non-null spelling for either form.
     */
    private fun renderPlaceholderType(type: IrType): String? =
        type.classFqName?.asString()

    /** `#{any(fqn)}` when [typeFqn] is non-null, otherwise `#{any()}`. */
    private fun renderPlaceholder(typeFqn: String?): String =
        if (typeFqn != null) "#{any($typeFqn)}" else "#{any()}"

    /**
     * Builds a [MethodMatcher][org.openrewrite.java.MethodMatcher] spec from a
     * before-lambda's root call. The declaring-type segment is tightened
     * from the previous `* method(..)` wildcard to the actual declaring
     * type's Kotlin FQN — but **only** for member calls. Kotlin extension
     * functions lower to static methods on a synthetic JVM facade class
     * (e.g. `kotlin.text.StringsKt` for `kotlin.text` package extensions);
     * the J.MethodInvocation's `declaringType` reflects that facade, NOT
     * the source-level receiver type. Matching against the receiver type
     * directly silently misses every extension call, so we keep the
     * wildcard for those until proper facade resolution lands.
     *
     *  - Member calls (`dispatchReceiverParameter != null`,
     *    `extensionReceiverParameter == null`): tighten to
     *    `<receiverFqn> method(..)`.
     *  - Extension calls (`extensionReceiverParameter != null`): keep
     *    `* method(..)` — JVM facade FQN computation needs a compiler
     *    helper (`JvmFileClassUtil` or equivalent) that respects
     *    `@file:JvmName` overrides; that's a separate runway item.
     *  - Top-level non-extension functions (no receiver of either kind):
     *    keep `* method(..)`.
     *
     * If the receiver type can't be reduced to a class FQN (function types,
     * intersection types, etc.), fall back to wildcard so the matcher still
     * fires rather than silently dropping the recipe.
     */
    private fun computeMatcherSpec(rootCall: IrCall): String {
        val owner = rootCall.symbol.owner
        val methodName = owner.name.asString()
        // Extensions go on the JVM facade — receiver type doesn't match LST.
        if (owner.extensionReceiverParameter != null) {
            return "* $methodName(..)"
        }
        val receiverFqn = owner.dispatchReceiverParameter?.type?.classFqName?.asString()
        return if (receiverFqn != null) {
            "$receiverFqn $methodName(..)"
        } else {
            "* $methodName(..)"
        }
    }

    private fun parsesAsIsoDuration(literal: String): Boolean = try {
        JDuration.parse(literal); true
    } catch (_: DateTimeParseException) {
        false
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

    private fun addTagsOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        tagsArg: IrExpression,
    ) {
        val returnType: IrType = overrides.returnType
        cls.addFunction(
            name = "getTags",
            returnType = returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                // The original arg lives inside the soon-to-be-replaced recipe()
                // call; deep-copy so the IR node has a fresh identity owned by
                // the new function. Without this we'd hand the same node to two
                // owners and break IR tree invariants.
                +irReturn(tagsArg.deepCopyWithSymbols(initialParent = this@apply))
            }
        }
    }

    /**
     * Lowering output for a pattern-mode recipe whose before lambda body is a
     * method call rooted at the receiver param. The runtime helper builds one
     * {@link org.openrewrite.java.MethodMatcher} per entry in [matcherSpecs]
     * and accepts a method invocation when any of them matches; it then applies
     * [afterTemplate] (a KotlinTemplate string with one `#{any()}` per slot)
     * with substitutions sourced as described by [substitutionSourcesCsv]:
     * left-to-right by placeholder position, each entry is `-1` for the
     * matched method's receiver (`method.getSelect()`) or `N >= 0` for the
     * Nth argument (`method.getArguments().get(N)`).
     *
     * For multi-before recipes (`rewrite(b1, b2, ...) to a`), every before
     * lambda lowers to its own spec but they share a single after template +
     * CSV — the IR pass refuses to lower unless every before lambda agrees on
     * the captured argument shape, so the same source positions apply.
     *
     * Why MethodMatcher rather than KotlinTemplate-based matching: KotlinTemplate's
     * `matches("#{any()}.method()", cursor)` does not currently bind a receiver
     * placeholder against a concrete invocation (verified by probe).
     */
    private class RewriteTemplates(
        val matcherSpecs: List<String>,
        val afterTemplate: String,
        val substitutionSourcesCsv: String,
    )

    /**
     * A single root-call arg position is either:
     *  - a reference to a lambda value parameter (a "named capture"), or
     *  - an IR const literal (matched wildly against the runtime call, but
     *    re-emitted in the after template where the same `(kind, value)`
     *    appears in source order).
     * Any other kind of arg (compound expression, named-fn ref, etc.) means
     * "out of v0 scope" — we fall back to no-op visitor by returning null.
     */
    private sealed class ArgSig {
        class ParamRef(val symbol: IrValueSymbol) : ArgSig()
        class LiteralConst(val kind: org.jetbrains.kotlin.ir.expressions.IrConstKind, val value: Any?) : ArgSig()
    }

    /**
     * Validated before lambda: the lambda's value parameters, the root method
     * call, a per-arg-position signature for the root call's args used to set
     * up the after template's substitutions, and the lambda param symbol (if
     * any) that appears in the root call's receiver position.
     *
     * `receiverParamSymbol` is:
     *  - some `params[i].symbol` when the root call has an IrGetValue
     *    dispatch/extension receiver bound to that lambda param,
     *  - null when the root call has no receiver at all (top-level function
     *    invocation, e.g. `println(s)`).
     *
     * External receivers (e.g. `Foo.bar(s)` where the receiver is not a
     * lambda param) are rejected at validation time.
     */
    private class BeforeLambda(
        val params: List<IrValueParameter>,
        val rootCall: IrCall,
        val argSignatures: List<ArgSig>,
        val receiverParamSymbol: IrValueSymbol?,
    )

    /**
     * Tries to lower the recipe body to a `(matcherSpecs, afterTemplate, substitutionSourcesCsv)`
     * triple for the v0 shape this commit supports: exactly one
     * `rewrite(before, ...) to after` clause in the body. Each before lambda
     * must have param[0] in receiver position of a method call and remaining
     * root-call args must be param refs or literal constants. Multi-before
     * (`rewrite(b1, b2, ...) to a`) is accepted only when every before lambda
     * has the same canonical argument signature so they can share a single
     * after template + substitution CSV. Returns null on any deviation —
     * the generated recipe then still compiles but inherits the Recipe
     * default no-op visitor.
     */
    private fun extractRewriteTemplates(
        recipeCall: IrCall,
        sourceText: String,
    ): RewriteTemplates? {
        val callee = recipeCall.symbol.owner
        val blockIdx = callee.valueParameters.indexOfFirst { it.name == Name.identifier("block") }
        if (blockIdx < 0) return null
        val blockArg = recipeCall.arguments[blockIdx] as? IrFunctionExpression ?: return null
        val statements = (blockArg.function.body as? IrBlockBody)?.statements ?: return null
        // Single-rewrite v0: exactly one top-level statement. `to` returns Unit,
        // so the wrapper appears as a bare expression statement (or wrapped in
        // an IrReturn when the lambda lowers a single-expression body).
        val firstStmt = statements.singleOrNull() ?: return null
        val toCall = (firstStmt as? IrReturn)?.value as? IrCall ?: firstStmt as? IrCall ?: return null
        // The `rewrite(before: (P) -> R)` overload returns RewriteAdvice1; the
        // `rewrite(before: (P1, P2) -> R)` overload returns RewriteAdvice2.
        // Both expose `to(after)` whose owning class names are RewriteAdviceN.
        val toCallFqn = toCall.symbol.owner.kotlinFqName.asString()
        if (toCallFqn != "org.openrewrite.RewriteAdvice1.to" &&
            toCallFqn != "org.openrewrite.RewriteAdvice2.to"
        ) return null
        val rewriteCall = toCall.dispatchReceiver as? IrCall ?: return null
        if (rewriteCall.symbol.owner.kotlinFqName.asString() != "org.openrewrite.RecipeBuilder.rewrite") return null

        // Two accepted shapes:
        //  1) `rewrite(before)`               — valueArgumentsCount == 1.
        //  2) `rewrite(first, vararg rest)`   — valueArgumentsCount == 2 with
        //                                       arg 1 an `IrVararg` of lambdas.
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

        val beforeLambdas = beforeExprs.map { validateBeforeLambda(it) ?: return null }
        // Multi-before requires a shared after template; enforce that every
        // before lambda canonicalises to the same argument signature. We use
        // the first lambda as the source-truth for the after template, and
        // reject on shape mismatch so we never silently emit a wrong CSV.
        val canonical = beforeLambdas[0].canonicalSignature()
        for (i in 1 until beforeLambdas.size) {
            if (beforeLambdas[i].canonicalSignature() != canonical) return null
            // Same number of lambda params is implied by the DSL surface (all
            // overloads of `rewrite` share a single `(P) -> R` arity), but
            // belt-and-braces verify in case the call resolves unexpectedly.
            if (beforeLambdas[i].params.size != beforeLambdas[0].params.size) return null
        }
        val afterTemplateAndSources = buildAfterTemplate(afterArg, sourceText, beforeLambdas[0]) ?: return null
        val matcherSpecs = beforeLambdas.map { computeMatcherSpec(it.rootCall) }
        return RewriteTemplates(
            matcherSpecs = matcherSpecs,
            afterTemplate = afterTemplateAndSources.first,
            substitutionSourcesCsv = afterTemplateAndSources.second,
        )
    }

    /**
     * Canonicalised form of a before lambda's shape, used to verify
     * multi-before lambdas all capture in the same way so they can share the
     * after template + substitution CSV. Two signatures are equal iff
     *  - the receiver position resolves the same way: same lambda-param index
     *    or `null` (no receiver) on both sides, and
     *  - each arg position is either the same param index (0 = first param,
     *    1 = second param, ...) or the same literal `(kind, value)` pair.
     */
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
        data class Literal(val kind: org.jetbrains.kotlin.ir.expressions.IrConstKind, val value: Any?) : CanonicalArgSig()
        object Unknown : CanonicalArgSig()
    }

    /**
     * Validates a before lambda whose body is a single method call rooted on
     * one of the lambda params OR on no receiver (top-level function call).
     * Each root-call arg must be either an IrGetValue of one of the lambda
     * params (named capture) or an IrConst (literal capture). Returns null on
     * any deviation.
     *
     * Accepted shapes:
     *  - `{ p0: T -> p0.foo(...) }`    — param in receiver position.
     *  - `{ p0: T, p1: U -> p1.foo(p0, ...) }` — any lambda param as receiver.
     *  - `{ p0: T -> foo(p0) }`        — top-level function call, no receiver.
     *
     * Rejected: root calls whose receiver is some external expression (e.g.
     * `Foo.bar(s)` where Foo is a class reference), since the matcher would
     * need to know the receiver type at recipe-generation time.
     */
    private fun validateBeforeLambda(fnExpr: IrFunctionExpression): BeforeLambda? {
        val fn = fnExpr.function
        val params = fn.valueParameters
        if (params.isEmpty()) return null
        val body = fn.body as? IrBlockBody ?: return null
        val singleStmt = body.statements.singleOrNull() ?: return null
        val rootCall = when (singleStmt) {
            is IrReturn -> singleStmt.value as? IrCall
            is IrCall -> singleStmt
            else -> null
        } ?: return null

        // Receiver resolution. The dispatch/extension receivers are mutually
        // exclusive on a single call in K2 IR (extension-receiver IrGetValue
        // has zero-width offsets outside the IrCall's source range; we still
        // identify it via symbol equality, not source position).
        val rawReceiver: IrExpression? = rootCall.dispatchReceiver ?: rootCall.extensionReceiver
        val paramSyms: Set<IrValueSymbol> = params.map { it.symbol }.toSet()
        val receiverParamSymbol: IrValueSymbol? = when (rawReceiver) {
            null -> null  // top-level function: no receiver at all.
            is IrGetValue -> {
                // The receiver must be one of the lambda's value params; any
                // other IrGetValue would need a different MethodMatcher spec.
                if (rawReceiver.symbol !in paramSyms) return null
                rawReceiver.symbol
            }
            else -> return null  // external receiver (class ref, call, etc.) — out of v0 scope.
        }

        val sigs = mutableListOf<ArgSig>()
        for (i in 0 until rootCall.valueArgumentsCount) {
            // Default-valued args the user didn't write surface as null here in
            // K2 IR. Skip them: the runtime MethodMatcher wildcards args, and
            // the after template never references unwritten positions.
            val arg = rootCall.getValueArgument(i) ?: continue
            when (arg) {
                is IrGetValue -> {
                    // Any lambda param is a valid arg-position capture; the
                    // after template resolves the source per-param below
                    // (receiver param → -1, otherwise its arg index).
                    if (arg.symbol !in paramSyms) return null
                    sigs += ArgSig.ParamRef(arg.symbol)
                }
                is IrConst -> sigs += ArgSig.LiteralConst(arg.kind, arg.value)
                else -> return null
            }
        }
        return BeforeLambda(params, rootCall, sigs, receiverParamSymbol)
    }

    /**
     * Builds the KotlinTemplate string + substitution-sources CSV for the
     * after lambda body. Strategy:
     *
     *  1. Compute the body's source span by walking the IR and taking the
     *     union of all elements' (non-zero-width) offsets. This subsumes
     *     both single-call bodies and nested calls like `s.foo(...).bar()`.
     *  2. Walk the body's IR for substitution spots: IrGetValue of a lambda
     *     value param (mapped by position to the before's params), or
     *     IrConst whose `(kind, value)` matches an unconsumed before-arg
     *     literal in source order.
     *  3. Spots whose offsets are inside the slice are substituted in place.
     *     Spots with zero-width offsets outside the slice (the extension-
     *     receiver case for the lambda receiver param) are emitted as a
     *     `#{any()}.` prepend to the template.
     *  4. The substitution-sources CSV lists slot indices in template
     *     left-to-right order: `-1` for receiver, `N >= 0` for arg N.
     *
     * Returns null if the after references a non-receiver param that the
     * before pattern doesn't capture, if any non-receiver spot falls outside
     * the slice, or if more than one prepend spot is needed.
     */
    private fun buildAfterTemplate(
        fnExpr: IrFunctionExpression,
        sourceText: String,
        before: BeforeLambda,
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

        // After's param[i] resolves to the before's param[i] by position. The
        // matched-method source it points to is either:
        //  - the receiver (-1) iff the before-param is the receiver-param, OR
        //  - the arg index where the before-param appears in the root call.
        // If the before pattern doesn't capture that param at all, the after
        // can't reference it — return null and fall back to no-op visitor.
        val paramSymToSource = HashMap<IrValueSymbol, Int>(afterParams.size)
        for (i in 0 until afterParams.size) {
            val beforeParamSym = before.params[i].symbol
            val source = if (beforeParamSym == before.receiverParamSymbol) {
                -1
            } else {
                val argIdx = before.argSignatures.indexOfFirst { sig ->
                    sig is ArgSig.ParamRef && sig.symbol == beforeParamSym
                }
                if (argIdx < 0) return null
                argIdx
            }
            paramSymToSource[afterParams[i].symbol] = source
        }

        // Unconsumed literal arg sigs, keyed by before-arg position. We consume
        // each at most once when matching IrConst nodes during the walk so
        // duplicate literal values pair positionally.
        data class LiteralSlot(val argPos: Int, val sig: ArgSig.LiteralConst, var consumed: Boolean = false)
        val literalSlots = before.argSignatures.mapIndexedNotNull { idx, sig ->
            if (sig is ArgSig.LiteralConst) LiteralSlot(idx, sig) else null
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
        val sliceStart = minStart
        val sliceEnd = maxEnd
        val slice = sourceText.substring(sliceStart, sliceEnd)

        data class Spot(
            val startOffset: Int,
            val endOffset: Int,
            val sourceIndex: Int,
            // Kotlin FQN to use in the placeholder (e.g. "kotlin.String"), or
            // null when the type isn't representable; null degrades gracefully
            // to an untyped `#{any()}` so the template still parses.
            val typeFqn: String?,
        )
        val spots = mutableListOf<Spot>()

        expr.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) { element.acceptChildrenVoid(this) }

            override fun visitGetValue(expression: IrGetValue) {
                val src = paramSymToSource[expression.symbol] ?: return
                spots += Spot(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    sourceIndex = src,
                    typeFqn = renderPlaceholderType(expression.symbol.owner.type),
                )
            }

            override fun visitConst(expression: IrConst) {
                val slot = literalSlots.firstOrNull { !it.consumed && it.sig.kind == expression.kind && it.sig.value == expression.value }
                    ?: return
                slot.consumed = true
                spots += Spot(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    sourceIndex = slot.argPos,
                    typeFqn = renderPlaceholderType(expression.type),
                )
            }
        })

        // Partition into in-slice (normal in-place substitution) and prepend
        // (zero-width-outside receiver). Reject other out-of-slice spots.
        val inSliceSpots = mutableListOf<Spot>()
        val prependSpots = mutableListOf<Spot>()
        for (spot in spots) {
            val inSlice = spot.startOffset in sliceStart until sliceEnd &&
                spot.endOffset in (spot.startOffset + 1)..sliceEnd
            if (inSlice) {
                inSliceSpots += spot
            } else if (spot.sourceIndex == -1) {
                prependSpots += spot
            } else {
                return null
            }
        }
        if (prependSpots.size > 1) return null

        // Right-to-left source-text substitution; left-to-right CSV ordering.
        // Each placeholder carries the slot's Kotlin type when we could render
        // one — narrower than `#{any()}` so KotlinTemplate validates that the
        // matched-value's type matches the source-level shape the recipe
        // author wrote.
        val templateBuilder = StringBuilder(slice)
        for (spot in inSliceSpots.sortedByDescending { it.startOffset }) {
            templateBuilder.replace(
                spot.startOffset - sliceStart,
                spot.endOffset - sliceStart,
                renderPlaceholder(spot.typeFqn),
            )
        }
        val template = if (prependSpots.isEmpty()) {
            templateBuilder.toString()
        } else {
            "${renderPlaceholder(prependSpots.single().typeFqn)}.$templateBuilder"
        }

        val orderedSources = mutableListOf<Int>()
        if (prependSpots.isNotEmpty()) orderedSources += prependSpots.single().sourceIndex
        orderedSources += inSliceSpots.sortedBy { it.startOffset }.map { it.sourceIndex }
        val csv = orderedSources.joinToString(",")

        return template to csv
    }

    /**
     * Tries to lower the recipe body to a stateless phase-mode edit: exactly
     * one top-level `edit { visitMethodInvocation { call -> ... } }` clause and
     * nothing else. The lambda body is whatever the user wrote — we don't
     * introspect it; the Kotlin compiler's lambda lowering emits it as a
     * regular `Function1<J.MethodInvocation, J.MethodInvocation>` instance,
     * and the runtime helper invokes it per method-invocation visit.
     *
     * The two-arg `edit(scanRef, block)` overload is intentionally rejected
     * here — it's the scan-bound variant whose lowering needs ScanningRecipe
     * (a separate, larger commit). Same for any `scan` / `generate` siblings.
     */
    private fun extractStatelessEditLambda(recipeCall: IrCall): VisitorLowering.PhaseStatelessEdit? {
        val callee = recipeCall.symbol.owner
        val blockIdx = callee.valueParameters.indexOfFirst { it.name == Name.identifier("block") }
        if (blockIdx < 0) return null
        val blockArg = recipeCall.arguments[blockIdx] as? IrFunctionExpression ?: return null
        val recipeStmts = (blockArg.function.body as? IrBlockBody)?.statements ?: return null
        // Exactly one top-level call — same shape constraint as pattern mode.
        // Mixing rules are enforced earlier by the FIR checker; this is just
        // the lowering's structural gate for what it can currently emit.
        // Allowing aux statements at the recipe-body level is a separate
        // relaxation (would require hoisting to class fields or constructor);
        // not in scope here.
        val firstRecipeStmt = recipeStmts.singleOrNull() ?: return null
        val editCall = (firstRecipeStmt as? IrReturn)?.value as? IrCall
            ?: firstRecipeStmt as? IrCall
            ?: return null
        // The single-arg `edit(block: EditScope.() -> Unit)` overload only.
        if (editCall.symbol.owner.kotlinFqName.asString() != "org.openrewrite.RecipeBuilder.edit") return null
        if (editCall.valueArgumentsCount != 1) return null
        val editBlockArg = editCall.getValueArgument(0) as? IrFunctionExpression ?: return null
        // Validate shape up front so an unsupported body falls through to the
        // no-op visitor instead of emitting a malformed override. The deep
        // copy + re-partition at emit time relies on this gate having passed.
        validateBlockHasExactlyOneVisitCall(
            block = editBlockArg,
            scopePrefix = EDIT_SCOPE_PREFIX,
        ) ?: return null
        return VisitorLowering.PhaseStatelessEdit(editBlock = editBlockArg)
    }

    /**
     * Splits a scan/edit block body into (auxiliary statements, the single
     * `visit*` call's lambda argument, the matched visit method's simple
     * name). The block must contain exactly one call whose FQN starts with
     * [scopePrefix] and whose simple-name suffix is in [VISIT_NAMES];
     * non-visit statements are aux and get hoisted into the override body so
     * the visit lambda captures them via closure.
     *
     * Returns null if zero or multiple recognised visit calls are present.
     * Multi-primitive support (mixing two different `visit*` calls in one
     * block) is a separate runway item — that path needs a richer runtime
     * helper signature, so for now we keep the structural gate at "single
     * visit anchor".
     */
    private class BlockPartition(
        val auxStatements: List<IrStatement>,
        val visitLambda: IrFunctionExpression,
        val visitMethodName: String,
    )

    private fun partitionVisitCall(
        stmts: List<IrStatement>,
        scopePrefix: String,
    ): BlockPartition? {
        var visitIdx = -1
        var visitLambda: IrFunctionExpression? = null
        var visitName: String? = null
        for ((i, stmt) in stmts.withIndex()) {
            val call = (stmt as? IrReturn)?.value as? IrCall
                ?: stmt as? IrCall
                ?: continue
            val fqn = call.symbol.owner.kotlinFqName.asString()
            if (!fqn.startsWith(scopePrefix)) continue
            val name = fqn.removePrefix(scopePrefix)
            if (name !in VISIT_NAMES) continue
            if (visitIdx >= 0) return null // more than one visit anchor
            if (call.valueArgumentsCount != 1) return null
            val lambda = call.getValueArgument(0) as? IrFunctionExpression ?: return null
            visitIdx = i
            visitLambda = lambda
            visitName = name
        }
        val lambda = visitLambda ?: return null
        val name = visitName ?: return null
        val aux = stmts.filterIndexed { i, _ -> i != visitIdx }
        return BlockPartition(auxStatements = aux, visitLambda = lambda, visitMethodName = name)
    }

    /**
     * Extractor-time shape check: confirms the scan/edit block contains
     * exactly one recognised `visit*` call. Doesn't return the partition
     * because the emit site re-runs partitioning on a deep copy (so the
     * recovered visit lambda and aux statements share consistent symbols
     * after copy). Returns `Unit` on success, null otherwise.
     */
    private fun validateBlockHasExactlyOneVisitCall(
        block: IrFunctionExpression,
        scopePrefix: String,
    ): Unit? {
        val stmts = (block.function.body as? IrBlockBody)?.statements ?: return null
        partitionVisitCall(stmts, scopePrefix) ?: return null
        return Unit
    }

    /**
     * Tries to lower the recipe body to scan + acc-threaded edit phase mode.
     * The shape we accept here is exactly the resume-brief minimum-viable demo:
     *
     *     val seen = scan<A>(initial = ...) { visitMethodInvocation { call -> /* uses acc */ } }
     *     edit(seen) { visitMethodInvocation { call -> /* uses acc */ } }
     *
     * — two top-level statements, the first an `IrVariable` whose initializer
     * is a `scan(...) { ... }` call, the second an `edit(scanRef) { ... }`
     * call. Each block may contain auxiliary statements (helper vals, etc.)
     * alongside exactly one `visitMethodInvocation { ... }` call. The aux
     * statements get hoisted into the generated override body so the visit
     * lambda captures them via closure; their `acc` references are rewritten
     * just like the visit lambda's. Anything that fails the shape match
     * returns null and the lowering falls through (recipe still compiles,
     * inherits the no-op visitor).
     */
    private fun extractScanEditPhase(recipeCall: IrCall): VisitorLowering.PhaseScanEdit? {
        val callee = recipeCall.symbol.owner
        val blockIdx = callee.valueParameters.indexOfFirst { it.name == Name.identifier("block") }
        if (blockIdx < 0) return null
        val blockArg = recipeCall.arguments[blockIdx] as? IrFunctionExpression ?: return null
        val recipeStmts = (blockArg.function.body as? IrBlockBody)?.statements ?: return null
        // Two-statement form: val seen = scan(...) { ... } ; edit(seen) { ... }.
        if (recipeStmts.size != 2) return null

        val scanVar = recipeStmts[0] as? IrVariable ?: return null
        val scanCall = scanVar.initializer as? IrCall ?: return null
        if (scanCall.symbol.owner.kotlinFqName.asString() != "org.openrewrite.RecipeBuilder.scan") return null
        // `scan<A>(initial, block)` — two value args, one type arg (A).
        val initialExpr = scanCall.getValueArgument(0) ?: return null
        val scanBlock = scanCall.getValueArgument(1) as? IrFunctionExpression ?: return null
        val accType: IrType = scanCall.typeArguments.firstOrNull() ?: return null
        validateBlockHasExactlyOneVisitCall(
            block = scanBlock,
            scopePrefix = SCAN_SCOPE_PREFIX,
        ) ?: return null

        val editStmt = recipeStmts[1]
        val editCall = (editStmt as? IrReturn)?.value as? IrCall
            ?: editStmt as? IrCall
            ?: return null
        if (editCall.symbol.owner.kotlinFqName.asString() != "org.openrewrite.RecipeBuilder.edit") return null
        // `edit(scanRef, block)` — the two-arg form. The single-arg form is
        // handled by extractStatelessEditLambda and doesn't pair with scan.
        if (editCall.valueArgumentsCount != 2) return null
        val scanRefArg = editCall.getValueArgument(0) as? IrGetValue ?: return null
        if (scanRefArg.symbol != scanVar.symbol) return null
        val editBlock = editCall.getValueArgument(1) as? IrFunctionExpression ?: return null
        validateBlockHasExactlyOneVisitCall(
            block = editBlock,
            scopePrefix = EDIT_SCOPE_WITH_ACC_PREFIX,
        ) ?: return null

        return VisitorLowering.PhaseScanEdit(
            accType = accType,
            initialExpr = initialExpr,
            scanBlock = scanBlock,
            editBlock = editBlock,
        )
    }

    /**
     * Emits the three ScanningRecipe overrides for a phase-mode recipe:
     * `getInitialValue(ctx): A`, `getScanner(acc): TreeVisitor<*,
     * ExecutionContext>`, and `getVisitor(acc): TreeVisitor<*,
     * ExecutionContext>`. The scanner and visitor bodies wrap the user's
     * (deep-copied) inner lambda in the appropriate runtime helper; before
     * passing the lambda to the helper, references to the outer
     * `ScanScope<A>.acc` / `EditScopeWithAcc<A>.acc` getter inside the body
     * are rewritten to read the local method's `acc` parameter (which the
     * Kotlin compiler then captures as a closure when lowering the lambda
     * to a Function class).
     */
    private fun addPhaseScanEditOverrides(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        lowering: VisitorLowering.PhaseScanEdit,
    ) {
        val getInitialValueFn = ctx.getInitialValue
        val getScannerFn = ctx.getScanner
        val getVisitorAccFn = ctx.getVisitorAcc
        val ecCls = ctx.executionContextClassSymbol
        // ScanningRecipe must be fully resolved for the lowering to be valid.
        // The class shell still got built (extending Recipe in fallback) so
        // skipping overrides keeps the recipe loadable; it just won't transform.
        if (getInitialValueFn == null || getScannerFn == null || getVisitorAccFn == null) return
        if (ecCls == null) return

        addGetInitialValueOverride(cls, ctx, getInitialValueFn, ecCls, lowering)
        addPhaseVisitorOverride(
            cls = cls,
            ctx = ctx,
            overrides = getScannerFn,
            helpersByVisitName = ctx.scanVisitorHelpers,
            lowering = lowering,
            block = lowering.scanBlock,
            scopePrefix = SCAN_SCOPE_PREFIX,
            accGetterToRewrite = ctx.scanScopeAccGetterSymbol,
            jvmName = "getScanner",
        )
        // No edit phase = leave the framework's default getVisitor(acc) which
        // is `TreeVisitor.noop()`. PhaseScanEdit currently always carries an
        // edit block; the null branch is there for the future scan-only path.
        val editBlock = lowering.editBlock
        if (editBlock != null) {
            addPhaseVisitorOverride(
                cls = cls,
                ctx = ctx,
                overrides = getVisitorAccFn,
                helpersByVisitName = ctx.editVisitorHelpers,
                lowering = lowering,
                block = editBlock,
                scopePrefix = EDIT_SCOPE_WITH_ACC_PREFIX,
                accGetterToRewrite = ctx.editScopeWithAccGetterSymbol,
                jvmName = "getVisitor",
            )
        }
    }

    private fun addGetInitialValueOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        executionContextClass: IrClassSymbol,
        lowering: VisitorLowering.PhaseScanEdit,
    ) {
        cls.addFunction(
            name = "getInitialValue",
            returnType = lowering.accType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            addValueParameter("ctx", executionContextClass.defaultType)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irReturn(lowering.initialExpr.deepCopyWithSymbols(initialParent = this@apply))
            }
        }
    }

    /**
     * Shared shape for getScanner(acc) and getVisitor(acc). Differs only in
     * the helper to call and which acc-getter symbol to rewrite — but the
     * structure is identical: deep-copy the user's scan/edit block, hoist
     * any aux statements as locals of the override method, rewrite acc
     * references (in both aux and the visit lambda) against the new `acc`
     * parameter, then return the helper call wrapping the visit lambda.
     */
    private fun addPhaseVisitorOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        helpersByVisitName: Map<String, IrSimpleFunctionSymbol>,
        lowering: VisitorLowering.PhaseScanEdit,
        block: IrFunctionExpression,
        scopePrefix: String,
        accGetterToRewrite: IrSimpleFunctionSymbol?,
        jvmName: String,
    ) {
        // Override return type follows the helper's return type
        // (`TreeVisitor<?, ExecutionContext>`). We need to expand the block
        // before we can pick the helper (its visit-method-name comes from
        // the partition), so build the override body and look up the helper
        // inside; if no matching helper resolves, abort and leave the parent
        // class's default override.
        // Pick any helper to determine the return type — they all return
        // `TreeVisitor<?, ExecutionContext>`. If the map is empty there's
        // nothing we can do anyway.
        val sampleHelper = helpersByVisitName.values.firstOrNull() ?: return
        cls.addFunction(
            name = jvmName,
            returnType = sampleHelper.owner.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            val accParam = addValueParameter("acc", lowering.accType)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                val expansion = expandBlockIntoOverride(
                    block = block,
                    scopePrefix = scopePrefix,
                    overrideFunction = this@apply,
                )
                val helperSymbol = helpersByVisitName[expansion.visitMethodName]
                    ?: return@irBlockBody
                // Acc rewriting covers both aux statements and the visit
                // lambda — aux may reference the scope's `acc` (e.g.
                // `val n = acc.size`) just as freely as the visit body.
                for (aux in expansion.auxStatements) {
                    rewriteAccReferencesIn(aux, accParam, accGetterToRewrite)
                    +aux
                }
                rewriteAccReferencesIn(expansion.visitLambda, accParam, accGetterToRewrite)
                val factoryCall = irCall(
                    callee = helperSymbol,
                    type = helperSymbol.owner.returnType,
                )
                factoryCall.arguments[0] = expansion.visitLambda
                +irReturn(factoryCall)
            }
        }
    }

    /**
     * Rewrites every `IrCall` to the given acc-getter inside [element] into an
     * `IrGetValue` of the supplied `acc` parameter. Recurses through children
     * via [transformChildrenVoid]; works for an `IrFunctionExpression` (lambda
     * whose body holds the references) or any `IrStatement` (aux declaration
     * or expression whose subtree might contain an acc reference).
     *
     * The user's body sees `acc` as a property on the outer scan/edit
     * receiver scope. Once we hoist into a generated method whose own `acc`
     * parameter is the live accumulator, those getter calls must route to
     * the parameter instead — otherwise they'd dereference a scope-instance
     * that doesn't exist at runtime.
     */
    private fun rewriteAccReferencesIn(
        element: IrElement,
        accParam: IrValueParameter,
        accGetterToRewrite: IrSimpleFunctionSymbol?,
    ) {
        if (accGetterToRewrite == null) return
        element.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol == accGetterToRewrite) {
                    return IrGetValueImpl(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                        type = accParam.type,
                        symbol = accParam.symbol,
                    )
                }
                return super.visitCall(expression)
            }
        })
    }

    /**
     * Lowering for stateless phase-mode edit. The generated `getVisitor()`
     * deep-copies the whole `edit { }` block once so all internal symbol
     * cross-references stay coherent, then re-partitions the copy into
     * (aux statements, the inner visit lambda). Aux statements are emitted
     * as locals of `getVisitor()`; the visit lambda is the argument to
     * `GeneratedRecipeSupport.methodInvocationEditVisitor`. Because the
     * Kotlin compiler lowers the visit lambda into a Function1 class that
     * captures its enclosing locals, the aux declarations are visible to
     * the lambda at runtime via standard closure capture.
     */
    private fun addPhaseEditGetVisitorOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        editBlock: IrFunctionExpression,
    ) {
        cls.addFunction(
            name = "getVisitor",
            returnType = overrides.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                val expansion = expandBlockIntoOverride(
                    block = editBlock,
                    scopePrefix = EDIT_SCOPE_PREFIX,
                    overrideFunction = this@apply,
                )
                // Helper symbol is selected by the visit-method-name recovered
                // from the partition. If a `visit*` primitive's helper isn't
                // resolvable (older GeneratedRecipeSupport on the classpath
                // missing the helper), fall through to the no-op visitor by
                // skipping the override entirely.
                val factorySymbol = ctx.editVisitorHelpers[expansion.visitMethodName]
                    ?: return@irBlockBody
                for (aux in expansion.auxStatements) +aux
                val factoryCall = irCall(
                    callee = factorySymbol,
                    type = factorySymbol.owner.returnType,
                )
                factoryCall.arguments[0] = expansion.visitLambda
                +irReturn(factoryCall)
            }
        }
    }

    /**
     * Result of deep-copying a scan/edit block and repartitioning the copy:
     * the auxiliary statements (with their `parent` updated to the override
     * method), the single visit-call's inner lambda (with its
     * `function.parent` updated to the override method), and the visit
     * method's simple name (used by the caller to pick the right runtime
     * helper). All three pieces come from the same deep copy so any
     * cross-references — e.g., an aux `val` referenced from inside the visit
     * lambda — stay symbol-coherent.
     */
    private class BlockExpansion(
        val auxStatements: List<IrStatement>,
        val visitLambda: IrFunctionExpression,
        val visitMethodName: String,
    )

    private fun expandBlockIntoOverride(
        block: IrFunctionExpression,
        scopePrefix: String,
        overrideFunction: IrSimpleFunction,
    ): BlockExpansion {
        // Deep-copy the whole block as one unit. The initialParent only
        // re-parents the top-level node (the inner IrSimpleFunction) — child
        // declarations inside the body still point at that inner function
        // until we reparent them below.
        val copiedBlock = block.deepCopyWithSymbols(initialParent = overrideFunction)
        val stmts = (copiedBlock.function.body as IrBlockBody).statements
        // Guaranteed to succeed: the extractor already validated the shape.
        // If a later refactor breaks that invariant, fail loudly here rather
        // than silently emit a malformed override.
        val partition = partitionVisitCall(stmts, scopePrefix)
            ?: error("Expected exactly one $scopePrefix visit call in deep-copied block; extractor invariant violated.")
        val visitLambda = partition.visitLambda
        // Reparent aux declarations from the inner block function up to the
        // override function so they read as locals of the override body
        // rather than locals of a now-orphaned lambda function.
        for (aux in partition.auxStatements) {
            if (aux is IrVariable) aux.parent = overrideFunction
        }
        // The visit lambda is moving out of the block's body and into the
        // helper-call argument position; its function.parent has to follow.
        visitLambda.function.parent = overrideFunction
        return BlockExpansion(
            auxStatements = partition.auxStatements,
            visitLambda = visitLambda,
            visitMethodName = partition.visitMethodName,
        )
    }

    private fun addGetVisitorOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        factorySymbol: IrSimpleFunctionSymbol,
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
                    callee = factorySymbol,
                    type = factorySymbol.owner.returnType,
                )
                factoryCall.arguments[0] = irString(templates.matcherSpecs.joinToString("\n"))
                factoryCall.arguments[1] = irString(templates.afterTemplate)
                factoryCall.arguments[2] = irString(templates.substitutionSourcesCsv)
                +irReturn(factoryCall)
            }
        }
    }

    private fun addEstimatedEffortOverride(
        cls: IrClass,
        ctx: RecipeIrGenContext,
        overrides: IrSimpleFunction,
        literal: String,
    ) {
        val parseSymbol = ctx.durationParseSymbol ?: return
        cls.addFunction(
            name = "getEstimatedEffortPerOccurrence",
            returnType = overrides.returnType,
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        ).apply {
            overriddenSymbols = listOf(overrides.symbol)
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                val parseCall = irCall(
                    callee = parseSymbol,
                    type = parseSymbol.owner.returnType,
                )
                parseCall.arguments[0] = irString(literal)
                +irReturn(parseCall)
            }
        }
    }
}
