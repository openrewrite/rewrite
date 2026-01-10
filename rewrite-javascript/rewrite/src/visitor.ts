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
import {emptyMarkers, Marker, Markers} from "./markers";
import {Cursor, isSourceFile, rootCursor, SourceFile, Tree} from "./tree";
import {create, Draft} from "mutative";
import {mapAsync, mapSync, updateIfChanged} from "./util";

type Objectish = Record<string, any> | Array<any>;

export type ValidRecipeReturnType<State> = State | void | undefined;

export async function produceAsync<Base extends Objectish>(
    before: Base | Base,
    recipe: (draft: Draft<Base>) => ValidRecipeReturnType<Draft<Base>> |
        PromiseLike<ValidRecipeReturnType<Draft<Base>>>
): Promise<Base | undefined> {
    const b: Base = before;
    // Mutative's create(base, recipe) supports async recipes and rawReturn(undefined)
    return create(b, recipe as any) as Base | undefined;
}

export function produceSync<Base extends Objectish>(
    before: Base | Base,
    recipe: (draft: Draft<Base>) => ValidRecipeReturnType<Draft<Base>>
): Base | undefined {
    const b: Base = before;
    return create(b, recipe as any) as Base | undefined;
}

const stopAfterPreVisit = Symbol.for("STOP_AFTER_PRE_VISIT")

export abstract class TreeVisitor<T extends Tree, P> {
    readonly async = false as const;
    protected cursor: Cursor = rootCursor();
    private visitCount: number = 0;
    public afterVisit: TreeVisitor<any, P>[] = [];

    visitDefined<R extends T>(tree: Tree, p: P, parent?: Cursor): R {
        return this.visit<R>(tree, p, parent)!;
    }

    visit<R extends T>(tree: Tree, p: P, parent?: Cursor): R | undefined {
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
                t = this.preVisit(tree as T, p)
                if (this.cursor.messages.get(stopAfterPreVisit) !== true) {
                    if (t !== undefined) {
                        t = this.accept(t, p)
                    }
                    if (t !== undefined) {
                        t = this.postVisit(t, p)
                    }
                }
            }

            this.cursor = this.cursor.parent!;

            if (topLevel) {
                if (this.afterVisit) {
                    while (this.afterVisit.length > 0) {
                        const v = this.afterVisit.shift()!;
                        v.cursor = this.cursor;
                        if (t !== undefined) {
                            t = v.visit(t, p);
                        }
                    }
                }
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

    protected accept(t: T, p: P): T | undefined {
        return t
    }

    protected stopAfterPreVisit(): void {
        this.cursor.messages.set(stopAfterPreVisit, true);
    }

    isAcceptable(sourceFile: SourceFile, p: P): boolean {
        return true;
    }

    protected preVisit(tree: T, p: P): T | undefined {
        return tree;
    }

    protected postVisit(tree: T, p: P): T | undefined {
        return tree;
    }

    protected visitMarkers(markers: Markers, p: P): Markers {
        if (markers === undefined) {
            return emptyMarkers;
        } else if (markers === emptyMarkers) {
            return emptyMarkers;
        } else if ((markers.markers?.length || 0) === 0) {
            return markers;
        }
        return updateIfChanged(markers, {
            markers: mapSync(markers.markers, m => this.visitMarker(m, p))
        });
    }

    protected visitMarker<M extends Marker>(marker: M, p: P): M {
        return marker;
    }

    protected produceTree<T extends Tree>(
        before: T,
        p: P,
        recipe?: (draft: Draft<T>) => ValidRecipeReturnType<Draft<T>>
    ): T | undefined {
        // Visit markers separately to avoid Mutative drafting cyclic marker structures
        const newMarkers = this.visitMarkers(before.markers, p);

        if (recipe) {
            // Remove markers before Mutative drafting to avoid cycles, then restore after
            const withoutMarkers = { ...before, markers: emptyMarkers };
            const result = create(withoutMarkers, recipe as any) as T | undefined;
            if (result === undefined) {
                return undefined;
            }
            // Restore markers (use newMarkers since we visited them)
            return { ...result, markers: newMarkers } as T;
        }

        // No recipe - just update markers if changed
        return updateIfChanged(before, { markers: newMarkers } as Partial<T>);
    }
}

export abstract class AsyncTreeVisitor<T extends Tree, P> {
    readonly async = true as const;
    protected cursor: Cursor = rootCursor();
    private visitCount: number = 0;
    public afterVisit: (TreeVisitor<any, P> | AsyncTreeVisitor<any, P>)[] = [];

    async visitDefined<R extends T>(tree: Tree, p: P, parent?: Cursor): Promise<R> {
        return (await this.visit<R>(tree, p, parent))!;
    }

    async visit<R extends T>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
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
            await this.isAcceptable(tree, p));

        try {
            if (isAcceptable) {
                t = await this.preVisit(tree as T, p)
                if (this.cursor.messages.get(stopAfterPreVisit) !== true) {
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
                    while (this.afterVisit.length > 0) {
                        const v = this.afterVisit.shift()!;
                        // Type assertion needed because 'cursor' is protected in both types
                        (v as any).cursor = this.cursor;
                        if (t !== undefined) {
                            t = await v.visit(t, p);
                        }
                    }
                }
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

    protected stopAfterPreVisit(): void {
        this.cursor.messages.set(stopAfterPreVisit, true);
    }

    async isAcceptable(sourceFile: SourceFile, p: P): Promise<boolean> {
        return true;
    }

    protected async preVisit(tree: T, p: P): Promise<T | undefined> {
        return tree;
    }

    protected async postVisit(tree: T, p: P): Promise<T | undefined> {
        return tree;
    }

    protected async visitMarkers(markers: Markers, p: P): Promise<Markers> {
        if (markers === undefined) {
            return emptyMarkers;
        } else if (markers === emptyMarkers) {
            return emptyMarkers;
        } else if ((markers.markers?.length || 0) === 0) {
            return markers;
        }
        return (await produceAsync<Markers>(markers, async (draft) => {
            draft.markers = await mapAsync(markers.markers, m => this.visitMarker(m, p))
        }))!;
    }

    protected async visitMarker<M extends Marker>(marker: M, p: P): Promise<M> {
        return marker;
    }

    protected async produceTree<T extends Tree>(
        before: T,
        p: P,
        recipe?:
            ((draft: Draft<T>) => ValidRecipeReturnType<Draft<T>>) |
            ((draft: Draft<T>) => Promise<ValidRecipeReturnType<Draft<T>>>)
    ): Promise<T | undefined> {
        return produceAsync(before, async draft => {
            draft.markers = await this.visitMarkers(before.markers, p);
            if (recipe) {
                await recipe(draft);
            }
        });
    }
}

export function noopVisitor<T extends Tree, P>(): TreeVisitor<T, P> {
    return new class extends TreeVisitor<T, P> {
        visit<R extends Tree>(tree: Tree): R | undefined {
            return tree as unknown as R;
        }
    }
}

export class RecipeRunError extends Error {
    constructor(public readonly cause: Error, public readonly cursor?: Cursor) {
        super(cause.stack);
    }
}
