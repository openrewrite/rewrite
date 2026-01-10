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
import {AsyncTreeVisitor, noopVisitor, TreeVisitor} from './visitor';
import {Cursor, isSourceFile, SourceFile, Tree} from './tree';
import {Recipe, RecipeVisitor} from "./recipe";
import {ExecutionContext} from "./execution";

export async function check<T extends Tree>(
    checkCondition: Recipe | Promise<Recipe> | RecipeVisitor | Promise<RecipeVisitor> | boolean,
    v: RecipeVisitor | Promise<RecipeVisitor>
): Promise<RecipeVisitor> {
    const resolvedCheck = await checkCondition;
    const resolvedV = await v;

    if (typeof resolvedCheck === 'boolean') {
        return resolvedCheck ? resolvedV : noopVisitor<T, ExecutionContext>();
    }
    return new Check(resolvedCheck, resolvedV);
}

export class Check<T extends Tree> extends AsyncTreeVisitor<T, ExecutionContext> {
    constructor(
        readonly check: RecipeVisitor | Recipe,
        readonly v: RecipeVisitor
    ) {
        super();
    }

    async isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Promise<boolean> {
        return await (await this.checkVisitor()).isAcceptable(sourceFile, ctx) &&
            await this.v.isAcceptable(sourceFile, ctx);
    }

    async visit<R extends T>(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
        // if tree isn't an instanceof of SourceFile, then a precondition visitor may
        // not be able to do its work because it may assume we are starting from the root level
        if (!isSourceFile(tree)) {
            return parent !== undefined
                ? await this.v.visit(tree, ctx, parent)
                : await this.v.visit(tree, ctx);
        }

        const checkResult = parent !== undefined
            ? await (await this.checkVisitor()).visit(tree, ctx, parent)
            : await (await this.checkVisitor()).visit(tree, ctx);

        // If check visitor modified the tree (returned something different), run the main visitor
        if (checkResult !== (tree as unknown as T)) {
            return parent !== undefined
                ? await this.v.visit(tree, ctx, parent)
                : await this.v.visit(tree, ctx);
        }

        return tree as unknown as R;
    }

    private async checkVisitor(): Promise<RecipeVisitor> {
        return this.check instanceof Recipe ? this.check.editor() : this.check;
    }
}
