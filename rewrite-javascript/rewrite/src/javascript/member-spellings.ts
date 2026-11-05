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
import {emptySpace, Expression, isIdentifier, J, singleSpace, Statement} from "../java";
import {randomId} from "../uuid";
import {JS, moduleExportName} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {resolve, rootName} from "./scope";
import {aliasing, moduleNameOf, newIdentifier, respecify, withMovedTypes} from "./add-import";
import {sameModule, spelledLike} from "./package-name";

/**
 * The spellings of a member that name it through its module rather than through a binding of it,
 * renamed when `maybeRebind` moves the member: `NS.Old` where `NS` is a module object the file binds
 * at module scope (`import * as NS`, `import NS = require`, `const NS = require`), `import("m").Old`,
 * and `export {Old} from "m"`, which keeps the name it publishes as `export {New as Old} from "m"`.
 *
 * A module object still names `from`'s module after the move, so `NS.Old` moves only within one module.
 * An `import()` type spells its module, which moves with it. So does a re-export: one that re-exports
 * something else too leaves that behind, and joins a re-export of `to` of the same kind (`export type`
 * or not) or takes a statement of its own right after the one it left.
 */
export class RenameMemberSpellings<P> extends JavaScriptVisitor<P> {
    constructor(
        readonly from: {module: string; member: string},
        readonly to: {module: string; member: string},
        /** The names the file binds `from`'s module object under at module scope. */
        readonly moduleObjects: ReadonlySet<string>,
        /** Whether a renamed spelling renames the file's types too, which a rebind of a binding does itself. */
        readonly retypes: boolean
    ) {
        super();
    }

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: P): Promise<J | undefined> {
        const visited = await super.visitJsCompilationUnit(cu, p);
        if (visited?.kind !== JS.Kind.CompilationUnit) {
            return visited;
        }
        // A re-export moving to another module may leave its statement or join another, so it moves here
        const moved = this.withinModule ? visited as JS.CompilationUnit : this.withReExportsMoved(visited as JS.CompilationUnit);
        // Only a spelling renamed here moves the types, so a file left as it was keeps them as they were
        return moved === cu || !this.retypes ? moved : withMovedTypes(moved, this.from, this.to);
    }

    private get withinModule(): boolean {
        return sameModule(this.from.module, this.to.module);
    }

    /** Whether `qualifier.name`, at the cursor, reads the moved member through one of the module objects. */
    private readsMovedMember(qualifier: J | undefined, name: J.Identifier): boolean {
        return this.withinModule && name.simpleName === this.from.member &&
            qualifier !== undefined && isIdentifier(qualifier) && this.moduleObjects.has(qualifier.simpleName) &&
            resolve(this.cursor, qualifier)?.kind === JS.Kind.CompilationUnit;
    }

    override async visitFieldAccess(fieldAccess: J.FieldAccess, p: P): Promise<J | undefined> {
        const visited = await super.visitFieldAccess(fieldAccess, p);
        if (visited?.kind !== J.Kind.FieldAccess || !this.readsMovedMember(fieldAccess.target, fieldAccess.name.element)) {
            return visited;
        }
        return produce(visited as J.FieldAccess, draft => {
            draft.name.element.simpleName = this.to.member;
        });
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: P): Promise<J | undefined> {
        const visited = await super.visitMethodInvocation(method, p);
        if (visited?.kind !== J.Kind.MethodInvocation || !this.readsMovedMember(method.select?.element, method.name)) {
            return visited;
        }
        return produce(visited as J.MethodInvocation, draft => {
            draft.name.simpleName = this.to.member;
            if (draft.methodType) {
                draft.methodType = {...draft.methodType, name: this.to.member};
            }
        });
    }

    override async visitImportType(importType: JS.ImportType, p: P): Promise<J | undefined> {
        const visited = await super.visitImportType(importType, p);
        if (visited?.kind !== JS.Kind.ImportType) {
            return visited;
        }
        const argument = importType.argumentAndAttributes.elements[0]?.element;
        const literal = argument?.kind === JS.Kind.LiteralType ? (argument as JS.LiteralType).literal : undefined;
        const qualifier = rootName(importType.qualifier?.element);
        if (literal?.kind !== J.Kind.Literal || typeof (literal as J.Literal).value !== "string" ||
            !sameModule(moduleNameOf(literal as J.Literal), this.from.module) ||
            qualifier === undefined || !isIdentifier(qualifier) || qualifier.simpleName !== this.from.member) {
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
     * Moves the member's re-exports from `from`'s module to `to`'s. A specifier leaves a statement that
     * re-exports something else, joins a re-export of `to` of the same kind where the file has one, and
     * otherwise takes a statement of its own right after the one it left. A statement it empties goes.
     */
    private withReExportsMoved(cu: JS.CompilationUnit): JS.CompilationUnit {
        const statements = [...cu.statements];
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
                // Alone in its statement with nothing to join: the statement moves as it is
                statements[i] = {...statements[i], element: this.respecified(withElements(declaration, elements.map(e => this.renamed(e))))};
                continue;
            }
            const moved = without(elements, staying).map(e => this.renamed(e));
            if (target >= 0) {
                const into = statements[target].element as JS.ExportDeclaration;
                statements[target] = {...statements[target], element: withElements(into, appended(clauseOf(into).elements.elements, moved))};
            } else {
                statements.splice(i + 1, 0, this.ownStatement(statements[i], declaration, moved));
            }
            if (staying.size > 0) {
                statements[i] = {...statements[i], element: withElements(declaration, without(elements, moving))};
            } else {
                // The statement after takes the place, and the layout, of the one that goes
                const next = statements[i + 1];
                if (next !== undefined) {
                    statements[i + 1] = {...next, element: {...next.element, prefix: declaration.prefix}};
                }
                statements.splice(i--, 1);
            }
        }
        return changed ? {...cu, statements} : cu;
    }

    /** A statement re-exporting `moved` from `to`, laid out on the line after the one it came from. */
    private ownStatement(stmt: J.RightPadded<Statement>, declaration: JS.ExportDeclaration,
                         moved: J.RightPadded<Expression>[]): J.RightPadded<Statement> {
        const indent = declaration.prefix.whitespace.slice(declaration.prefix.whitespace.lastIndexOf("\n") + 1);
        const clause = clauseOf(declaration);
        const own = this.respecified({
            ...declaration,
            id: randomId(),
            prefix: {...declaration.prefix, comments: [], whitespace: "\n" + indent},
            exportClause: {...clause, id: randomId(), elements: {...clause.elements, elements: moved}} as JS.NamedExports
        });
        return {...stmt, element: own, markers: {...stmt.markers, id: randomId()}};
    }

    /** `declaration` re-exporting from `to`'s module, its specifier spelled the way the file spells modules. */
    private respecified(declaration: JS.ExportDeclaration): JS.ExportDeclaration {
        return produce(declaration, draft => {
            const literal = draft.moduleSpecifier!.element as Draft<J.Literal>;
            literal.id = randomId();
            respecify(literal, spelledLike(this.to.module, String(literal.value)));
        });
    }

    /** The specifier reading `to.member` where it read `from.member`, still publishing the name it published. */
    private renamed(element: J.RightPadded<Expression>): J.RightPadded<Expression> {
        // A member keeping its name keeps its specifier as written; only the module moves
        if (this.to.member === this.from.member) {
            return element;
        }
        return produce(element, draft => {
            const specifier = draft.element as Draft<JS.ExportSpecifier>;
            if (specifier.specifier.kind === JS.Kind.Alias) {
                // `export {Old as X}`: only the member it reads changes, X is the module's own name for it.
                // A fresh identifier, since `export {"Old" as X}` spells the member as a string literal.
                const name = (specifier.specifier as Draft<JS.Alias>).propertyName;
                name.element = {...newIdentifier(this.to.member), prefix: name.element.prefix} as Draft<J.Identifier>;
            } else {
                // `export {Old}` keeps publishing `Old`, now as `export {New as Old}`
                specifier.specifier = aliasing(specifier.specifier as J.Identifier, this.to.member) as Draft<JS.Alias>;
            }
        });
    }

    /** Whether an `export {…} from` entry re-exports the member being moved. */
    private reExportsMember(element: J): boolean {
        if (element.kind !== JS.Kind.ExportSpecifier) {
            return false;
        }
        const specifier = (element as JS.ExportSpecifier).specifier;
        const read = specifier.kind === JS.Kind.Alias ? (specifier as JS.Alias).propertyName.element : specifier;
        return (read.kind === J.Kind.Identifier || read.kind === J.Kind.Literal) &&
            moduleExportName(read as J.Identifier | J.Literal) === this.from.member;
    }
}

