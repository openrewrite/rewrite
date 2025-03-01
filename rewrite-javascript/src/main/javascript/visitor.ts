import {EmptyMarkers, Markers} from "./markers";
import {Cursor, isSourceFile, rootCursor, SourceFile, Tree} from "./tree";
import {createDraft, Draft, finishDraft, Objectish} from "immer";
import {mapAsync} from "./util";

/* Not exported beyond the internal immer module */
export type ValidImmerRecipeReturnType<State> =
    | State
    | void
    | undefined

export async function produceAsync<Base extends Objectish>(
    before: Promise<Base> | Base,
    recipe: (draft: Draft<Base>) => ValidImmerRecipeReturnType<Draft<Base>> |
        PromiseLike<ValidImmerRecipeReturnType<Draft<Base>>>
): Promise<Base> {
    const b: Base = await before;
    const draft = createDraft(b);
    await recipe(draft);
    return finishDraft(draft) as Base;
}

export abstract class TreeVisitor<T extends Tree, P> {
    protected cursor: Cursor = rootCursor();
    private visitCount: number = 0;
    private afterVisit?: TreeVisitor<any, P>[];

    async visitDefined<R extends Tree>(tree: Tree, p: P, parent?: Cursor): Promise<R> {
        return (await this.visit<R>(tree, p, parent))!;
    }

    async visit<R extends Tree>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        if (parent !== undefined) {
            this.cursor = parent;
        }

        let topLevel = false;
        if (this.visitCount === 0) {
            topLevel = true;
        }

        this.visitCount += 1;
        this.cursor = new Cursor(tree, this.cursor);

        let t: T | undefined
        const isAcceptable = (!(isSourceFile(tree)) ||
            this.isAcceptable(tree, p));

        try {
            if (isAcceptable) {
                t = await this.preVisit(tree as T, p)
                if (this.cursor.messages.get("STOP_AFTER_PRE_VISIT") !== true) {
                    if (t !== undefined) {
                        t = await this.accept(t, p)
                    }
                    if (t !== undefined) {
                        t = await this.postVisit(t, p)
                    }
                }
            }

            this.cursor = this.cursor.parent!;

            if (topLevel) {
                if (this.afterVisit) {
                    for (const v of this.afterVisit) {
                        v.cursor = this.cursor;
                        if (t !== undefined) {
                            t = await v.visit(t, p);
                        }
                    }
                }
                this.afterVisit = undefined;
                this.visitCount = 0;
            }
        } catch (e) {
            if (e instanceof RecipeRunError) {
                throw e;
            }
            throw new RecipeRunError(e as Error, this.cursor);
        }

        return (isAcceptable ? t : tree) as unknown as R;
    }

    protected async accept(t: T, p: P): Promise<T | undefined> {
        return t
    }

    isAcceptable(sourceFile: SourceFile, p: P): boolean {
        return true;
    }

    protected async preVisit(tree: T, p: P): Promise<T | undefined> {
        return tree;
    }

    protected async postVisit(tree: T, p: P): Promise<T | undefined> {
        return tree;
    }

    protected async visitMarkers(markers: Markers, p: P): Promise<Markers> {
        if (markers === EmptyMarkers) {
            return EmptyMarkers;
        } else if (markers.markers.length === 0) {
            return markers;
        }
        return produceAsync<Markers>(markers, async (draft) => {
            draft.markers = await mapAsync(markers.markers, m => this.visitMarker(m, p))
        });
    }

    protected async visitMarker<M>(marker: M, p: P): Promise<M> {
        return marker;
    }

    protected async produceTree<T extends Tree>(
        before: T,
        p: P,
        recipe?:
            ((draft: Draft<T>) => ValidImmerRecipeReturnType<Draft<T>>) |
            ((draft: Draft<T>) => Promise<ValidImmerRecipeReturnType<Draft<T>>>)
    ): Promise<T> {
        const draft: Draft<T> = createDraft(before);
        (draft as Draft<Tree>).markers = await this.visitMarkers(before.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft(draft) as T;
    }
}

export function noopVisitor<T extends Tree, P>() {
    return new class extends TreeVisitor<T, P> {
        async visit<R extends Tree>(tree: Tree): Promise<R | undefined> {
            return tree as unknown as R;
        }
    }
}

export class RecipeRunError extends Error {
    readonly cause: Error;
    readonly cursor?: Cursor;

    constructor(cause: Error, cursor?: Cursor) {
        super();
        this.cause = cause;
        this.cursor = cursor;
    }
}

