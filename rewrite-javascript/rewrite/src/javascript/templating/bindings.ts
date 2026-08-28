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
import {J, Type} from '../../java';
import {JavaScriptVisitor} from '../visitor';
import {Cursor} from '../../tree';

/**
 * Renames the identifiers a template uses for its declared bindings to the names the file
 * actually binds. Runs before parameter substitution, so only the template's own code is in
 * scope and a caller's captured code is never rewritten.
 */
export async function renameBindings<T extends J>(tree: T, renames: Record<string, string>, modules: Record<string, string>): Promise<T> {
    return new RenameBindingsVisitor(renames, modules).visit(tree, undefined) as Promise<T>;
}

class RenameBindingsVisitor extends JavaScriptVisitor<undefined> {
    constructor(private readonly renames: Record<string, string>,
                private readonly modules: Record<string, string>) {
        super();
    }

    override async visitIdentifier(identifier: J.Identifier, p: undefined): Promise<J | undefined> {
        const renamed = this.renames[identifier.simpleName];
        if (renamed === undefined || renamed === identifier.simpleName) {
            return identifier;
        }

        // Attribution settles it where the context import resolved. It is absent for a module the
        // parse could not reach, and the identifier's position decides instead.
        const resolved = resolvedModule(identifier);
        const refersToBinding = resolved !== undefined
            ? resolved === this.modules[identifier.simpleName]
            : !namesItsParent(this.cursor, identifier);

        return refersToBinding ? {...identifier, simpleName: renamed} as J.Identifier : identifier;
    }
}

/** Whether the parent is naming this identifier — as a property, a method, a declaration — rather than referencing it. */
function namesItsParent(cursor: Cursor, identifier: J.Identifier): boolean {
    let c: Cursor | undefined = cursor.parent;
    while (c && isPadding(c.value)) {
        c = c.parent;
    }
    const parent = c?.value as { kind?: string; name?: unknown; select?: unknown } | undefined;

    // A call names a member of whatever it selects from. With nothing selected there is no member,
    // and its `name` is a reference to the function being called.
    if (parent?.kind === J.Kind.MethodInvocation && !parent.select) {
        return false;
    }

    const name = parent?.name;
    return name === identifier || (name as { element?: unknown } | undefined)?.element === identifier;
}

function isPadding(value: unknown): boolean {
    const kind = (value as { kind?: string } | undefined)?.kind;
    return kind === J.Kind.RightPadded || kind === J.Kind.LeftPadded || kind === J.Kind.Container;
}

/** The module an identifier's attribution traces back to, following the owning-class chain to its root. */
function resolvedModule(identifier: J.Identifier): string | undefined {
    const fieldType = identifier.fieldType;
    if (fieldType?.kind === Type.Kind.Variable) {
        const owner = (fieldType as Type.Variable).owner;
        return owner && Type.isClass(owner) ? rootName(owner as Type.Class) : undefined;
    }
    const type = identifier.type;
    if (type && Type.isMethod(type)) {
        const declaring = (type as Type.Method).declaringType;
        return declaring ? rootName(declaring as Type.Class) : undefined;
    }
    if (type && Type.isClass(type)) {
        return rootName(type as Type.Class);
    }
    return undefined;
}

function rootName(classType: Type.Class): string {
    let current: Type.Class = classType;
    while (current.owningClass && Type.isClass(current.owningClass)) {
        current = current.owningClass as Type.Class;
    }
    return Type.FullyQualified.getFullyQualifiedName(current);
}
