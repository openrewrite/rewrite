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
import {create as produce, Draft} from "mutative";
import {ElementRemovalFormatter, emptySpace, Expression, isIdentifier, isStringLiteral, J, singleSpace, Statement} from "../java";
import {randomId, UUID} from "../uuid";
import {Cursor} from "../tree";
import {isObjectBindingPattern, JS, moduleExportName} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {resolve, rootName} from "./scope";
import {aliasing, moduleNameOf, movedBinding, newIdentifier, respecify, withMovedTypes, withoutStatement} from "./add-import";
import {sameModule, spelledLike} from "./package-name";

/**
 * Renames a moved member where the file reads it through its module rather than an import of it.
 * `NS` is a module object: `import * as NS from "m"`, `import NS = require("m")`, `const NS = require("m")`
 * or `const NS = await import("m")`.
 *
 *     // m.Old → m.New
 *     NS.Old()                     →  NS.New()
 *     NS["Old"]                    →  NS["New"]
 *     const {Old} = NS             →  const {New: Old} = NS
 *     import("m").Old              →  import("m").New
 *     export {Old} from "m"        →  export {New as Old} from "m"
 *
 *     // m.Old → m2.New: NS still reads "m", so NS.Old stays
 *     import("m").Old              →  import("m2").New
 *     export {Old, X} from "m"     →  export {X} from "m"; export {New as Old} from "m2"
 */
