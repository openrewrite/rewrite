import {noopVisitor, TreeVisitor} from "./visitor";
import {Cursor, SourceFile, Tree} from "./tree";
import {ExecutionContext} from "./execution";
import {Duration, TimeDuration} from "typed-duration";
import {randomId} from "./uuid";

export abstract class Recipe {
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

    readonly estimatedEffortPerOccurrence: TimeDuration = Duration.minutes.of(5)

    abstract get editor(): TreeVisitor<any, ExecutionContext>
}

class MyRecipe extends Recipe {
    name: string = "org.openrewrite.typescript.my-recipe";
    displayName: string = `My recipe [this](https://hi.com).`;
    description: string = `My description.`;
    tags: string[] = ["tag1", "tag2"];

    get editor(): TreeVisitor<any, ExecutionContext> {
        return noopVisitor();
    }
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
