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
import {ModuleBinding} from './types';

/**
 * Whether the template's dependencies bring a workspace that could resolve `module`.
 */
export function isResolvable(module: string, dependencies: Record<string, string>): boolean {
    const segments = module.split('/');
    const pkg = module.startsWith('@') ? segments.slice(0, 2).join('/') : segments[0];
    return Object.prototype.hasOwnProperty.call(dependencies, pkg);
}

/**
 * What a declared binding's name is parsed against, ahead of the template and so out of its output.
 * An import is what carries attribution, and costs a module resolution to get it; a declaration
 * names the binding for a module no workspace could have resolved anyway.
 */
export function bindingContextStatement(name: string, binding: ModuleBinding, dependencies: Record<string, string>): string {
    if (!isResolvable(binding.module, dependencies)) {
        return binding.typeOnly ? `type ${name} = any;` : `declare const ${name}: any;`;
    }
    const type = binding.typeOnly ? 'type ' : '';
    if (binding.member === '*') {
        return `import ${type}* as ${name} from '${binding.module}';`;
    }
    if (binding.member === undefined || binding.member === 'default') {
        return `import ${type}${name} from '${binding.module}';`;
    }
    const specifier = binding.member === name ? name : `${binding.member} as ${name}`;
    return `import ${type}{${specifier}} from '${binding.module}';`;
}

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

/** An identifier in its parent's `name` slot is being named — a property, a method, a declaration. */
function namesItsParent(cursor: Cursor, identifier: J.Identifier): boolean {
    let c: Cursor | undefined = cursor.parent;
    while (c && isPadding(c.value)) {
        c = c.parent;
    }
    const name = (c?.value as { name?: unknown } | undefined)?.name;
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
