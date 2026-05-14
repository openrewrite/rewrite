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
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
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
 *  1. Reads the constant `displayName` and `description` arguments.
 *  2. Emits a synthetic top-level class `Generated$<PropertyName>` that
 *     extends `org.openrewrite.Recipe`, overriding `getDisplayName()` and
 *     `getDescription()` to return those constants.
 *  3. Rewrites the original `recipe(...)` call expression in the property
 *     initializer to a constructor invocation of the generated class.
 *
 * Intentionally not done yet (deferred to follow-up commits):
 *  - Translating the trailing builder lambda (`rewrite ... to ...` / phase
 *    mode) into a real `getVisitor()` override. The generated class today
 *    inherits the framework default no-op visitor — enough to compile and
 *    instantiate, but not to actually transform code.
 *  - Wiring `tags` and `estimatedEffortPerOccurrence`.
 *
 * Without this pass the call to `org.openrewrite.recipe` falls through to
 * its runtime stub in `RecipeDsl.kt`, which throws to flag the missing
 * plugin. Once this pass replaces the call, the val initializer becomes a
 * direct constructor invocation and the runtime stub is never reached.
 */
internal class RecipeIrGenerationExtension : IrGenerationExtension {

    private companion object {
        val RECIPE_FQN: FqName = FqName("org.openrewrite.recipe")
    }

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
        val durationClassSymbol: IrClassSymbol?,
        val durationParseSymbol: IrSimpleFunctionSymbol?,
    )

    private fun processFile(file: IrFile, ctx: RecipeIrGenContext) {
        // First pass: identify recipe properties, emit synthetic classes, and
        // record the call→constructor-call replacement map. We don't mutate
        // the initializer in place because the transform below needs the
        // original IrCall identity to match against.
        val replacements: MutableMap<IrCall, IrConstructorCall> = LinkedHashMap()

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

            val generatedClass = buildGeneratedRecipeClass(
                ctx = ctx,
                parentFile = file,
                propertyName = declaration.name,
                metadata = metadata,
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

    private fun buildGeneratedRecipeClass(
        ctx: RecipeIrGenContext,
        parentFile: IrFile,
        propertyName: Name,
        metadata: RecipeMetadata,
    ): IrClass {
        val cls = ctx.pluginContext.irFactory.buildClass {
            name = Name.identifier("Generated\$${propertyName.asString()}")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }
        cls.parent = parentFile
        cls.superTypes = listOf(ctx.recipeClassSymbol.defaultType)
        cls.createThisReceiverParameter()

        cls.addConstructor {
            isPrimary = true
            // IrClass.defaultType lives in `org.jetbrains.kotlin.ir.util`; we avoid
            // adding a second import that collides on simple name with the symbol-side
            // extension by going through the class symbol.
            returnType = cls.symbol.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(ctx.pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(ctx.recipeNoArgCtorSymbol.owner)
            }
        }

        addStringOverride(cls, ctx.pluginContext, "getDisplayName", metadata.displayName, ctx.getDisplayName)
        addStringOverride(cls, ctx.pluginContext, "getDescription", metadata.description, ctx.getDescription)

        if (metadata.tagsArg != null && ctx.getTags != null) {
            addTagsOverride(cls, ctx, ctx.getTags, metadata.tagsArg)
        }
        if (metadata.estimatedEffortLiteral != null &&
            ctx.getEstimatedEffort != null &&
            ctx.durationParseSymbol != null
        ) {
            // Validate the ISO-8601 literal at compile time. Bad input falls through
            // to a no-override (Recipe's default 5min); a future FIR checker will
            // surface this as a user-visible error before then.
            if (parsesAsIsoDuration(metadata.estimatedEffortLiteral)) {
                addEstimatedEffortOverride(cls, ctx, ctx.getEstimatedEffort, metadata.estimatedEffortLiteral)
            }
        }
        return cls
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