/** `statement` as an `export {…} from "<module>"` declaration, where it is one. */
function reExportFrom(statement: J | undefined, module: string): JS.ExportDeclaration | undefined {
    if (statement?.kind !== JS.Kind.ExportDeclaration) {
        return undefined;
    }
    const declaration = statement as JS.ExportDeclaration;
    const source = declaration.moduleSpecifier?.element;
    return source?.kind === J.Kind.Literal && typeof (source as J.Literal).value === "string" &&
        sameModule(moduleNameOf(source as J.Literal), module) && declaration.exportClause?.kind === JS.Kind.NamedExports
        ? declaration : undefined;
}

function clauseOf(declaration: JS.ExportDeclaration): JS.NamedExports {
    return declaration.exportClause as JS.NamedExports;
}

function withElements(declaration: JS.ExportDeclaration, elements: J.RightPadded<Expression>[]): JS.ExportDeclaration {
    const clause = clauseOf(declaration);
    return {...declaration, exportClause: {...clause, elements: {...clause.elements, elements}} as JS.NamedExports};
}

/** `elements` without `dropped`, keeping the space after `{`, and the space and any trailing comma before `}`. */
function without<T extends J>(elements: J.RightPadded<T>[], dropped: ReadonlySet<J.RightPadded<T>>): J.RightPadded<T>[] {
    const kept = elements.filter(e => !dropped.has(e));
    const first = elements[0], last = elements[elements.length - 1];
    return kept.map((e, i) => ({
        ...e,
        element: i === 0 ? {...e.element, prefix: first.element.prefix} : e.element,
        ...(i === kept.length - 1 ? {after: last.after, markers: last.markers} : {})
    }));
}

/** `elements` with `added` at the end, separated as `elements` are, the trailing comma and space before `}` moving last. */
function appended<T extends J>(elements: J.RightPadded<T>[], added: J.RightPadded<T>[]): J.RightPadded<T>[] {
    const last = elements[elements.length - 1];
    const separator = elements[1]?.element.prefix ??
        (elements[0].element.prefix.whitespace.includes("\n") ? elements[0].element.prefix : singleSpace);
    const plain = (markers: J.RightPadded<T>["markers"]) =>
        ({...markers, markers: markers.markers.filter(m => m.kind !== J.Markers.TrailingComma)});
    return [
        ...elements.slice(0, -1),
        {...last, after: emptySpace, markers: plain(last.markers)},
        ...added.map((e, i) => i === added.length - 1
            ? {...e, element: {...e.element, prefix: separator}, after: last.after, markers: last.markers}
            : {...e, element: {...e.element, prefix: separator}, after: emptySpace, markers: plain(e.markers)})
    ];
}
