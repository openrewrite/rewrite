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
import {J, NameTree} from '../../java';
import {JavaScriptVisitor} from '../visitor';
import {isValueReference, scopeOf} from '../scope';

/**
 * Renames the identifiers a template uses for its declared bindings to the names the file
 * actually binds. Runs before parameter substitution, so only the template's own code is in
 * scope and a caller's captured code is never rewritten.
 */
export async function renameBindings<T extends J>(tree: T, renames: Record<string, string>): Promise<T> {
    return new RenameBindingsVisitor(renames).visit(tree, undefined) as Promise<T>;
}

class RenameBindingsVisitor extends JavaScriptVisitor<undefined> {
    constructor(private readonly renames: Record<string, string>) {
        super();
    }

    // The class of `Mocked<T>` and a decorator's name sit behind this hook, which the base visitor holds closed
    protected override async visitTypeName<N extends NameTree>(nameTree: N, p: undefined): Promise<N> {
        return await this.visit(nameTree, p) as N;
    }

    override async visitIdentifier(identifier: J.Identifier, p: undefined): Promise<J | undefined> {
        const renamed = this.renames[identifier.simpleName];
        if (renamed === undefined || renamed === identifier.simpleName) {
            return identifier;
        }

        // Only the context binds at module scope, so a reference nothing in the template rebinds reads it
        const refersToBinding = isValueReference(this.cursor, identifier) &&
            !scopeOf(this.cursor).declares(identifier.simpleName);

        return refersToBinding ? {...identifier, simpleName: renamed} as J.Identifier : identifier;
    }
}
