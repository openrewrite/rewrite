/*
 * Copyright 2026 the original author or authors.
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
import {Cursor} from '../..';
import {emptySpace, J} from '../../java';
import {findMarker} from '../../markers';
import {JS} from '../tree';
import {JavaScriptVisitor} from '../visitor';
import {compilationUnitOf, deconflict, namesDeclaredWithin, namesReferencedWithin, namesUsedIn, namesUsedWithin} from '../scope';
import {SubstitutedValue} from './placeholder-replacement';

/**
 * Renames each name the template's own code declares that a value substituted into it reads from
 * further out: `for (const item of ${xs}) ${f}(item)` with `f` bound to `x => x === item` would
 * otherwise read the template's `item` where the source read its own. The new name is spelled nowhere
 * in the template or the file it lands in.
 *
 * A name the replaced code declares itself is the template's to rebind: `for (const item of ${xs}) ${body}`
 * becoming `${xs}.forEach(item => ${body})` hands the body the `item` it read before.
 */
export async function renameCapturingDeclarations(tree: J, cursor: Cursor, replaced: J | undefined): Promise<J> {
    const values: J[] = [];
    const ownCode = await new class extends JavaScriptVisitor<undefined> {
        override async visit<R extends J>(t: any, p: undefined, parent?: Cursor): Promise<R | undefined> {
            if (findMarker(t, SubstitutedValue.KIND)) {
                values.push(t);
                return {kind: J.Kind.Empty, id: t.id, prefix: emptySpace, markers: t.markers} as J as R;
            }
            return super.visit(t, p, parent);
        }
    }().visit<J>(tree, undefined);
    if (values.length === 0 || ownCode === undefined) {
        return tree;
    }

    const declared = namesDeclaredWithin(ownCode);
    const captured = new Set<string>();
    for (const value of values) {
        for (const name of namesReferencedWithin(value)) {
            if (declared.has(name) && !(replaced && namesDeclaredWithin(replaced).has(name))) {
                captured.add(name);
            }
        }
    }
    const shorthand = shorthandNames(ownCode);
    const renames = new Map<string, string>();
    const cu = compilationUnitOf(cursor);
    const taken = (name: string) => namesUsedWithin(tree).has(name) || (cu !== undefined && namesUsedIn(cu).has(name)) ||
        [...renames.values()].includes(name);
    for (const name of captured) {
        // A shorthand property names its key with the reference, which a rename would change
        if (!shorthand.has(name)) {
            renames.set(name, deconflict(name, taken));
        }
    }
    return renames.size === 0 ? tree : (await new RenameOwnIdentifiers(renames).visit<J>(tree, undefined))!;
}

function shorthandNames(tree: J): Set<string> {
    const names = new Set<string>();
    new class extends JavaScriptVisitor<undefined> {
        protected override async visitPropertyAssignment(property: JS.PropertyAssignment, p: undefined): Promise<J | undefined> {
            if (property.initializer === undefined && property.name.element.kind === J.Kind.Identifier) {
                names.add((property.name.element as J.Identifier).simpleName);
            }
            return super.visitPropertyAssignment(property, p);
        }
    }().visit(tree, undefined);
    return names;
}

/** Renames the identifiers of the template's own code that read or declare a name, leaving its substituted values alone. */
class RenameOwnIdentifiers extends JavaScriptVisitor<undefined> {
    constructor(private readonly renames: ReadonlyMap<string, string>) {
        super();
    }

    override async visit<R extends J>(t: any, p: undefined, parent?: Cursor): Promise<R | undefined> {
        return findMarker(t, SubstitutedValue.KIND) ? t : super.visit(t, p, parent);
    }

    override async visitIdentifier(identifier: J.Identifier, p: undefined): Promise<J | undefined> {
        const renamed = this.renames.get(identifier.simpleName);
        if (renamed === undefined || this.namesMember(identifier)) {
            return identifier;
        }
        return {...identifier, simpleName: renamed} as J.Identifier;
    }

    /** Whether the identifier names a member rather than a binding: `a.item`, `a.item()` or `{item: …}`. */
    private namesMember(identifier: J.Identifier): boolean {
        const parent = this.cursor.parentTree()?.value as J | undefined;
        switch (parent?.kind) {
            case J.Kind.FieldAccess:
                return (parent as J.FieldAccess).name.element === identifier;
            case J.Kind.MethodInvocation:
                return (parent as J.MethodInvocation).select !== undefined && (parent as J.MethodInvocation).name === identifier;
            case JS.Kind.PropertyAssignment:
                return (parent as JS.PropertyAssignment).name.element === identifier;
            case J.Kind.MethodDeclaration:
                return (parent as J.MethodDeclaration).name === identifier &&
                    this.cursor.parentTree()?.parentTree()?.value?.kind === J.Kind.Block &&
                    this.cursor.parentTree()?.parentTree()?.parentTree()?.value?.kind === J.Kind.ClassDeclaration;
            default:
                return false;
        }
    }
}
