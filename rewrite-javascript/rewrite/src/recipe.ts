/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {noopVisitor, TreeVisitor} from "./visitor";
import {Cursor, SourceFile, Tree} from "./tree";
import {ExecutionContext} from "./execution";
import {DataTableDescriptor} from "./data-table";
import {mapAsync} from "./util";

const OPTIONS_KEY = "__recipe_options__";

export type Minutes = number;

export abstract class Recipe {
    constructor(options?: {}) {
        if (options) {
            Object.assign(this, options);
        }
    }

    /**
     * A unique name for the recipe consisting of a dot separated sequence of category
     * names in which the recipe should appear followed by a name. For example,
     * "org.openrewrite.typescript.find-methods-by-pattern".
     */
    readonly abstract name: string

    /**
     * A human-readable display name for the recipe, initial capped with no period.
     * For example, "Find text". The display name can be assumed to be rendered in
     * documentation and other places where markdown is understood, so it is possible
     * to use stylistic markers like backticks to indicate types. For example,
     * "Find uses of `console.log`".
     *
     * @language Markdown
     */
    readonly abstract displayName: string

    /**
     * A human-readable description for the recipe, consisting of one or more full
     * sentences ending with a period.
     *
     * "Find methods by pattern." is an example. The description can be assumed to be rendered in
     * documentation and other places where markdown is understood, so it is possible
     * to use stylistic markers like backticks to indicate types. For example,
     * "Find uses of `console.log`.".
     *
     * @language Markdown.
     */
    readonly abstract description: string

    readonly tags: string[] = []

    readonly estimatedEffortPerOccurrence: Minutes = 5

    readonly dataTables: DataTableDescriptor[] = []

    async recipeList(): Promise<Recipe[]> {
        return []
    }

    /**
     * A human-readable display name for this recipe instance, including some descriptive
     * text about the recipe options that are supplied, if any. The name must be
     * initial capped with no period. For example, "Find text "hello world"".
     *
     * For consistency, when surrounding option descriptive text in quotes to visually differentiate
     * it from the text before it, use single ``.
     *
     * Override to provide meaningful recipe instance names for recipes with complex sets of options.
     *
     * @return A name that describes this recipe instance.
     */
    instanceName(): string {
        return this.displayName
    }

    async descriptor(): Promise<RecipeDescriptor> {
        const optionsRecord: Record<string, OptionDescriptor> = (this as any).constructor[OPTIONS_KEY] || {}
        return {
            name: this.name,
            displayName: this.displayName,
            instanceName: this.instanceName(),
            description: this.description,
            tags: this.tags,
            estimatedEffortPerOccurrence: this.estimatedEffortPerOccurrence,
            recipeList: await mapAsync(await this.recipeList(), async r => r.descriptor()),
            options: Object.entries(optionsRecord).map(([key, descriptor]) => ({
                name: key,
                value: (this as any)[key],
                required: descriptor.required ?? true,
                ...descriptor
            }))
        }
    }

    /**
     * Returns the visitor that performs the transformation. This method is called by the
     * recipe framework during execution and must be overridden by concrete recipe implementations.
     *
     * @returns A visitor that performs the recipe's transformation
     */
    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return noopVisitor()
    }

    /**
     * At the end of a recipe run, the recipe scheduler will call this method to allow the
     * recipe to perform any cleanup or finalization tasks. This method is guaranteed to be called
     * only once per run.
     *
     * @param ctx The recipe run execution context.
     */
    async onComplete(ctx: ExecutionContext): Promise<void> {
    }
}

export interface RecipeDescriptor {
    readonly name: string
    readonly displayName: string
    readonly instanceName: string
    readonly description: string
    readonly tags: string[]
    readonly estimatedEffortPerOccurrence: Minutes
    readonly recipeList: RecipeDescriptor[]
    readonly options: ({ name: string, value?: any } & OptionDescriptor)[]
}

export interface OptionDescriptor {
    readonly displayName: string
    readonly description: string
    readonly required?: boolean
    readonly example?: string
    readonly valid?: string[]
}

export abstract class ScanningRecipe<P> extends Recipe {
    private readonly recipeAccMessage = Symbol("org.openrewrite.recipe.acc");

    accumulator(cursor: Cursor, ctx: ExecutionContext): P {
        const ms = cursor.root.messages;
        if (!ms.has(this.recipeAccMessage)) {
            ms.set(this.recipeAccMessage, this.initialValue(ctx));
        }
        return ms.get(this.recipeAccMessage);
    }

    abstract initialValue(ctx: ExecutionContext): P

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const editorWithContext = (cursor: Cursor, ctx: ExecutionContext) =>
            this.editorWithData(this.accumulator(cursor, ctx));

        return new class extends TreeVisitor<any, ExecutionContext> {
            private delegate?: TreeVisitor<any, ExecutionContext>

            async isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Promise<boolean> {
                return (await this.delegateForCtx(ctx)).isAcceptable(sourceFile, ctx);
            }

            async visit<R extends Tree>(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
                return (await this.delegateForCtx(ctx)).visit(tree, ctx, parent);
            }

            private async delegateForCtx(ctx: ExecutionContext) {
                if (!this.delegate) {
                    this.delegate = await editorWithContext(this.cursor, ctx);
                }
                return this.delegate;
            }
        }
    }

    async editorWithData(acc: P): Promise<TreeVisitor<any, ExecutionContext>> {
        return noopVisitor();
    }

    async generate(acc: P, ctx: ExecutionContext): Promise<SourceFile[]> {
        return [];
    }

    async scanner(acc: P): Promise<TreeVisitor<any, ExecutionContext>> {
        return noopVisitor();
    }
}

/**
 * Do not permit overriding of editor()
 */
Object.freeze(ScanningRecipe.prototype.editor);

export class RecipeRegistry {
    /**
     * The registry map stores recipe constructors keyed by their name.
     */
    all = new Map<string, new (options?: any) => Recipe>();

    public register<T extends Recipe>(recipeClass: new (options?: any) => T): void {
        try {
            const r = new recipeClass({});
            this.all.set(r.name, recipeClass);
        } catch (e) {
            throw new Error(`Failed to register recipe. Ensure the constructor can be called without any arguments.`);
        }
    }
}

export function Option(descriptor: OptionDescriptor) {
    return function (target: any, propertyKey: string) {
        // Ensure the constructor has options storage.
        if (!target.constructor.hasOwnProperty(OPTIONS_KEY)) {
            Object.defineProperty(target.constructor, OPTIONS_KEY, {
                value: {},
                writable: true,
                configurable: true,
            });
        }

        // Register the option metadata under the property key.
        target.constructor[OPTIONS_KEY][propertyKey] = descriptor;
    };
}

/**
 * Mark a property as transient, meaning it should not be part of the serialized form of
 * a recipe.
 *
 * @param target
 * @param propertyKey
 * @constructor
 */
export function Transient(target: any, propertyKey: string) {
    // Get the property descriptor, if any, then redefine it as non-enumerable.
    const descriptor = Object.getOwnPropertyDescriptor(target, propertyKey) || {
        configurable: true,
        writable: true,
    };
    descriptor.enumerable = false;
    Object.defineProperty(target, propertyKey, descriptor);
}