export class RenameMemberSpellings<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly from: {module: string; member: string},
        readonly to: {module: string; member: string},
        /** The module objects of `from`'s module, like `NS` in `import * as NS from "m"`. */
        readonly moduleObjects: ReadonlySet<string>
    ) {
        super();
    }

    /**
     * Moves re-exports to another module on the whole file, since `export {Old, X} from "m"` can split in two.
     * Renames the types only in a file this changed. Where a `RebindImport` of the same move ran first, it already
     * renamed them, so nothing is left to rename.
     */
    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const visited = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;
        const moved = this.withinModule ? visited : this.withReExportsMoved(visited);
        return moved === cu ? moved : withMovedTypes(moved, this.from, this.to);
    }

    private get withinModule(): boolean {
        return sameModule(this.from.module, this.to.module);
    }

    /** Whether `read` goes through one of the module objects of `from`'s module, as the file's top level binds it. */
    private throughModuleObject(read: ObjectRead | undefined): boolean {
        return this.withinModule && read !== undefined && this.moduleObjects.has(read.object.simpleName) &&
            resolve(read.at, read.object)?.kind === JS.Kind.CompilationUnit;
    }

    /** Whether `node` reads the moved member through a module object: `NS.Old`, `(NS).Old`, `NS["Old"]`, `NS.Old()`. */
    private readsMovedMember(node: J): boolean {
        const read = memberRead(node, this.cursor);
        return read?.member === this.from.member && this.throughModuleObject(read);
    }

    /** `NS.Old` → `NS.New` */
    override async visitFieldAccess(fieldAccess: J.FieldAccess, p: P): Promise<J | undefined> {
        const visited = await super.visitFieldAccess(fieldAccess, p);
        if (visited?.kind !== J.Kind.FieldAccess || !this.readsMovedMember(fieldAccess)) {
            return visited;
        }
        return produce(visited as J.FieldAccess, draft => {
            draft.name.element.simpleName = this.to.member;
        });
    }

    /** `NS.Old()` → `NS.New()` */
    override async visitMethodInvocation(method: J.MethodInvocation, p: P): Promise<J | undefined> {
        const visited = await super.visitMethodInvocation(method, p);
        if (visited?.kind !== J.Kind.MethodInvocation || !this.readsMovedMember(method)) {
            return visited;
        }
        return produce(visited as J.MethodInvocation, draft => {
            draft.name.simpleName = this.to.member;
            if (draft.methodType) {
                draft.methodType = {...draft.methodType, name: this.to.member};
            }
        });
    }

    /** `NS["Old"]` → `NS["New"]` */
    override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: P): Promise<J | undefined> {
        const visited = await super.visitArrayAccess(arrayAccess, p);
        if (visited?.kind !== J.Kind.ArrayAccess || !this.readsMovedMember(arrayAccess)) {
            return visited;
        }
        return produce(visited as J.ArrayAccess, draft => {
            respecify(draft.dimension.index.element as Draft<J.Literal>, this.to.member);
        });
    }

    /** `const {Old} = NS` → `const {New: Old} = NS`, so the local name stays `Old`. */
    override async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: P): Promise<J | undefined> {
        const visited = await super.visitVariable(variable, p);
        if (visited?.kind !== J.Kind.NamedVariable) {
            return visited;
        }
        const named = visited as J.VariableDeclarations.NamedVariable;
        const pattern = named.name;
        if (!isObjectBindingPattern(pattern) || !this.throughModuleObject(objectRead(variable.initializer?.element, this.cursor))) {
            return visited;
        }
        const elements = pattern.bindings.elements.map((e, i) => {
            const element = e.element as JS.BindingElement;
            const read = element.kind === JS.Kind.BindingElement ? element.propertyName?.element ?? element.name : undefined;
            return read !== undefined && isIdentifier(read) && read.simpleName === this.from.member && isIdentifier(element.name)
                ? {...e, element: movedBinding(pattern, i, this.to.member, element.name.simpleName)} : e;
        });
        if (elements.every((e, i) => e === pattern.bindings.elements[i])) {
            return visited;
        }
        const destructured: JS.ObjectBindingPattern = {...pattern, bindings: {...pattern.bindings, elements}};
        const moved: J.VariableDeclarations.NamedVariable = {...named, name: destructured};
        return moved;
    }

    /** `import("m").Old` → `import("m2").New` */
    override async visitImportType(importType: JS.ImportType, p: P): Promise<J | undefined> {
        const visited = await super.visitImportType(importType, p);
        if (visited?.kind !== JS.Kind.ImportType) {
            return visited;
        }
        const named = importTypeMember(importType);
        if (named === undefined || !sameModule(named.module, this.from.module) || named.member !== this.from.member) {
            return visited;
        }
        return produce(visited as JS.ImportType, draft => {
            (rootName(draft.qualifier!.element) as Draft<J.Identifier>).simpleName = this.to.member;
            if (!this.withinModule) {
                const spelled = (draft.argumentAndAttributes.elements[0].element as Draft<JS.LiteralType>).literal as Draft<J.Literal>;
                respecify(spelled, spelledLike(this.to.module, String(spelled.value)));
            }
        });
    }

    /** Within one module: `export {Old} from "m"` → `export {New as Old} from "m"`. */
    override async visitExportDeclaration(exportDeclaration: JS.ExportDeclaration, p: P): Promise<J | undefined> {
        const visited = await super.visitExportDeclaration(exportDeclaration, p);
        const declaration = reExportFrom(visited, this.from.module);
        if (!this.withinModule || declaration === undefined) {
            return visited;
        }
        const elements = clauseOf(declaration).elements.elements;
        return elements.some(e => this.reExportsMember(e.element))
            ? withElements(declaration, elements.map(e => this.reExportsMember(e.element) ? this.renamed(e) : e))
            : visited;
    }

    /**
     * Moves the member's re-exports to `to`'s module:
     *
     *     export {Old} from "m"       →  export {New as Old} from "m2"
     *     export {Old, X} from "m"    →  export {X} from "m"; export {New as Old} from "m2"
     *     export {Old} from "m";      →  export {Y, New as Old} from "m2"
     *     export {Y} from "m2"           (joins a re-export of the same kind, `export type` or not)
     *
     * A comment ending the line (`export {X} from "m"; // note`) stays on it, before the new statement.
     */
    private withReExportsMoved(cu: JS.CompilationUnit): JS.CompilationUnit {
        const statements = [...cu.statements];
        const emptied: UUID[] = [];
        let eof = cu.eof;
        let changed = false;
        for (let i = 0; i < statements.length; i++) {
            const declaration = reExportFrom(statements[i].element, this.from.module);
            const elements = declaration && clauseOf(declaration).elements.elements;
            const moving = new Set(elements?.filter(e => this.reExportsMember(e.element)));
            if (declaration === undefined || elements === undefined || moving.size === 0) {
                continue;
            }
            changed = true;
            const staying = new Set(elements.filter(e => !moving.has(e)));
            const target = statements.findIndex((s, j) =>
                j !== i && reExportFrom(s.element, this.to.module)?.typeOnly.element === declaration.typeOnly.element);
            if (target < 0 && staying.size === 0) {
                statements[i] = {...statements[i], element: this.respecified(withElements(declaration, elements.map(e => this.renamed(e))))};
                continue;
            }
            const moved = without(elements, staying).map(e => this.renamed(e));
            if (target >= 0) {
                const into = statements[target].element as JS.ExportDeclaration;
                statements[target] = {...statements[target], element: withElements(into, appended(clauseOf(into).elements.elements, moved))};
            } else {
                const next = statements[i + 1];
                const split = splitAtLineEnd(next?.element.prefix ?? eof, next === undefined);
                if (split !== undefined && next !== undefined) {
                    statements[i + 1] = {...next, element: {...next.element, prefix: split.rest}};
                } else if (split !== undefined) {
                    eof = split.rest;
                }
                statements.splice(i + 1, 0, this.ownStatement(statements[i], declaration, moved, split?.line));
            }
            if (staying.size > 0) {
                statements[i] = {...statements[i], element: withElements(declaration, without(elements, moving))};
            } else {
                emptied.push(declaration.id);
            }
        }
        return changed ? emptied.reduce((left, id) => withoutStatement(left, id), {...cu, statements, eof}) : cu;
    }

    /** A new `export {…} from "m2"` for `moved` on the next line, after `line`: the comments ending this one. */
    private ownStatement(stmt: J.RightPadded<Statement>, declaration: JS.ExportDeclaration,
                         moved: J.RightPadded<Expression>[], line: J.Space | undefined): J.RightPadded<Statement> {
        const indent = declaration.prefix.whitespace.slice(declaration.prefix.whitespace.lastIndexOf("\n") + 1);
        const clause = clauseOf(declaration);
        const own = this.respecified({
            ...declaration,
            id: randomId(),
            prefix: line === undefined ? {...declaration.prefix, comments: [], whitespace: "\n" + indent} :
                {...line, comments: line.comments.map((c, k) => k < line.comments.length - 1 ? c : {...c, suffix: "\n" + indent})},
            exportClause: {...clause, id: randomId(), elements: {...clause.elements, elements: moved}} as JS.NamedExports
        });
        return {...stmt, element: own, markers: {...stmt.markers, id: randomId()}};
    }

    /** `export … from "m"` → `export … from "m2"`, keeping a `node:` scheme the file uses. */
    private respecified(declaration: JS.ExportDeclaration): JS.ExportDeclaration {
        return produce(declaration, draft => {
            const literal = draft.moduleSpecifier!.element as Draft<J.Literal>;
            literal.id = randomId();
            respecify(literal, spelledLike(this.to.module, String(literal.value)));
        });
    }

    /**
     * The specifier reading the new member, still exporting the same name: `Old` → `New as Old`,
     * and `Old as X` or `"Old" as X` → `New as X`. Unchanged where only the module moves.
     * `New` is typed as the specifier is, which the type rename then follows.
     */
    private renamed(element: J.RightPadded<Expression>): J.RightPadded<Expression> {
        if (this.to.member === this.from.member || element.element.kind !== JS.Kind.ExportSpecifier) {
            return element;
        }
        const specifier = element.element as JS.ExportSpecifier;
        const read = specifier.specifier;
        const member: J.Identifier = {...newIdentifier(this.to.member), type: specifier.type};
        let renamed: JS.Alias;
        if (read.kind === JS.Kind.Alias) {
            const alias = read as JS.Alias;
            renamed = {...alias, propertyName: {...alias.propertyName, element: {...member, prefix: alias.propertyName.element.prefix}}};
        } else if (read.kind === J.Kind.Identifier || read.kind === J.Kind.Literal) {
            const alias = aliasing(read as J.Identifier | J.Literal, this.to.member);
            renamed = {...alias, propertyName: {...alias.propertyName, element: member}};
        } else {
            return element;
        }
        const reading: JS.ExportSpecifier = {...specifier, specifier: renamed};
        return {...element, element: reading};
    }

    /** Whether the entry is `Old`, `Old as X` or `"Old" as X`. */
    private reExportsMember(element: J): boolean {
        const read = reExportedName(element);
        return read !== undefined && moduleExportName(read) === this.from.member;
    }
}

