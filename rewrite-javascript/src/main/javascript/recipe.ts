import {noopVisitor, TreeVisitor} from "./visitor";
import {Cursor, SourceFile, Tree} from "./tree";
import {ExecutionContext} from "./execution";
import {randomId} from "./uuid";

const OPTIONS_KEY = "__recipe_options__";

export type Minutes = number;

export abstract class Recipe {
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

    get descriptor(): RecipeDescriptor {
        const optionRecord: Record<string, OptionDescriptor> = (this as any).constructor[OPTIONS_KEY] || {}
        return {
            displayName: this.displayName,
            instanceName: this.instanceName(),
            description: this.description,
            tags: this.tags,
            estimatedEffortPerOccurrence: this.estimatedEffortPerOccurrence,
            options: Object.entries(optionRecord).map(([key, descriptor]) => ({
                name: key,
                ...descriptor
            }))
        }
    }

    get editor(): TreeVisitor<any, ExecutionContext> {
        return noopVisitor()
    }
}

export interface RecipeDescriptor {
    readonly displayName: string
    readonly instanceName: string
    readonly description: string
    readonly tags: string[]
    readonly estimatedEffortPerOccurrence: Minutes
    readonly options: ({ name: string } & OptionDescriptor)[]
}

export interface OptionDescriptor {
    readonly displayName: string
    readonly description: string
    readonly example?: string
    readonly valid?: string[],
}

export abstract class ScanningRecipe<P> extends Recipe {
    private recipeAccMessage: string = `org.openrewrite.recipe.acc.${randomId()}`;

    accumulator(cursor: Cursor, ctx: ExecutionContext): P {
        const ms = cursor.root.messages;
        if (!ms.has(this.recipeAccMessage)) {
            ms.set(this.recipeAccMessage, this.initialValue(ctx));
        }
        return ms.get(this.recipeAccMessage);
    }

    abstract initialValue(ctx: ExecutionContext): P

    get editor(): TreeVisitor<any, ExecutionContext> {
        const editorWithContext = (cursor: Cursor, ctx: ExecutionContext) =>
            this.editorWithData(this.accumulator(cursor, ctx));

        return new class extends TreeVisitor<any, ExecutionContext> {
            private delegate?: TreeVisitor<any, ExecutionContext>

            isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): boolean {
                return this.delegateForCtx(ctx).isAcceptable(sourceFile, ctx);
            }

            async visit<R extends Tree>(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
                return this.delegateForCtx(ctx).visit(tree, ctx, parent);
            }

            private delegateForCtx(ctx: ExecutionContext) {
                if (!this.delegate) {
                    this.delegate = editorWithContext(this.cursor, ctx);
                }
                return this.delegate;
            }
        }
    }

    editorWithData(acc: P): TreeVisitor<any, ExecutionContext> {
        return noopVisitor();
    }

    generate(acc: P): SourceFile[] {
        return [];
    }

    scanner(acc: P): TreeVisitor<any, ExecutionContext> {
        return noopVisitor();
    }
}

/**
 * Do not permit overriding of editor()
 */
Object.freeze(ScanningRecipe.prototype.editor);

export class RecipeRegistry {
    // The registry map stores recipe constructors keyed by their instance name.
    static all = new Map<string, { new(): Recipe }>();

    public static register(name: string, recipeClass: { new(): Recipe }): void {
        RecipeRegistry.all.set(name, recipeClass);
    }
}

/**
 * @param recipeName A unique name for the recipe consisting of a dot separated sequence of category
 * names in which the recipe should appear followed by a name. For example,
 * "org.openrewrite.typescript.find-methods-by-pattern".
 */
export function Registered(recipeName: string) {
    return function <T extends { new(...args: any[]): Recipe }>(constructor: T): T {
        try {
            new constructor();
            RecipeRegistry.register(recipeName, constructor);
            return constructor;
        } catch (e) {
            throw new Error(`Failed to register recipe ${recipeName}. Ensure the constructor can be called without any arguments.`);
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
