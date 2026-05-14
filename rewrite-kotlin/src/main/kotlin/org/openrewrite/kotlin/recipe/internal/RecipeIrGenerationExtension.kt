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
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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

        for (file in moduleFragment.files) {
            processFile(
                file = file,
                pluginContext = pluginContext,
                recipeClassSymbol = recipeClassSymbol,
                recipeNoArgCtorSymbol = recipeNoArgCtor,
                getDisplayName = recipeGetDisplayName,
                getDescription = recipeGetDescription,
            )
        }
    }

    private fun processFile(
        file: IrFile,
        pluginContext: IrPluginContext,
        recipeClassSymbol: IrClassSymbol,
        recipeNoArgCtorSymbol: IrConstructorSymbol,
        getDisplayName: IrSimpleFunction,
        getDescription: IrSimpleFunction,
    ) {
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

            val (displayName, description) = readMetadata(initializerExpr) ?: continue

            val generatedClass = buildGeneratedRecipeClass(
                pluginContext = pluginContext,
                parentFile = file,
                propertyName = declaration.name,
                displayName = displayName,
                description = description,
                recipeClassSymbol = recipeClassSymbol,
                recipeNoArgCtorSymbol = recipeNoArgCtorSymbol,
                getDisplayName = getDisplayName,
                getDescription = getDescription,
            )
            file.addChild(generatedClass)

            val generatedCtor = generatedClass.declarations
                .filterIsInstance<IrConstructor>()
                .single().symbol

            val builder = DeclarationIrBuilder(
                generatorContext = pluginContext,
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
     * Extracts the constant `displayName` and `description` arguments from a
     * `recipe(...)` call. Returns null if either is missing or non-constant —
     * the IR pass simply leaves such calls alone, and the runtime stub error
     * informs the author when it fires.
     */
    private fun readMetadata(call: IrCall): Pair<String, String>? {
        val callee = call.symbol.owner
        val displayNameIdx = callee.valueParameters.indexOfFirst {
            it.name == Name.identifier("displayName")
        }
        val descriptionIdx = callee.valueParameters.indexOfFirst {
            it.name == Name.identifier("description")
        }
        if (displayNameIdx < 0 || descriptionIdx < 0) return null
        val displayName = (call.arguments[displayNameIdx] as? IrConst)?.value as? String ?: return null
        val description = (call.arguments[descriptionIdx] as? IrConst)?.value as? String ?: return null
        return displayName to description
    }

    private fun buildGeneratedRecipeClass(
        pluginContext: IrPluginContext,
        parentFile: IrFile,
        propertyName: Name,
        displayName: String,
        description: String,
        recipeClassSymbol: IrClassSymbol,
        recipeNoArgCtorSymbol: IrConstructorSymbol,
        getDisplayName: IrSimpleFunction,
        getDescription: IrSimpleFunction,
    ): IrClass {
        val cls = pluginContext.irFactory.buildClass {
            name = Name.identifier("Generated\$${propertyName.asString()}")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }
        cls.parent = parentFile
        cls.superTypes = listOf(recipeClassSymbol.defaultType)
        cls.createThisReceiverParameter()

        cls.addConstructor {
            isPrimary = true
            // IrClass.defaultType lives in `org.jetbrains.kotlin.ir.util`; we avoid
            // adding a second import that collides on simple name with the symbol-side
            // extension by going through the class symbol.
            returnType = cls.symbol.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(recipeNoArgCtorSymbol.owner)
            }
        }

        addStringOverride(cls, pluginContext, "getDisplayName", displayName, getDisplayName)
        addStringOverride(cls, pluginContext, "getDescription", description, getDescription)
        return cls
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
}