/** A name read through an object, and the cursor to resolve the object from. */
export interface ObjectRead {
    object: J.Identifier;
    at: Cursor;
}

/**
 * The object `qualifier` names, where `cursor` stands on the node holding it: `NS` → NS, `(NS)` → NS.
 * For `(NS)` the cursor steps inside, since `resolve` wants the cursor of the node holding `NS`.
 */
export function objectRead(qualifier: J | undefined, cursor: Cursor): ObjectRead | undefined {
    let object = qualifier, at = cursor;
    while (object?.kind === J.Kind.Parentheses) {
        at = new Cursor(object, at);
        object = (object as J.Parentheses<J>).tree.element;
    }
    return object !== undefined && isIdentifier(object) ? {object, at} : undefined;
}

/**
 * The member `node` reads through an object, where `cursor` stands on `node`:
 * `NS.X`, `(NS).X`, `NS["X"]` and `NS.X()` → `{object: NS, member: "X"}`.
 */
export function memberRead(node: J, cursor: Cursor): (ObjectRead & {member: string}) | undefined {
    let qualifier: J | undefined, member: string | undefined;
    if (node.kind === J.Kind.FieldAccess) {
        [qualifier, member] = [(node as J.FieldAccess).target, (node as J.FieldAccess).name.element.simpleName];
    } else if (node.kind === J.Kind.MethodInvocation) {
        [qualifier, member] = [(node as J.MethodInvocation).select?.element, (node as J.MethodInvocation).name.simpleName];
    } else if (node.kind === J.Kind.ArrayAccess) {
        const index = (node as J.ArrayAccess).dimension.index.element;
        [qualifier, member] = [(node as J.ArrayAccess).indexed, isStringLiteral(index) ? index.value : undefined];
    }
    const read = member === undefined ? undefined : objectRead(qualifier, cursor);
    return read && {...read, member: member!};
}

