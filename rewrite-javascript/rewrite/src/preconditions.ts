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
import {noopVisitor, TreeVisitor} from './visitor';
import {Cursor, isSourceFile, SourceFile, Tree} from './tree';

export function check<T extends Tree, P>(
    checkCondition: TreeVisitor<T, P> | boolean,
    v: TreeVisitor<T, P>
): TreeVisitor<T, P> {
    if (typeof checkCondition === 'boolean') {
        return checkCondition ? v : noopVisitor<T, P>();
    }
    return new Check(checkCondition, v);
}

class Check<T extends Tree, P> extends TreeVisitor<T, P> {
    constructor(
        private readonly check: TreeVisitor<T, P>,
        private readonly v: TreeVisitor<T, P>
    ) {
        super();
    }

    async isAcceptable(sourceFile: SourceFile, p: P): Promise<boolean> {
        return await this.check.isAcceptable(sourceFile, p) &&
            await this.v.isAcceptable(sourceFile, p);
    }

    async visit<R extends T>(tree: Tree, p: P, parent?: Cursor): Promise<R | undefined> {
        // if tree isn't an instanceof of SourceFile, then a precondition visitor may
        // not be able to do its work because it may assume we are starting from the root level
        if (!isSourceFile(tree)) {
            return parent !== undefined
                ? this.v.visit<R>(tree, p, parent)
                : this.v.visit<R>(tree, p);
        }

        const checkResult = parent !== undefined
            ? await this.check.visit(tree, p, parent)
            : await this.check.visit(tree, p);

        // If check visitor modified the tree (returned something different), run the main visitor
        if (checkResult !== (tree as unknown as T)) {
            return parent !== undefined
                ? this.v.visit<R>(tree, p, parent)
                : this.v.visit<R>(tree, p);
        }

        return tree as unknown as R;
    }
}