/** The member an `import()` type names, which no import binds: `import("m").X.Y` → `{module: "m", member: "X"}`. */
export function importTypeMember(importType: JS.ImportType): {module: string, member: string} | undefined {
    const argument = importType.argumentAndAttributes.elements[0]?.element;
    const literal = argument?.kind === JS.Kind.LiteralType ? (argument as JS.LiteralType).literal : undefined;
    const member = rootName(importType.qualifier?.element);
    return isStringLiteral(literal) && member !== undefined && isIdentifier(member)
        ? {module: moduleNameOf(literal), member: member.simpleName} : undefined;
}

/** `statement` as an `export {…} from "m"` declaration, with the module it re-exports from. */
export function reExportDeclaration(statement: J | undefined): {declaration: JS.ExportDeclaration, module: string} | undefined {
    if (statement?.kind !== JS.Kind.ExportDeclaration) {
        return undefined;
    }
    const declaration = statement as JS.ExportDeclaration;
    const source = declaration.moduleSpecifier?.element;
    return isStringLiteral(source) && declaration.exportClause?.kind === JS.Kind.NamedExports
        ? {declaration, module: moduleNameOf(source)} : undefined;
}

/** The name an entry of `export {…} from "m"` reads from `m`: `X` in `X`, `X as Y` and `"X" as Y`. */
export function reExportedName(element: J): J.Identifier | J.Literal | undefined {
    if (element.kind !== JS.Kind.ExportSpecifier) {
        return undefined;
    }
    const specifier = (element as JS.ExportSpecifier).specifier;
    const read = specifier.kind === JS.Kind.Alias ? (specifier as JS.Alias).propertyName.element : specifier;
    return read.kind === J.Kind.Identifier || read.kind === J.Kind.Literal ? read as J.Identifier | J.Literal : undefined;
}

/** `statement` as an `export {…} from "<module>"` declaration, where it is one. */
function reExportFrom(statement: J | undefined, module: string): JS.ExportDeclaration | undefined {
    const found = reExportDeclaration(statement);
    return found !== undefined && sameModule(found.module, module) ? found.declaration : undefined;
}

/**
 * The space after a statement, split where its line ends:
 * ` // note\n// next` → `line` ` // note`, `rest` `\n// next`. At the end of the file all of it is `line`.
 * `undefined` where no comment follows on the same line.
 */
function splitAtLineEnd(space: J.Space, endsFile: boolean): {line: J.Space, rest: J.Space} | undefined {
    if (space.whitespace.includes("\n") || space.comments.length === 0) {
        return undefined;
    }
    const end = space.comments.findIndex(c => c.suffix.includes("\n"));
    if (end < 0) {
        return endsFile ? {line: space, rest: {...space, whitespace: "", comments: []}} : undefined;
    }
    return {
        line: {...space, comments: space.comments.slice(0, end + 1)},
        rest: {...space, whitespace: space.comments[end].suffix, comments: space.comments.slice(end + 1)}
    };
}

function clauseOf(declaration: JS.ExportDeclaration): JS.NamedExports {
    return declaration.exportClause as JS.NamedExports;
}

function withElements(declaration: JS.ExportDeclaration, elements: J.RightPadded<Expression>[]): JS.ExportDeclaration {
    const clause = clauseOf(declaration);
    return {...declaration, exportClause: {...clause, elements: {...clause.elements, elements}} as JS.NamedExports};
}

/** `elements` without `dropped`, each keeping its comments: `{ a, b }` without `a` is `{ b }`. */
function without<T extends J>(elements: J.RightPadded<T>[], dropped: ReadonlySet<J.RightPadded<T>>): J.RightPadded<T>[] {
    const formatter = new ElementRemovalFormatter<T>();
    const kept: J.RightPadded<T>[] = [];
    for (const e of elements) {
        if (dropped.has(e)) {
            formatter.markRemoved(e.element);
        } else {
            kept.push({...e, element: formatter.processKept(e.element)});
        }
    }
    const last = elements[elements.length - 1];
    kept[kept.length - 1] = {...kept[kept.length - 1], after: last.after, markers: last.markers};
    return kept;
}

/**
 * `elements` with `added` at the end, separated the same way and each keeping its comments:
 * `{a, b,}` + `c` → `{a, b, c,}`, and `{}` + `c` → `{c}`.
 */
function appended<T extends J>(elements: J.RightPadded<T>[], added: J.RightPadded<T>[]): J.RightPadded<T>[] {
    const last = elements[elements.length - 1];
    const plain = (markers: J.RightPadded<T>["markers"]) =>
        ({...markers, markers: markers.markers.filter(m => m.kind !== J.Markers.TrailingComma)});
    if (elements.length === 1 && last.element.kind === J.Kind.Empty) {
        return added.map((e, i) => ({
            ...e,
            element: {...e.element, prefix: {...e.element.prefix, whitespace: i === 0 ? last.element.prefix.whitespace : " "}},
            ...(i === added.length - 1 ? {after: last.after, markers: last.markers} : {after: emptySpace, markers: plain(e.markers)})
        }));
    }
    const separator = (elements[1]?.element.prefix ??
        (elements[0].element.prefix.whitespace.includes("\n") ? elements[0].element.prefix : singleSpace)).whitespace;
    const separated = (e: J.RightPadded<T>) => ({...e.element, prefix: {...e.element.prefix, whitespace: separator}});
    return [
        ...elements.slice(0, -1),
        {...last, after: emptySpace, markers: plain(last.markers)},
        ...added.map((e, i) => i === added.length - 1
            ? {...e, element: separated(e), after: last.after, markers: last.markers}
            : {...e, element: separated(e), after: emptySpace, markers: plain(e.markers)})
    ];
}
